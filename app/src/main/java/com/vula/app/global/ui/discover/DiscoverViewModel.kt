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
import com.vula.app.core.data.SessionManager
import com.vula.app.core.model.Post
import com.vula.app.core.model.User
import com.vula.app.core.network.VulaApiService
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
import javax.inject.Inject

// ── Filter chip enum ──────────────────────────────────────────────────────────

enum class DiscoverFilter(val label: String, val icon: ImageVector) {
    TRENDING("Trending", Icons.Default.Whatshot),
    PEOPLE("People",    Icons.Default.Group),
    CLIPS("Clips",      Icons.Default.Videocam),
    NEW("New",          Icons.Default.AutoAwesome),
}

// ── Models ────────────────────────────────────────────────────────────────────

data class TrendingTopic(
    val hashtag: String,
    val postCount: Int,
    val coverImageUrl: String?
)

data class SuggestedUser(
    val user: User,
    val isFollowing: Boolean
)

sealed class SearchResult {
    data class UserResult(val user: User)              : SearchResult()
    data class HashtagResult(val hashtag: String, val postCount: Int) : SearchResult()
}

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class DiscoverUiState {
    object Loading : DiscoverUiState()
    data class Success(
        val explorePosts:   List<Post>,
        val trendingTopics: List<TrendingTopic>,
        val suggestedUsers: List<SuggestedUser>,
        val hasMorePages:   Boolean = true
    ) : DiscoverUiState()
    data class Error(val message: String) : DiscoverUiState()
}

// ─────────────────────────────────────────────────────────────────────────────

private const val PREFS_NAME    = "discover_prefs"
private const val KEY_RECENT    = "recent_searches"
private const val SEPARATOR     = "|||"
private const val EXPLORE_LIMIT = 40
private const val CLIPS_LIMIT   = 30

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val api: VulaApiService,
    private val followRepository: FollowRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var currentUid: String = ""

    // ── Explore grid ──────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _activeFilter = MutableStateFlow(DiscoverFilter.TRENDING)
    val activeFilter: StateFlow<DiscoverFilter> = _activeFilter.asStateFlow()

    private val _selectedTopic = MutableStateFlow<TrendingTopic?>(null)
    val selectedTopic: StateFlow<TrendingTopic?> = _selectedTopic.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────
    private val _searchQuery    = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults  = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching    = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(loadRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _followingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followingState: StateFlow<Map<String, Boolean>> = _followingState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            currentUid = sessionManager.getUserIdNow() ?: ""
            loadDiscover()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun setFilter(filter: DiscoverFilter) {
        _activeFilter.value  = filter
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

                // Run both lookups in parallel
                val usersDeferred = async {
                    runCatching {
                        api.searchUsers(query = cleanQuery, limit = 15).body() ?: emptyList()
                    }.getOrElse { emptyList() }
                }

                val postsDeferred = async {
                    if (isAtUser) return@async emptyList()
                    runCatching {
                        api.getExplorePosts(filter = "new", limit = 50).body() ?: emptyList()
                    }.getOrElse { emptyList() }
                }

                val apiUsers = usersDeferred.await()
                val apiPosts = postsDeferred.await()

                // Build hashtag counts from captions
                val tagCounts = apiPosts
                    .flatMap { post ->
                        Regex("#(\\w+)").findAll(post.caption)
                            .map { it.groupValues[1].lowercase() }
                            .filter { it.startsWith(cleanQuery) }
                            .toList()
                    }
                    .groupingBy { it }
                    .eachCount()

                val results = mutableListOf<SearchResult>()
                if (!isHashtag) {
                    apiUsers.forEach { u ->
                        results.add(SearchResult.UserResult(u.toUser()))
                    }
                }
                tagCounts.entries
                    .sortedByDescending { it.value }
                    .take(8)
                    .forEach { (tag, count) -> results.add(SearchResult.HashtagResult(tag, count)) }

                if (isHashtag && tagCounts.isEmpty()) {
                    apiUsers.forEach { results.add(SearchResult.UserResult(it.toUser())) }
                }

                _searchResults.value = results
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addRecentSearch(term: String) {
        val updated = _recentSearches.value.toMutableList()
            .also { it.remove(term); it.add(0, term) }.take(6)
        _recentSearches.value = updated
        saveRecentSearches(updated)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove(KEY_RECENT).apply()
    }

    fun toggleFollow(targetUserId: String) {
        val currentlyFollowing = _followingState.value[targetUserId] == true
        _followingState.value = _followingState.value.toMutableMap()
            .also { it[targetUserId] = !currentlyFollowing }
        viewModelScope.launch {
            if (currentlyFollowing) followRepository.unfollowUser(targetUserId, currentUid)
            else                    followRepository.followUser(targetUserId, currentUid)
        }
    }

    fun refresh() = loadDiscover()

    // ── Private loaders ───────────────────────────────────────────────────────

    private fun loadDiscover() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            try {
                val filter = when (_activeFilter.value) {
                    DiscoverFilter.TRENDING -> "trending"
                    DiscoverFilter.NEW      -> "new"
                    DiscoverFilter.CLIPS    -> "clips"
                    DiscoverFilter.PEOPLE   -> "new"
                }
                val limit = if (_activeFilter.value == DiscoverFilter.CLIPS) CLIPS_LIMIT else EXPLORE_LIMIT

                val postsDeferred   = async { runCatching { api.getExplorePosts(filter, limit).body() ?: emptyList() }.getOrElse { emptyList() } }
                val peopleDeferred  = async { runCatching { api.getSuggestedUsers(15).body() ?: emptyList() }.getOrElse { emptyList() } }

                var apiPosts = postsDeferred.await()
                val apiPeople = peopleDeferred.await()

                // Apply topic filter in-memory
                _selectedTopic.value?.let { topic ->
                    apiPosts = apiPosts.filter {
                        it.caption.contains("#${topic.hashtag}", ignoreCase = true)
                    }
                }

                // Exclude own posts
                val posts = apiPosts
                    .filter { it.authorId != currentUid }
                    .map { it.toPost() }

                val trending = buildTrendingTopics(posts)

                // Fetch follow status for suggested users
                val followMap = mutableMapOf<String, Boolean>()
                apiPeople.filter { it.id != currentUid }.take(10).forEach { u ->
                    val status = runCatching { api.getFollowStatus(u.id).body()?.isFollowing == true }
                        .getOrElse { false }
                    followMap[u.id] = status
                }
                _followingState.value = followMap

                val suggestedUsers = apiPeople
                    .filter { it.id != currentUid }
                    .take(10)
                    .map { SuggestedUser(it.toUser(), followMap[it.id] == true) }

                _uiState.value = DiscoverUiState.Success(
                    explorePosts   = posts,
                    trendingTopics = trending,
                    suggestedUsers = suggestedUsers,
                    hasMorePages   = apiPosts.size >= limit
                )
            } catch (e: Exception) {
                _uiState.value = DiscoverUiState.Error(e.message ?: "Unknown error")
            }
        }
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
            .map { (tag, count) -> TrendingTopic(tag, count, tagCover[tag]) }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun com.vula.app.core.network.ApiUser.toUser() = User(
        id              = id,
        username        = username,
        phoneNumber     = phoneNumber,
        phoneHash       = "",
        displayName     = displayName,
        bio             = bio,
        profileImageUrl = profileImageUrl,
        richStatus      = richStatus,
        followersCount  = followersCount,
        followingCount  = followingCount,
        postsCount      = postsCount,
        isOnline        = isOnline,
        createdAt       = createdAt
    )

    private fun com.vula.app.core.network.ApiPost.toPost() = Post(
        id                    = id,
        authorId              = authorId,
        authorUsername        = authorUsername,
        authorProfileImageUrl = authorProfileImageUrl,
        caption               = caption,
        imageUrl              = imageUrl,
        mediaType             = mediaType,
        likesCount            = likesCount,
        commentsCount         = commentsCount,
        createdAt             = createdAt,
        likedBy               = likedBy,
        reactions             = reactions
    )

    // ── SharedPreferences helpers ─────────────────────────────────────────────

    private fun loadRecentSearches(): List<String> =
        prefs.getString(KEY_RECENT, null)
            ?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    private fun saveRecentSearches(searches: List<String>) {
        prefs.edit().putString(KEY_RECENT, searches.joinToString(SEPARATOR)).apply()
    }
}
