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
class ScheduledPostsViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ScheduledPostsViewModel"
    }

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var nextCursor: String? = null
    private var currentProfileId: String? = null

    fun loadPosts(profileId: String) {
        if (_isLoading.value) return

        currentProfileId = profileId
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = postRepository.getScheduledPosts(
                    profileId = profileId,
                    limit = 20,
                    cursor = null
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    _posts.value = body?.data ?: emptyList()
                    nextCursor = body?.pagination?.nextCursor
                    _hasMore.value = body?.pagination?.hasMore ?: false
                    Log.d(TAG, "Loaded ${_posts.value.size} scheduled posts")
                } else {
                    _errorMessage.value = "Failed to load scheduled posts: ${response.code()}"
                    Log.e(TAG, "Failed to load scheduled posts: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading scheduled posts: ${e.localizedMessage}"
                Log.e(TAG, "Error loading scheduled posts", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        val profileId = currentProfileId ?: return
        if (_isLoading.value || !_hasMore.value || nextCursor == null) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val response = postRepository.getScheduledPosts(
                    profileId = profileId,
                    limit = 20,
                    cursor = nextCursor
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    _posts.value = _posts.value + (body?.data ?: emptyList())
                    nextCursor = body?.pagination?.nextCursor
                    _hasMore.value = body?.pagination?.hasMore ?: false
                } else {
                    Log.e(TAG, "Failed to load more: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.deleteCommunityPost(postId, profileId)

                if (response.isSuccessful) {
                    _posts.value = _posts.value.filter { it.id != postId }
                    Log.d(TAG, "Deleted scheduled post: $postId")
                } else {
                    _errorMessage.value = "Failed to delete post"
                    Log.e(TAG, "Failed to delete post: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting post: ${e.localizedMessage}"
                Log.e(TAG, "Error deleting post", e)
            }
        }
    }

    fun refresh() {
        currentProfileId?.let { loadPosts(it) }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
