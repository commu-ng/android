package ng.commu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.CommunityPost
import ng.commu.data.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BookmarksViewModel"
    }

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

    private var nextCursor: String? = null
    private var currentProfileId: String? = null

    fun loadBookmarks(profileId: String, refresh: Boolean = false) {
        if (refresh) {
            nextCursor = null
            _posts.value = emptyList()
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentProfileId = profileId

            try {
                val response = postRepository.getBookmarkedPosts(
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
                        Log.d(TAG, "Loaded ${body.data.size} bookmarked posts")
                    }
                } else {
                    _errorMessage.value = "Failed to load bookmarks: ${response.code()}"
                    Log.e(TAG, "Failed to load bookmarks: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading bookmarks: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load bookmarks", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreBookmarks() {
        if (_isLoadingMore.value || !_hasMore.value || nextCursor == null || currentProfileId == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val response = postRepository.getBookmarkedPosts(
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
                        Log.d(TAG, "Loaded ${body.data.size} more bookmarked posts")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more bookmarks", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun unbookmarkPost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.unbookmarkPost(postId, profileId)
                if (response.isSuccessful) {
                    // Remove from local list
                    _posts.value = _posts.value.filter { it.id != postId }
                    Log.d(TAG, "Unbookmarked post: $postId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbookmark post", e)
            }
        }
    }

    fun refresh(profileId: String) {
        loadBookmarks(profileId, refresh = true)
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
                        refreshPost(postId, profileId)
                    } else {
                        Log.e(TAG, "Failed to remove reaction: ${response.code()}")
                    }
                } else {
                    val response = postRepository.addReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        refreshPost(postId, profileId)
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
                    // Refresh post to get updated reactions
                    refreshPost(postId, profileId)
                } else {
                    Log.e(TAG, "Failed to add reaction: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add reaction", e)
            }
        }
    }

    private fun refreshPost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.getCommunityPost(postId, profileId)
                if (response.isSuccessful) {
                    val updatedPost = response.body()?.data
                    if (updatedPost != null) {
                        _posts.value = _posts.value.map { post ->
                            if (post.id == postId) updatedPost else post
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh post", e)
            }
        }
    }
}
