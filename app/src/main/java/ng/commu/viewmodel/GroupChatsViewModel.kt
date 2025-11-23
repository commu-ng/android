package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.GroupChat
import ng.commu.data.repository.MessageRepository
import javax.inject.Inject

@HiltViewModel
class GroupChatsViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _groupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChats: StateFlow<List<GroupChat>> = _groupChats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadGroupChats(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = messageRepository.getGroupChats(profileId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _groupChats.value = body.data
                    }
                } else {
                    _errorMessage.value = "Failed to load group chats: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load group chats"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGroupChat(
        name: String,
        profileId: String,
        memberProfileIds: List<String>,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = messageRepository.createGroupChat(
                    name = name,
                    creatorProfileId = profileId,
                    memberProfileIds = memberProfileIds
                )
                if (response.isSuccessful) {
                    response.body()?.let { groupChat ->
                        loadGroupChats(profileId) // Refresh list
                        onSuccess(groupChat.id)
                    }
                } else {
                    _errorMessage.value = "Failed to create group chat: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create group chat"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
