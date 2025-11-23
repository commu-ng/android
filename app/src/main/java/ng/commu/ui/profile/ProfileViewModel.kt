package ng.commu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.Community
import ng.commu.data.repository.CommunityRepository
import javax.inject.Inject

sealed class CommunitiesUiState {
    object Loading : CommunitiesUiState()
    data class Success(val communities: List<Community>) : CommunitiesUiState()
    data class Error(val message: String) : CommunitiesUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _communitiesState = MutableStateFlow<CommunitiesUiState>(CommunitiesUiState.Loading)
    val communitiesState: StateFlow<CommunitiesUiState> = _communitiesState.asStateFlow()

    init {
        loadCommunities()
    }

    fun loadCommunities() {
        viewModelScope.launch {
            _communitiesState.value = CommunitiesUiState.Loading
            val result = communityRepository.getUserCommunities()
            _communitiesState.value = result.fold(
                onSuccess = { CommunitiesUiState.Success(it) },
                onFailure = { CommunitiesUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}
