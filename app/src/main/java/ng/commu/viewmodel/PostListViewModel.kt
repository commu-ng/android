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
class PostListViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PostListViewModel"
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

    private val _hasLoadedOnce = MutableStateFlow(false)
    val hasLoadedOnce: StateFlow<Boolean> = _hasLoadedOnce.asStateFlow()

    private var nextCursor: String? = null
    private var currentProfileId: String? = null

    fun loadPosts(profileId: String, refresh: Boolean = false) {
        if (refresh) {
            nextCursor = null
            // Don't clear posts during refresh to avoid flickering
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentProfileId = profileId

            try {
                val response = postRepository.getCommunityPosts(
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
                        Log.d(TAG, "Loaded ${body.data.size} posts")
                    }
                } else {
                    _errorMessage.value = "Failed to load posts: ${response.code()}"
                    Log.e(TAG, "Failed to load posts: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading posts: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load posts", e)
            } finally {
                _isLoading.value = false
                _hasLoadedOnce.value = true
            }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingMore.value || !_hasMore.value || nextCursor == null || currentProfileId == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val response = postRepository.getCommunityPosts(
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
                        Log.d(TAG, "Loaded ${body.data.size} more posts")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more posts", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun refresh(profileId: String) {
        loadPosts(profileId, refresh = true)
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
                    Log.d(TAG, "Removing reaction $emoji from post $postId by profile $profileId")
                    val response = postRepository.removeReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Reaction removed successfully, refreshing post")
                        refreshPost(postId, profileId)
                    } else {
                        Log.e(TAG, "Failed to remove reaction: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } else {
                    Log.d(TAG, "Adding reaction $emoji to post $postId by profile $profileId")
                    val response = postRepository.addReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Reaction added successfully, refreshing post")
                        refreshPost(postId, profileId)
                    } else {
                        Log.e(TAG, "Failed to add reaction: ${response.code()} - ${response.errorBody()?.string()}")
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
                Log.d(TAG, "Adding reaction $emoji to post $postId by profile $profileId")
                val response = postRepository.addReaction(postId, emoji, profileId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Reaction added successfully, refreshing post")
                    // Reload the specific post to get updated reactions
                    refreshPost(postId, profileId)
                } else {
                    Log.e(TAG, "Failed to add reaction: ${response.code()} - ${response.errorBody()?.string()}")
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

    fun deletePost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting post $postId")
                val response = postRepository.deleteCommunityPost(postId, profileId)
                if (response.isSuccessful) {
                    // Remove from local state
                    _posts.value = _posts.value.filter { it.id != postId }
                    Log.d(TAG, "Post deleted successfully")
                } else {
                    Log.e(TAG, "Failed to delete post: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete post", e)
            }
        }
    }

    private fun refreshPost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing post $postId for profile $profileId")
                val response = postRepository.getCommunityPost(postId, profileId)
                if (response.isSuccessful) {
                    val updatedPost = response.body()?.data
                    if (updatedPost != null) {
                        Log.d(TAG, "Got updated post with ${updatedPost.reactions?.size ?: 0} reactions")
                        _posts.value = _posts.value.map { post ->
                            if (post.id == postId) updatedPost else post
                        }
                        Log.d(TAG, "Updated posts list")
                    } else {
                        Log.e(TAG, "Updated post is null")
                    }
                } else {
                    Log.e(TAG, "Failed to refresh post: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh post", e)
            }
        }
    }
}
