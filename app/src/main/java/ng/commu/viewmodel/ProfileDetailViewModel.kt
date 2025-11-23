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
import ng.commu.data.model.Profile
import ng.commu.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileDetailViewModel"
    }

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(false)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var nextCursor: String? = null
    private var currentUsername: String? = null
    private var currentProfileId: String? = null

    fun loadProfile(username: String, profileId: String?) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentUsername = username
            currentProfileId = profileId

            try {
                // Load profile
                val profileResponse = profileRepository.getProfileByUsername(username, profileId)
                if (profileResponse.isSuccessful) {
                    _profile.value = profileResponse.body()?.data
                    Log.d(TAG, "Loaded profile for $username")
                } else {
                    _errorMessage.value = "Failed to load profile: ${profileResponse.code()}"
                    Log.e(TAG, "Failed to load profile: ${profileResponse.errorBody()?.string()}")
                    _isLoading.value = false
                    return@launch
                }

                // Load posts
                _isLoadingPosts.value = true
                val postsResponse = profileRepository.getProfilePosts(
                    username = username,
                    profileId = profileId,
                    limit = 20,
                    cursor = null
                )

                if (postsResponse.isSuccessful) {
                    val body = postsResponse.body()
                    if (body != null) {
                        _posts.value = body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} posts for $username")
                    }
                } else {
                    Log.e(TAG, "Failed to load posts: ${postsResponse.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load profile"
                Log.e(TAG, "Failed to load profile", e)
            } finally {
                _isLoading.value = false
                _isLoadingPosts.value = false
            }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingPosts.value || !_hasMore.value || nextCursor == null || currentUsername == null) {
            return
        }

        viewModelScope.launch {
            _isLoadingPosts.value = true

            try {
                val response = profileRepository.getProfilePosts(
                    username = currentUsername!!,
                    profileId = currentProfileId,
                    limit = 20,
                    cursor = nextCursor
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _posts.value = _posts.value + body.data
                        nextCursor = body.pagination.nextCursor
                        _hasMore.value = body.pagination.hasMore
                        Log.d(TAG, "Loaded ${body.data.size} more posts")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more posts", e)
            } finally {
                _isLoadingPosts.value = false
            }
        }
    }

    fun refresh(username: String, profileId: String?) {
        nextCursor = null
        _posts.value = emptyList()
        loadProfile(username, profileId)
    }
}
