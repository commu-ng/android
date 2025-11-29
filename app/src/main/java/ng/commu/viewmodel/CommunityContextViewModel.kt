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
import ng.commu.data.local.CommunityContextManager
import ng.commu.data.model.Community
import ng.commu.data.repository.CommunityRepository
import javax.inject.Inject

@HiltViewModel
class CommunityContextViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val communityRepository: CommunityRepository,
    private val communityContextManager: CommunityContextManager
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityContextVM"
        private const val PREFS_NAME = "community_context"
        private const val KEY_COMMUNITY_ID = "current_community_id"
    }

    private val _currentCommunityId = MutableStateFlow<String?>(null)
    val currentCommunityId: StateFlow<String?> = _currentCommunityId.asStateFlow()

    private val _currentCommunity = MutableStateFlow<Community?>(null)
    val currentCommunity: StateFlow<Community?> = _currentCommunity.asStateFlow()

    private val _availableCommunities = MutableStateFlow<List<Community>>(emptyList())
    val availableCommunities: StateFlow<List<Community>> = _availableCommunities.asStateFlow()
    val communities: StateFlow<List<Community>> = _availableCommunities.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // Start as true since we load in init
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Restore last selected community from SharedPreferences
        val savedId = prefs.getString(KEY_COMMUNITY_ID, null)
        _currentCommunityId.value = savedId

        // Load communities from API
        loadCommunities()
    }

    fun loadCommunities() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            communityRepository.getUserCommunities()
                .onSuccess { communities ->
                    _availableCommunities.value = communities

                    // If we have a saved community ID, find and set it
                    val savedId = _currentCommunityId.value
                    if (savedId != null) {
                        val community = communities.firstOrNull { it.id == savedId }
                        if (community != null) {
                            _currentCommunity.value = community
                            communityContextManager.setCommunity(community)
                        }
                    }

                    // If no community is set, select the first one
                    if (_currentCommunity.value == null && communities.isNotEmpty()) {
                        switchCommunity(communities.first())
                    }
                }
                .onFailure { error ->
                    _errorMessage.value = "Failed to load communities: ${error.message}"
                    Log.e(TAG, "Failed to load communities", error)
                }

            _isLoading.value = false
        }
    }

    fun switchCommunity(community: Community) {
        _currentCommunityId.value = community.id
        _currentCommunity.value = community

        // Set community context in manager for Origin header
        communityContextManager.setCommunity(community)

        // Persist selection
        prefs.edit().putString(KEY_COMMUNITY_ID, community.id).apply()

        Log.d(TAG, "Switched to community: ${community.name} (ID: ${community.id})")
        Log.d(TAG, "  Slug: ${community.slug}")
        Log.d(TAG, "  Custom Domain: ${community.customDomain ?: "none"}")
        Log.d(TAG, "  Domain Verified: ${community.domainVerified ?: "none"}")
    }

    fun clearCurrentCommunity() {
        _currentCommunityId.value = null
        _currentCommunity.value = null
        communityContextManager.setCommunity(null)
        prefs.edit().remove(KEY_COMMUNITY_ID).apply()
    }

    fun refreshCurrentCommunity() {
        loadCommunities()
    }
}
