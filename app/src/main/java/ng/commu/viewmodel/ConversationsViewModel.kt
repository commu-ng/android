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
import ng.commu.data.repository.MessageRepository
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ConversationsViewModel"
    }

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var nextCursor: String? = null
    private var currentProfileId: String? = null

    fun loadConversations(profileId: String, refresh: Boolean = false) {
        if (refresh) {
            nextCursor = null
            _conversations.value = emptyList()
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentProfileId = profileId

            try {
                val response = messageRepository.getConversations(
                    profileId = profileId,
                    limit = 20,
                    cursor = null
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _conversations.value = body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} conversations")
                    }
                } else {
                    _errorMessage.value = "Failed to load conversations: ${response.code()}"
                    Log.e(TAG, "Failed to load conversations: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading conversations: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load conversations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreConversations() {
        if (_isLoadingMore.value || !_hasMore.value || nextCursor == null || currentProfileId == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val response = messageRepository.getConversations(
                    profileId = currentProfileId!!,
                    limit = 20,
                    cursor = nextCursor
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _conversations.value = _conversations.value + body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} more conversations")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more conversations", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun loadUnreadCount(profileId: String) {
        viewModelScope.launch {
            try {
                val response = messageRepository.getUnreadMessagesCount(profileId)
                if (response.isSuccessful) {
                    _unreadCount.value = response.body()?.data?.count ?: 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load unread count", e)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val response = messageRepository.markAllConversationsAsRead()
                if (response.isSuccessful) {
                    _unreadCount.value = 0
                    // Update conversations to show as read
                    _conversations.value = _conversations.value.map {
                        it.copy(unreadCount = "0")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark all as read", e)
            }
        }
    }

    fun refresh(profileId: String) {
        loadConversations(profileId, refresh = true)
        loadUnreadCount(profileId)
    }
}
