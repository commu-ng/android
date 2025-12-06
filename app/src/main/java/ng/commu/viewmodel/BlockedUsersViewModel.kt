package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.BlockedUser
import ng.commu.data.repository.BlockRepository
import javax.inject.Inject

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val blockRepository: BlockRepository
) : ViewModel() {

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUnblocking = MutableStateFlow(false)
    val isUnblocking: StateFlow<Boolean> = _isUnblocking.asStateFlow()

    private val _isBlocking = MutableStateFlow(false)
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    private val _blockSuccess = MutableStateFlow(false)
    val blockSuccess: StateFlow<Boolean> = _blockSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = blockRepository.getBlockedUsers()
                if (response.isSuccessful) {
                    _blockedUsers.value = response.body()?.data ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load blocked users: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load blocked users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _isUnblocking.value = true
            _errorMessage.value = null

            try {
                val response = blockRepository.unblockUser(userId)
                if (response.isSuccessful) {
                    _blockedUsers.value = _blockedUsers.value.filter { it.id != userId }
                } else {
                    _errorMessage.value = "Failed to unblock user: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unblock user: ${e.message}"
            } finally {
                _isUnblocking.value = false
            }
        }
    }

    fun blockUser(userId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBlocking.value = true
            _errorMessage.value = null
            _blockSuccess.value = false

            try {
                val response = blockRepository.blockUser(userId)
                if (response.isSuccessful) {
                    _blockSuccess.value = true
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to block user: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to block user: ${e.message}"
            } finally {
                _isBlocking.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearBlockSuccess() {
        _blockSuccess.value = false
    }
}
