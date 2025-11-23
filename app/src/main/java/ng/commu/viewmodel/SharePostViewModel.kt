package ng.commu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.Conversation
import ng.commu.data.model.CommunityPost
import ng.commu.data.local.CommunityContextManager
import ng.commu.data.repository.MessageRepository
import javax.inject.Inject

@HiltViewModel
class SharePostViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val communityContextManager: CommunityContextManager
) : ViewModel() {

    companion object {
        private const val TAG = "SharePostViewModel"
    }

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedReceiverId = MutableStateFlow<String?>(null)
    val selectedReceiverId: StateFlow<String?> = _selectedReceiverId.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    fun loadConversations(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = messageRepository.getConversations(profileId, limit = 50)
                if (response.isSuccessful) {
                    _conversations.value = response.body()?.data ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to load conversations: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectReceiver(receiverId: String) {
        _selectedReceiverId.value = if (_selectedReceiverId.value == receiverId) null else receiverId
    }

    fun updateMessage(text: String) {
        _message.value = text
    }

    fun sendMessage(
        post: CommunityPost,
        profileId: String,
        onSuccess: () -> Unit
    ) {
        val receiverId = _selectedReceiverId.value ?: return

        viewModelScope.launch {
            _isSending.value = true
            try {
                // Build message with post link using community URL
                val communityUrl = communityContextManager.currentCommunity?.communityUrl
                    ?: "https://commu.ng"
                val postLink = "$communityUrl/posts/${post.id}"
                val fullMessage = if (_message.value.isBlank()) {
                    postLink
                } else {
                    "$postLink\n\n${_message.value.trim()}"
                }

                val response = messageRepository.sendMessage(
                    receiverId = receiverId,
                    profileId = profileId,
                    content = fullMessage,
                    imageIds = null
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully shared post via DM")
                    // Reset state
                    _message.value = ""
                    _selectedReceiverId.value = null
                    onSuccess()
                } else {
                    Log.e(TAG, "Failed to send message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    fun reset() {
        _searchQuery.value = ""
        _selectedReceiverId.value = null
        _message.value = ""
    }
}
