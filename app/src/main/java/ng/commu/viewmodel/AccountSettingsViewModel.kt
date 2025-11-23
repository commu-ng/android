package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.User
import ng.commu.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = accountRepository.getCurrentUser()
                if (response.isSuccessful) {
                    _currentUser.value = response.body()?.data
                } else {
                    _errorMessage.value = "Failed to load user info: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load user info: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _errorMessage.value = null

            try {
                val response = accountRepository.deleteAccount()
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to delete account: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete account: ${e.message}"
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                accountRepository.changePassword(currentPassword, newPassword)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to change password: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                accountRepository.changeEmail(newEmail, password)
                _successMessage.value = "Verification email sent! Please check your inbox."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to change email: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
