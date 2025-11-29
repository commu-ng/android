package ng.commu.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.Profile
import ng.commu.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ProfileContextViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileContextVM"
        private const val PREFS_NAME = "profile_context"
        private const val KEY_PREFIX = "current_profile_id_"
    }

    private val _currentProfileId = MutableStateFlow<String?>(null)
    val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _availableProfiles = MutableStateFlow<List<Profile>>(emptyList())
    val availableProfiles: StateFlow<List<Profile>> = _availableProfiles.asStateFlow()
    val profiles: StateFlow<List<Profile>> = _availableProfiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // Start as true to prevent flickering
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Track the current community ID for profile persistence
    private var _currentCommunityId: String? = null

    // Load profiles for the current community
    fun loadProfiles(communityId: String) {
        _currentCommunityId = communityId

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = profileRepository.getMyProfiles()
                if (response.isSuccessful) {
                    // API returns profiles filtered by community via Origin header
                    val profiles = response.body()?.data ?: emptyList()
                    _availableProfiles.value = profiles

                    // Restore saved profile for this community, or select primary (default) profile
                    val savedId = prefs.getString(KEY_PREFIX + communityId, null)
                    val savedProfile = savedId?.let { id -> profiles.firstOrNull { it.id == id } }
                    val primaryProfile = profiles.firstOrNull { it.isPrimary }
                    val profile = savedProfile ?: primaryProfile ?: profiles.firstOrNull()

                    if (profile != null) {
                        selectProfile(profile, communityId)
                    } else {
                        _currentProfile.value = null
                        _currentProfileId.value = null
                    }
                } else {
                    _errorMessage.value = "Failed to load profiles: ${response.code()}"
                    Log.e(TAG, "Failed to load profiles: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading profiles: ${e.localizedMessage}"
                Log.e(TAG, "Failed to load profiles", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectProfile(profile: Profile, communityId: String) {
        _currentProfileId.value = profile.id
        _currentProfile.value = profile

        // Persist selection per community
        prefs.edit().putString(KEY_PREFIX + communityId, profile.id).apply()

        Log.d(TAG, "Selected profile: ${profile.name} (@${profile.username})")
    }

    fun clearCurrentProfile() {
        _currentProfileId.value = null
        _currentProfile.value = null
    }

    fun switchProfile(profile: Profile) {
        val communityId = _currentCommunityId ?: return
        selectProfile(profile, communityId)
    }

    fun refreshProfiles(communityId: String) {
        loadProfiles(communityId)
    }
}
