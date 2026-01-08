package ng.commu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ng.commu.data.model.Message
import ng.commu.data.repository.MessageRepository
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val postRepository: ng.commu.data.repository.PostRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<android.net.Uri>> = _selectedImageUris.asStateFlow()

    private val _isUploadingImages = MutableStateFlow(false)
    val isUploadingImages: StateFlow<Boolean> = _isUploadingImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var nextCursor: String? = null
    private var currentProfileId: String? = null
    private var otherProfileId: String? = null
    private var pollingJob: Job? = null

    fun loadMessages(
        otherProfileId: String,
        profileId: String,
        refresh: Boolean = false
    ) {
        if (refresh) {
            nextCursor = null
            _messages.value = emptyList()
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            this@ChatViewModel.currentProfileId = profileId
            this@ChatViewModel.otherProfileId = otherProfileId

            try {
                val response = messageRepository.getConversationThread(
                    otherProfileId = otherProfileId,
                    profileId = profileId,
                    limit = 50,
                    cursor = null
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Messages come in chronological order (oldest first)
                        _messages.value = body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} messages")

                        // Mark conversation as read
                        markAsRead(otherProfileId)
                    }
                } else {
                    _errorMessage.value = "Failed to load messages: ${response.code()}"
                    Log.e(TAG, "Failed to load messages: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading messages: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load messages", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreMessages() {
        if (_isLoadingMore.value || !_hasMore.value || nextCursor == null ||
            currentProfileId == null || otherProfileId == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val response = messageRepository.getConversationThread(
                    otherProfileId = otherProfileId!!,
                    profileId = currentProfileId!!,
                    limit = 50,
                    cursor = nextCursor
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Prepend older messages
                        _messages.value = body.data + _messages.value
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} more messages")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more messages", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun addImages(uris: List<android.net.Uri>) {
        val currentImages = _selectedImageUris.value
        val newImages = (currentImages + uris).take(4) // Max 4 images
        _selectedImageUris.value = newImages
    }

    fun removeImage(uri: android.net.Uri) {
        _selectedImageUris.value = _selectedImageUris.value.filter { it != uri }
    }

    fun clearImages() {
        _selectedImageUris.value = emptyList()
    }

    fun sendMessage(context: android.content.Context) {
        val text = _messageText.value.trim()
        val imageUris = _selectedImageUris.value

        if ((text.isEmpty() && imageUris.isEmpty()) || currentProfileId == null || otherProfileId == null) {
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _isUploadingImages.value = imageUris.isNotEmpty()
            _errorMessage.value = null

            try {
                // Upload images first if any
                val imageIds = if (imageUris.isNotEmpty()) {
                    val uploadedIds = mutableListOf<String>()
                    for (uri in imageUris) {
                        try {
                            val file = ng.commu.utils.FileUtils.uriToFile(context, uri)
                            val response = postRepository.uploadImage(file)
                            if (response.isSuccessful) {
                                response.body()?.data?.let { uploadedIds.add(it.id) }
                            } else {
                                Log.e(TAG, "Failed to upload image: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error uploading image", e)
                        }
                    }
                    uploadedIds.takeIf { it.isNotEmpty() }
                } else {
                    null
                }

                _isUploadingImages.value = false

                val response = messageRepository.sendMessage(
                    receiverId = otherProfileId!!,
                    profileId = currentProfileId!!,
                    content = text.ifEmpty { " " }, // Send space if only images
                    imageIds = imageIds
                )

                if (response.isSuccessful) {
                    val message = response.body()?.data
                    if (message != null) {
                        // Add new message to the end
                        _messages.value = _messages.value + message
                        _messageText.value = ""
                        _selectedImageUris.value = emptyList()
                        Log.d(TAG, "Sent message: ${message.id}")
                    }
                } else {
                    _errorMessage.value = "Failed to send message: ${response.code()}"
                    Log.e(TAG, "Failed to send message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending message: ${e.localizedMessage}"
                Log.e(TAG, "Failed to send message", e)
            } finally {
                _isSending.value = false
                _isUploadingImages.value = false
            }
        }
    }

    private fun markAsRead(otherProfileId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markConversationAsRead(otherProfileId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }
        }
    }

    fun refresh() {
        if (otherProfileId != null && currentProfileId != null) {
            loadMessages(otherProfileId!!, currentProfileId!!, refresh = true)
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // 1 second
                pollForNewMessages()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollForNewMessages() {
        if (currentProfileId == null || otherProfileId == null) return

        try {
            val response = messageRepository.getConversationThread(
                otherProfileId = otherProfileId!!,
                profileId = currentProfileId!!,
                limit = 50,
                cursor = null
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val newMessages = body.data
                    if (newMessages.size != _messages.value.size ||
                        (newMessages.isNotEmpty() && _messages.value.isNotEmpty() &&
                         newMessages.last().id != _messages.value.last().id)) {
                        _messages.value = newMessages
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Polling failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
