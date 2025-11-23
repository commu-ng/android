package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.User
import ng.commu.data.repository.AuthRepository
import ng.commu.data.repository.DeviceRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        if (authRepository.hasStoredSession()) {
            autoLogin()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(loginName: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.login(loginName, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated

                    // Register device for push notifications
                    registerDeviceForPush()
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun signup(loginName: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.signup(loginName, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated

                    // Register device for push notifications
                    registerDeviceForPush()
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Signup failed")
                }
        }
    }

    private fun registerDeviceForPush() {
        viewModelScope.launch {
            try {
                deviceRepository.registerDevice()
            } catch (e: Exception) {
                // Silently fail - push notification registration is not critical
                // Log the error but don't disrupt the user's login flow
            }
        }
    }

    private fun autoLogin() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.getCurrentUser()
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                }
                .onFailure {
                    _authState.value = AuthState.Unauthenticated
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Delete device registration before logging out
            try {
                val pushToken = deviceRepository.getCurrentPushToken()
                if (pushToken != null) {
                    deviceRepository.deleteDevice(pushToken)
                }
            } catch (e: Exception) {
                // Silently fail - continue with logout even if device deletion fails
            }

            authRepository.logout()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
