package com.vula.app.global.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vula.app.core.model.Post
import com.vula.app.core.model.User
import com.vula.app.core.util.Constants
import com.vula.app.global.data.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ── Filter chip enum ──────────────────────────────────────────────────────────

enum class DiscoverFilter(val label: String, val emoji: String) {
    TRENDING("Trending", "🔥"),
    PEOPLE("People", "👥"),
    CLIPS("Clips", "🎬"),
    NEW("New", "✨"),
}

// ── Trending topic model ───────────────────────────────────────────────────────

data class TrendingTopic(
    val hashtag: String,
    val postCount: Int,
    val coverImageUrl: String?
)

// ── Suggested user model ───────────────────────────────────────────────────────

data class SuggestedUser(
    val user: User,
    val mutualCount: Int,
    val isFollowing: Boolean
)

// ── UI state ─────────────────────────────────────────────────────────────────

sealed class DiscoverUiState {
    object Loading : DiscoverUiState()
    data class Success(
        val explorePosts: List<Post>,
        val trendingTopics: List<TrendingTopic>,
        val suggestedUsers: List<SuggestedUser>
    ) : DiscoverUiState()
    data class Error(val message: String) : DiscoverUiState()
}

// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Explore grid ──────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    // ── Active filter chip ────────────────────────────────────────────────────
    private val _activeFilter = MutableStateFlow(DiscoverFilter.TRENDING)
    val activeFilter: StateFlow<DiscoverFilter> = _activeFilter.asStateFlow()

    // ── Selected trending topic (null = no topic filter) ─────────────────────
    private val _selectedTopic = MutableStateFlow<TrendingTopic?>(null)
    val selectedTopic: StateFlow<TrendingTopic?> = _selectedTopic.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("@ricky_v", "#vibe", "Cape Town")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // ── Follow state cache (userId → following) ───────────────────────────────
    private val _followingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followingState: StateFlow<Map<String, Boolean>> = _followingState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDiscover()
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    fun setFilter(filter: DiscoverFilter) {
        _activeFilter.value = filter
        _selectedTopic.value = null
        loadDiscover()
    }

    fun selectTopic(topic: TrendingTopic) {
        _selectedTopic.value = if (_selectedTopic.value?.hashtag == topic.hashtag) null else topic
        loadDiscover()
    }

    fun openSearch() { _isSearchActive.value = true }
    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(280)
            _isSearching.value = true
            try {
                val q = query.trim().lowercase()
                val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                    .whereGreaterThanOrEqualTo("username", q)
                    .whereLessThan("username", q + "\uF8FF")
                    .limit(20)
                    .get().await()
                _searchResults.value = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addRecentSearch(term: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(term)
        current.add(0, term)
        _recentSearches.value = current.take(6)
    }

    fun clearRecentSearches() { _recentSearches.value = emptyList() }

    fun toggleFollow(targetUserId: String) {
        val currentlyFollowing = _followingState.value[targetUserId] == true
        // Optimistic update
        _followingState.value = _followingState.value.toMutableMap().also {
            it[targetUserId] = !currentlyFollowing
        }
        viewModelScope.launch {
            if (currentlyFollowing) {
                followRepository.unfollowUser(targetUserId, currentUid)
            } else {
                followRepository.followUser(targetUserId, currentUid)
            }
        }
    }

    fun refresh() { loadDiscover() }

    // ─── Private loaders ───────────────────────────────────────────────────────

    private fun loadDiscover() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            try {
                val posts = fetchExplorePosts()
                val trending = buildTrendingTopics(posts)
                val people = fetchSuggestedUsers()
                _uiState.value = DiscoverUiState.Success(
                    explorePosts = posts,
                    trendingTopics = trending,
                    suggestedUsers = people
                )
            } catch (e: Exception) {
                _uiState.value = DiscoverUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchExplorePosts(): List<Post> {
        val baseQuery: Query = when (_activeFilter.value) {
            DiscoverFilter.TRENDING ->
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("likesCount", Query.Direction.DESCENDING)
                    .limit(40)

            DiscoverFilter.NEW ->
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(40)

            DiscoverFilter.CLIPS ->
                firestore.collection(Constants.POSTS_COLLECTION)
                    .whereEqualTo("mediaType", "video")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(30)

            DiscoverFilter.PEOPLE ->
                // People tab: show their posts; the people row itself is separate
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(30)
        }

        val snapshot = baseQuery.get().await()
        var posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }

        // Filter by selected topic hashtag if applicable
        val topic = _selectedTopic.value
        if (topic != null) {
            posts = posts.filter {
                it.caption.contains("#${topic.hashtag}", ignoreCase = true)
            }
        }

        // Exclude current user's own posts
        return posts.filter { it.authorId != currentUid }
    }

    private fun buildTrendingTopics(posts: List<Post>): List<TrendingTopic> {
        // Extract #hashtags from captions, count occurrences, pick top 8
        val hashtagRegex = Regex("#(\\w+)")
        val tagCounts = mutableMapOf<String, Int>()
        val tagCover = mutableMapOf<String, String?>()

        posts.forEach { post ->
            hashtagRegex.findAll(post.caption).forEach { match ->
                val tag = match.groupValues[1].lowercase()
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                if (!tagCover.containsKey(tag)) tagCover[tag] = post.imageUrl
            }
        }

        return tagCounts.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { (tag, count) ->
                TrendingTopic(
                    hashtag = tag,
                    postCount = count,
                    coverImageUrl = tagCover[tag]
                )
            }
    }

    private suspend fun fetchSuggestedUsers(): List<SuggestedUser> {
        return try {
            // Fetch users ordered by follower count, exclude self, limit 10
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(15)
                .get().await()

            val users = snapshot.documents
                .mapNotNull { it.toObject(User::class.java) }
                .filter { it.id != currentUid }
                .take(10)

            // Check follow state for each
            val followMap = mutableMapOf<String, Boolean>()
            users.forEach { user ->
                val followDoc = firestore.collection(Constants.USERS_COLLECTION)
                    .document(currentUid)
                    .collection(Constants.FOLLOWING_SUBCOLLECTION)
                    .document(user.id)
                    .get().await()
                followMap[user.id] = followDoc.exists()
            }
            _followingState.value = followMap

            users.map { user ->
                SuggestedUser(
                    user = user,
                    mutualCount = (user.followersCount / 10).coerceAtMost(50), // approximation
                    isFollowing = followMap[user.id] == true
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
