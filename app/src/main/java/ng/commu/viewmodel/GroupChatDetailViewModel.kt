package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ng.commu.data.model.GroupChat
import ng.commu.data.model.GroupChatMessage
import ng.commu.data.repository.MessageRepository
import javax.inject.Inject

@HiltViewModel
class GroupChatDetailViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _groupChat = MutableStateFlow<GroupChat?>(null)
    val groupChat: StateFlow<GroupChat?> = _groupChat.asStateFlow()

    private val _messages = MutableStateFlow<List<GroupChatMessage>>(emptyList())
    val messages: StateFlow<List<GroupChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadGroupChat(groupChatId: String, profileId: String) {
        viewModelScope.launch {
            try {
                val response = messageRepository.getGroupChat(groupChatId, profileId)
                if (response.isSuccessful) {
                    _groupChat.value = response.body()
                } else {
                    _errorMessage.value = "Failed to load group chat: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load group chat"
            }
        }
    }

    fun loadMessages(groupChatId: String, profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = messageRepository.getGroupChatMessages(groupChatId, profileId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _messages.value = body.data.reversed() // Newest at bottom
                    }
                    // Mark as read
                    messageRepository.markGroupChatAsRead(groupChatId, profileId)
                } else {
                    _errorMessage.value = "Failed to load messages: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(groupChatId: String, profileId: String, content: String) {
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            try {
                messageRepository.sendGroupChatMessage(groupChatId, profileId, content)
                // Reload messages
                loadMessages(groupChatId, profileId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send message"
            } finally {
                _isSending.value = false
            }
        }
    }

    private var pollingJob: kotlinx.coroutines.Job? = null

    // Poll for new messages every 1 second
    fun startPolling(groupChatId: String, profileId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // 1 second
                try {
                    val response = messageRepository.getGroupChatMessages(groupChatId, profileId)
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            val newMessages = body.data.reversed()
                            if (newMessages.size != _messages.value.size ||
                                (newMessages.isNotEmpty() && _messages.value.isNotEmpty() &&
                                 newMessages.last().id != _messages.value.last().id)) {
                                _messages.value = newMessages
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silently fail polling
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
