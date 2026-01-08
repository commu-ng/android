package ng.commu.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.Profile
import ng.commu.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingPicture = MutableStateFlow(false)
    val isUploadingPicture: StateFlow<Boolean> = _isUploadingPicture.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _profilePictureUri = MutableStateFlow<Uri?>(null)
    val profilePictureUri: StateFlow<Uri?> = _profilePictureUri.asStateFlow()

    fun loadProfile(profile: Profile) {
        _currentProfile.value = profile
        _displayName.value = profile.name
        _username.value = profile.username
        _bio.value = profile.bio ?: ""
    }

    fun updateDisplayName(name: String) {
        _displayName.value = name
    }

    fun updateUsername(username: String) {
        _username.value = username
        validateUsername(username)
    }

    fun updateBio(bio: String) {
        if (bio.length <= 500) {
            _bio.value = bio
        }
    }

    private fun validateUsername(username: String) {
        val trimmed = username.trim()

        _usernameError.value = when {
            trimmed.isEmpty() -> "Username is required"
            trimmed.length > 50 -> "Username must be 50 characters or less"
            !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                "Username can only contain letters, numbers, and underscores"
            else -> null
        }
    }

    fun setProfilePictureUri(uri: Uri?) {
        _profilePictureUri.value = uri
    }

    fun uploadProfilePicture(profileId: String, imageData: ByteArray, filename: String) {
        viewModelScope.launch {
            _isUploadingPicture.value = true
            _errorMessage.value = null

            try {
                // Check file size (10MB limit)
                if (imageData.size > 10 * 1024 * 1024) {
                    _errorMessage.value = "Image size must be under 10MB"
                    return@launch
                }

                val response = profileRepository.uploadProfilePicture(profileId, imageData, filename)
                if (response.isSuccessful) {
                    // Profile picture is now uploaded
                    println("Profile picture uploaded: ${response.body()?.data?.url}")
                } else {
                    _errorMessage.value = "Failed to upload profile picture: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upload profile picture: ${e.message}"
            } finally {
                _isUploadingPicture.value = false
            }
        }
    }

    fun saveProfile(profileId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                val response = profileRepository.updateProfile(
                    profileId = profileId,
                    name = _displayName.value.trim(),
                    username = _username.value.trim(),
                    bio = if (_bio.value.trim().isEmpty()) null else _bio.value.trim()
                )
                if (response.isSuccessful) {
                    _currentProfile.value = response.body()?.data
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to update profile: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun hasChanges(): Boolean {
        val profile = _currentProfile.value ?: return false
        return _displayName.value != profile.name ||
                _username.value != profile.username ||
                _bio.value != (profile.bio ?: "") ||
                _profilePictureUri.value != null
    }
}
