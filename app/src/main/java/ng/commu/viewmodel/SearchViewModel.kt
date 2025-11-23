package ng.commu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.CommunityPost
import ng.commu.data.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_DELAY = 500L // milliseconds
        private const val MIN_QUERY_LENGTH = 2
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var searchJob: Job? = null
    private var nextCursor: String? = null
    private var currentProfileId: String? = null
    private var currentQuery: String? = null

    fun updateSearchQuery(query: String, profileId: String) {
        _searchQuery.value = query
        currentProfileId = profileId

        // Cancel previous search job
        searchJob?.cancel()

        if (query.length < MIN_QUERY_LENGTH) {
            _posts.value = emptyList()
            _hasMore.value = false
            return
        }

        // Debounce search
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            performSearch(query, profileId)
        }
    }

    private fun performSearch(query: String, profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentQuery = query
            currentProfileId = profileId

            try {
                val response = postRepository.searchPosts(
                    query = query,
                    profileId = profileId,
                    limit = 20,
                    cursor = null
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _posts.value = body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Found ${body.data.size} posts for query: $query")
                    }
                } else {
                    _errorMessage.value = "Search failed: ${response.code()}"
                    Log.e(TAG, "Search failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error searching: ${e.localizedMessage}"
                Log.e(TAG, "Search error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreResults() {
        if (_isLoadingMore.value || !_hasMore.value || nextCursor == null ||
            currentProfileId == null || currentQuery == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val response = postRepository.searchPosts(
                    query = currentQuery!!,
                    profileId = currentProfileId!!,
                    limit = 20,
                    cursor = nextCursor
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _posts.value = _posts.value + body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} more results")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more results", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _posts.value = emptyList()
        _hasMore.value = false
        _errorMessage.value = null
        nextCursor = null
        currentQuery = null
    }

    fun toggleReaction(postId: String, profileId: String, emoji: String) {
        viewModelScope.launch {
            try {
                // Find the post and check if user has already reacted with this emoji
                val post = _posts.value.find { it.id == postId }
                val hasReacted = post?.reactions?.any {
                    it.emoji == emoji && it.user?.id == profileId
                } ?: false

                if (hasReacted) {
                    val response = postRepository.removeReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        currentQuery?.let { query -> performSearch(query, profileId) }
                    } else {
                        Log.e(TAG, "Failed to remove reaction: ${response.code()}")
                    }
                } else {
                    val response = postRepository.addReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        currentQuery?.let { query -> performSearch(query, profileId) }
                    } else {
                        Log.e(TAG, "Failed to add reaction: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle reaction", e)
            }
        }
    }

    fun addReaction(postId: String, profileId: String, emoji: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.addReaction(postId, emoji, profileId)
                if (response.isSuccessful) {
                    // Refresh search results to get updated reactions
                    currentQuery?.let { query ->
                        performSearch(query, profileId)
                    }
                } else {
                    Log.e(TAG, "Failed to add reaction: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add reaction", e)
            }
        }
    }

    fun bookmarkPost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.bookmarkPost(postId, profileId)
                if (response.isSuccessful) {
                    // Update local state
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) post.copy(isBookmarked = true) else post
                    }
                } else {
                    Log.e(TAG, "Failed to bookmark post: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bookmark post", e)
            }
        }
    }

    fun unbookmarkPost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.unbookmarkPost(postId, profileId)
                if (response.isSuccessful) {
                    // Update local state
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) post.copy(isBookmarked = false) else post
                    }
                } else {
                    Log.e(TAG, "Failed to unbookmark post: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbookmark post", e)
            }
        }
    }
}
