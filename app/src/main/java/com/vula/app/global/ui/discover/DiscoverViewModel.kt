package com.vula.app.global.ui.discover

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ── Filter chip enum ──────────────────────────────────────────────────────────

enum class DiscoverFilter(val label: String, val icon: ImageVector) {
    TRENDING("Trending", Icons.Default.Whatshot),
    PEOPLE("People", Icons.Default.Group),
    CLIPS("Clips", Icons.Default.Videocam),
    NEW("New", Icons.Default.AutoAwesome),
}

// ── Trending topic model ───────────────────────────────────────────────────────

data class TrendingTopic(
    val hashtag: String,
    val postCount: Int,
    val coverImageUrl: String?
)

// ── Suggested user model (mutualCount removed — was fake) ─────────────────────

data class SuggestedUser(
    val user: User,
    val isFollowing: Boolean
)

// ── Search result — users AND hashtags ────────────────────────────────────────

sealed class SearchResult {
    data class UserResult(val user: User) : SearchResult()
    data class HashtagResult(val hashtag: String, val postCount: Int) : SearchResult()
}

// ── UI state ─────────────────────────────────────────────────────────────────

sealed class DiscoverUiState {
    object Loading : DiscoverUiState()
    data class Success(
        val explorePosts: List<Post>,
        val trendingTopics: List<TrendingTopic>,
        val suggestedUsers: List<SuggestedUser>,
        /** True while we haven't fetched fewer items than the page limit. */
        val hasMorePages: Boolean = true
    ) : DiscoverUiState()
    data class Error(val message: String) : DiscoverUiState()
}

// ─────────────────────────────────────────────────────────────────────────────

private const val PREFS_NAME      = "discover_prefs"
private const val KEY_RECENT      = "recent_searches"
private const val SEPARATOR       = "|||"
private const val EXPLORE_LIMIT   = 40L
private const val CLIPS_LIMIT     = 30L

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val followRepository: FollowRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // ── Recent searches — persisted to SharedPreferences ─────────────────────
    private val _recentSearches = MutableStateFlow<List<String>>(loadRecentSearches())
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
        _selectedTopic.value =
            if (_selectedTopic.value?.hashtag == topic.hashtag) null else topic
        loadDiscover()
    }

    fun openSearch()  { _isSearchActive.value = true }
    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value    = ""
        _searchResults.value  = emptyList()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = emptyList(); return }

        searchJob = viewModelScope.launch {
            delay(280)
            _isSearching.value = true
            try {
                val isHashtag  = query.trim().startsWith("#")
                val isAtUser   = query.trim().startsWith("@")
                val cleanQuery = query.trim().lowercase().removePrefix("#").removePrefix("@")

                // ── Run both lookups in parallel ───────────────────────────
                val usersDeferred = async {
                    runCatching {
                        firestore.collection(Constants.USERS_COLLECTION)
                            .whereGreaterThanOrEqualTo("username", cleanQuery)
                            .whereLessThan("username", cleanQuery + "\uF8FF")
                            .limit(15)
                            .get().await()
                            .documents.mapNotNull { it.toObject(User::class.java) }
                    }.getOrElse { emptyList() }
                }

                val tagsDeferred = async {
                    // Skip hashtag lookup when the user is clearly searching for a person
                    if (isAtUser) return@async emptyMap<String, Int>()
                    runCatching {
                        firestore.collection(Constants.POSTS_COLLECTION)
                            .whereGreaterThanOrEqualTo("caption", "#$cleanQuery")
                            .whereLessThan("caption", "#$cleanQuery\uF8FF")
                            .limit(30)
                            .get().await()
                            .documents.mapNotNull { it.toObject(Post::class.java) }
                    }.getOrElse { emptyList<Post>() }
                        .flatMap { post ->
                            Regex("#(\\w+)").findAll(post.caption)
                                .map { it.groupValues[1].lowercase() }
                                .filter { it.startsWith(cleanQuery) }
                                .toList()
                        }
                        .groupingBy { it }
                        .eachCount()
                }

                val users    = usersDeferred.await()
                val tagCounts = tagsDeferred.await()

                val results = mutableListOf<SearchResult>()

                // People section (skip if this is a pure hashtag query)
                if (!isHashtag) {
                    users.forEach { results.add(SearchResult.UserResult(it)) }
                }

                // Tags section
                tagCounts.entries
                    .sortedByDescending { it.value }
                    .take(8)
                    .forEach { (tag, count) ->
                        results.add(SearchResult.HashtagResult(tag, count))
                    }

                // If it was a hashtag query but nothing matched — fall back to users
                if (isHashtag && tagCounts.isEmpty()) {
                    users.forEach { results.add(SearchResult.UserResult(it)) }
                }

                _searchResults.value = results
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addRecentSearch(term: String) {
        val updated = _recentSearches.value.toMutableList()
            .also { it.remove(term); it.add(0, term) }
            .take(6)
        _recentSearches.value = updated
        saveRecentSearches(updated)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove(KEY_RECENT).apply()
    }

    fun toggleFollow(targetUserId: String) {
        val currentlyFollowing = _followingState.value[targetUserId] == true
        _followingState.value = _followingState.value.toMutableMap().also {
            it[targetUserId] = !currentlyFollowing
        }
        viewModelScope.launch {
            if (currentlyFollowing) followRepository.unfollowUser(targetUserId, currentUid)
            else                    followRepository.followUser(targetUserId, currentUid)
        }
    }

    fun refresh() { loadDiscover() }

    // ─── Private loaders ───────────────────────────────────────────────────────

    private fun loadDiscover() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            try {
                val posts    = fetchExplorePosts()
                val trending = buildTrendingTopics(posts)
                val people   = fetchSuggestedUsers()

                val limit = if (_activeFilter.value == DiscoverFilter.CLIPS) CLIPS_LIMIT
                            else EXPLORE_LIMIT

                _uiState.value = DiscoverUiState.Success(
                    explorePosts   = posts,
                    trendingTopics = trending,
                    suggestedUsers = people,
                    // Only false when the server returned fewer items than we asked for
                    hasMorePages   = posts.size.toLong() >= limit
                )
            } catch (e: Exception) {
                _uiState.value = DiscoverUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchExplorePosts(): List<Post> {
        val limit = if (_activeFilter.value == DiscoverFilter.CLIPS) CLIPS_LIMIT
                    else EXPLORE_LIMIT

        val baseQuery: Query = when (_activeFilter.value) {
            DiscoverFilter.TRENDING ->
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("likesCount", Query.Direction.DESCENDING)
                    .limit(limit)

            DiscoverFilter.NEW ->
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)

            DiscoverFilter.CLIPS ->
                // No orderBy to avoid missing composite index; sorted in-memory below
                firestore.collection(Constants.POSTS_COLLECTION)
                    .whereEqualTo("mediaType", "video")
                    .limit(limit)

            DiscoverFilter.PEOPLE ->
                // People tab shows image posts as a visual backdrop; people cards are primary
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)
        }

        val snapshot = baseQuery.get().await()
        var posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }

        if (_activeFilter.value == DiscoverFilter.CLIPS) {
            posts = posts.sortedByDescending { it.createdAt }
        }

        _selectedTopic.value?.let { topic ->
            posts = posts.filter {
                it.caption.contains("#${topic.hashtag}", ignoreCase = true)
            }
        }

        return posts.filter { it.authorId != currentUid }
    }

    private fun buildTrendingTopics(posts: List<Post>): List<TrendingTopic> {
        val hashtagRegex = Regex("#(\\w+)")
        val tagCounts    = mutableMapOf<String, Int>()
        val tagCover     = mutableMapOf<String, String?>()

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
                TrendingTopic(hashtag = tag, postCount = count, coverImageUrl = tagCover[tag])
            }
    }

    private suspend fun fetchSuggestedUsers(): List<SuggestedUser> {
        return try {
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(15)
                .get().await()

            val users = snapshot.documents
                .mapNotNull { it.toObject(User::class.java) }
                .filter { it.id != currentUid }
                .take(10)

            val followMap = mutableMapOf<String, Boolean>()
            users.forEach { user ->
                val doc = firestore.collection(Constants.USERS_COLLECTION)
                    .document(currentUid)
                    .collection(Constants.FOLLOWING_SUBCOLLECTION)
                    .document(user.id)
                    .get().await()
                followMap[user.id] = doc.exists()
            }
            _followingState.value = followMap

            users.map { user ->
                SuggestedUser(user = user, isFollowing = followMap[user.id] == true)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ─── SharedPreferences helpers ─────────────────────────────────────────────

    private fun loadRecentSearches(): List<String> =
        prefs.getString(KEY_RECENT, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun saveRecentSearches(searches: List<String>) {
        prefs.edit().putString(KEY_RECENT, searches.joinToString(SEPARATOR)).apply()
    }
}
