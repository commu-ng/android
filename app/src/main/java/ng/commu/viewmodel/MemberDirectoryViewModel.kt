package ng.commu.viewmodel

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
class MemberDirectoryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadProfiles(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = profileRepository.getAllProfiles(profileId)
                if (response.isSuccessful) {
                    response.body()?.data?.let { profiles ->
                        _profiles.value = profiles
                    }
                } else {
                    _errorMessage.value = "Failed to load members: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load members"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh(profileId: String) {
        loadProfiles(profileId)
    }
}
