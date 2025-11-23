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
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PostDetailViewModel"
    }

    private val _post = MutableStateFlow<CommunityPost?>(null)
    val post: StateFlow<CommunityPost?> = _post.asStateFlow()

    private val _replies = MutableStateFlow<List<CommunityPost>>(emptyList())
    val replies: StateFlow<List<CommunityPost>> = _replies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMoreReplies = MutableStateFlow(false)
    val hasMoreReplies: StateFlow<Boolean> = _hasMoreReplies.asStateFlow()

    private var nextCursor: String? = null

    fun loadPost(postId: String, profileId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = postRepository.getCommunityPost(postId, profileId)
                if (response.isSuccessful) {
                    val loadedPost = response.body()?.data
                    _post.value = loadedPost

                    // Extract and flatten replies from the post
                    loadedPost?.replies?.let { nestedReplies ->
                        _replies.value = flattenReplies(nestedReplies, 0)
                        Log.d(TAG, "Loaded ${_replies.value.size} replies")
                    } ?: run {
                        _replies.value = emptyList()
                    }

                    Log.d(TAG, "Loaded post: $postId")
                } else {
                    _errorMessage.value = "Failed to load post: ${response.code()}"
                    Log.e(TAG, "Failed to load post: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading post: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load post", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun flattenReplies(replies: List<CommunityPost>, depth: Int): List<CommunityPost> {
        val result = mutableListOf<CommunityPost>()
        for (reply in replies) {
            // Add the reply with its depth
            result.add(reply.copy(depth = depth))
            // Recursively add nested replies
            reply.replies?.let { nestedReplies ->
                result.addAll(flattenReplies(nestedReplies, depth + 1))
            }
        }
        return result
    }

    fun refresh(postId: String, profileId: String?) {
        _post.value = null
        _replies.value = emptyList()
        nextCursor = null
        _hasMoreReplies.value = false
        loadPost(postId, profileId)
    }

    fun toggleReaction(postId: String, emoji: String, profileId: String) {
        viewModelScope.launch {
            try {
                // Check if user has already reacted with this emoji
                // Look in both the main post and replies
                val targetPost = if (_post.value?.id == postId) {
                    _post.value
                } else {
                    findPostInReplies(_replies.value, postId)
                }

                val hasReacted = targetPost?.reactions?.any {
                    it.emoji == emoji && it.user?.id == profileId
                } ?: false

                if (hasReacted) {
                    Log.d(TAG, "Removing reaction $emoji from post $postId")
                    val response = postRepository.removeReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        // Optimistically update local state
                        updatePostReactions(postId, emoji, profileId, isAdding = false)
                    } else {
                        Log.e(TAG, "Failed to remove reaction", Exception())
                    }
                } else {
                    Log.d(TAG, "Adding reaction $emoji to post $postId")
                    val response = postRepository.addReaction(postId, emoji, profileId)
                    if (response.isSuccessful) {
                        // Create a reaction object for optimistic update
                        val newReaction = ng.commu.data.model.CommunityPostReaction(
                            emoji = emoji,
                            user = ng.commu.data.model.CommunityPostReactionUser(
                                id = profileId,
                                username = "", // We don't have this info, but it's not critical for UI update
                                name = ""
                            )
                        )
                        // Optimistically update local state
                        updatePostReactions(postId, emoji, profileId, isAdding = true, newReaction)
                    } else {
                        Log.e(TAG, "Failed to add reaction", Exception())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle reaction", e)
            }
        }
    }

    private fun updatePostReactions(
        postId: String,
        emoji: String,
        profileId: String,
        isAdding: Boolean,
        newReaction: ng.commu.data.model.CommunityPostReaction? = null
    ) {
        // Update main post if it matches
        if (_post.value?.id == postId) {
            _post.value = _post.value?.let { post ->
                val updatedReactions = if (isAdding && newReaction != null) {
                    post.reactions?.plus(newReaction) ?: listOf(newReaction)
                } else {
                    post.reactions?.filterNot { it.emoji == emoji && it.user?.id == profileId }
                }
                post.copy(reactions = updatedReactions)
            }
        } else {
            // Update reply post
            _replies.value = updateReplyReactions(_replies.value, postId, emoji, profileId, isAdding, newReaction)
        }
    }

    private fun updateReplyReactions(
        replies: List<CommunityPost>,
        postId: String,
        emoji: String,
        profileId: String,
        isAdding: Boolean,
        newReaction: ng.commu.data.model.CommunityPostReaction?
    ): List<CommunityPost> {
        return replies.map { reply ->
            if (reply.id == postId) {
                val updatedReactions = if (isAdding && newReaction != null) {
                    reply.reactions?.plus(newReaction) ?: listOf(newReaction)
                } else {
                    reply.reactions?.filterNot { it.emoji == emoji && it.user?.id == profileId }
                }
                reply.copy(reactions = updatedReactions)
            } else if (reply.replies != null) {
                reply.copy(
                    replies = updateReplyReactions(reply.replies, postId, emoji, profileId, isAdding, newReaction)
                )
            } else {
                reply
            }
        }
    }

    private fun findPostInReplies(replies: List<CommunityPost>, postId: String): CommunityPost? {
        for (reply in replies) {
            if (reply.id == postId) return reply
            reply.replies?.let { nestedReplies ->
                findPostInReplies(nestedReplies, postId)?.let { return it }
            }
        }
        return null
    }

    fun addReaction(postId: String, emoji: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.addReaction(postId, emoji, profileId)
                if (response.isSuccessful) {
                    // Refresh post to get updated reactions
                    refresh(postId, profileId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add reaction", e)
            }
        }
    }

    fun removeReaction(postId: String, emoji: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = postRepository.removeReaction(postId, emoji, profileId)
                if (response.isSuccessful) {
                    // Refresh post to get updated reactions
                    refresh(postId, profileId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove reaction", e)
            }
        }
    }

    fun toggleBookmark(postId: String, isBookmarked: Boolean, profileId: String) {
        viewModelScope.launch {
            try {
                val response = if (isBookmarked) {
                    postRepository.unbookmarkPost(postId, profileId)
                } else {
                    postRepository.bookmarkPost(postId, profileId)
                }

                if (response.isSuccessful) {
                    // Update local state optimistically
                    _post.value = _post.value?.copy(isBookmarked = !isBookmarked)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle bookmark", e)
            }
        }
    }

    fun deletePost(postId: String, profileId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting post $postId")
                val response = postRepository.deleteCommunityPost(postId, profileId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Post deleted successfully")
                    // If it's a reply being deleted, remove it from the replies list
                    _replies.value = _replies.value.filter { it.id != postId }
                } else {
                    Log.e(TAG, "Failed to delete post: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete post", e)
            }
        }
    }
}
