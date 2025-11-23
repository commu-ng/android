package ng.commu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.CommunityPost
import ng.commu.data.remote.ApiService
import javax.inject.Inject

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    companion object {
        private const val TAG = "AnnouncementsViewModel"
    }

    private val _announcements = MutableStateFlow<List<CommunityPost>>(emptyList())
    val announcements: StateFlow<List<CommunityPost>> = _announcements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadAnnouncements(profileId: String?) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = apiService.getAnnouncements(profileId)
                if (response.isSuccessful) {
                    _announcements.value = response.body()?.data ?: emptyList()
                    Log.d(TAG, "Loaded ${_announcements.value.size} announcements")
                } else {
                    _errorMessage.value = "Failed to load announcements: ${response.code()}"
                    Log.e(TAG, "Failed to load announcements: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load announcements"
                Log.e(TAG, "Failed to load announcements", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh(profileId: String?) {
        _announcements.value = emptyList()
        loadAnnouncements(profileId)
    }
}
