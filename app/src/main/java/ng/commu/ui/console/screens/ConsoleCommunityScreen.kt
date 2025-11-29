package ng.commu.ui.console.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.R
import ng.commu.data.model.Community
import ng.commu.data.repository.CommunityRepository
import ng.commu.viewmodel.CommunityContextViewModel
import javax.inject.Inject

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Success(
        val recruitingCommunities: List<Community>,
        val ongoingCommunities: List<Community>
    ) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}

@HiltViewModel
class CommunityScreenViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _browseState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val browseState: StateFlow<BrowseUiState> = _browseState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadBrowseCommunities()
    }

    fun loadBrowseCommunities() {
        viewModelScope.launch {
            _browseState.value = BrowseUiState.Loading
            loadData()
        }
    }

    fun refresh(onMyCommunityRefresh: suspend () -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            onMyCommunityRefresh()
            loadData()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadData() {
        val recruitingDeferred = viewModelScope.async { communityRepository.getRecruitingCommunities() }
        val ongoingDeferred = viewModelScope.async { communityRepository.getOngoingCommunities() }

        val recruitingResult = recruitingDeferred.await()
        val ongoingResult = ongoingDeferred.await()

        if (recruitingResult.isSuccess && ongoingResult.isSuccess) {
            _browseState.value = BrowseUiState.Success(
                recruitingCommunities = recruitingResult.getOrDefault(emptyList()),
                ongoingCommunities = ongoingResult.getOrDefault(emptyList())
            )
        } else {
            val errorMessage = recruitingResult.exceptionOrNull()?.message
                ?: ongoingResult.exceptionOrNull()?.message
                ?: "Unknown error"
            _browseState.value = BrowseUiState.Error(errorMessage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleCommunityScreen(
    communityContextViewModel: CommunityContextViewModel = hiltViewModel(),
    communityScreenViewModel: CommunityScreenViewModel = hiltViewModel(),
    onSwitchToApp: () -> Unit = {},
    onCommunityClick: (String) -> Unit = {},
    onApplicationsClick: (String) -> Unit = {},
    onCreateCommunity: () -> Unit = {},
    onEditCommunity: (Community) -> Unit = {}
) {
    val communities by communityContextViewModel.availableCommunities.collectAsState()
    val currentCommunityId by communityContextViewModel.currentCommunityId.collectAsState()
    val isLoading by communityContextViewModel.isLoading.collectAsState()
    val browseState by communityScreenViewModel.browseState.collectAsState()
    val isRefreshing by communityScreenViewModel.isRefreshing.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.console_communities_title)) },
                actions = {
                    IconButton(onClick = onCreateCommunity) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.community_create_title)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = {
                        communityScreenViewModel.refresh {
                            communityContextViewModel.loadCommunities()
                        }
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // My Communities Section
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (communities.isNotEmpty()) {
                    item {
                        CommunitySectionHeader(
                            title = stringResource(R.string.communities_my_communities),
                            count = communities.size,
                            description = stringResource(R.string.communities_my_communities_description),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                    }

                    items(communities, key = { "my-${it.id}" }) { community ->
                        MyCommunityCard(
                            community = community,
                            isSelected = community.id == currentCommunityId,
                            onClick = {
                                communityContextViewModel.switchCommunity(community)
                                onSwitchToApp()
                            },
                            onApplicationsClick = {
                                onApplicationsClick(community.slug)
                            },
                            onEditClick = {
                                onEditCommunity(community)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Browse Communities
                when (val state = browseState) {
                    is BrowseUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is BrowseUiState.Error -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { communityScreenViewModel.loadBrowseCommunities() }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }

                    is BrowseUiState.Success -> {
                        // Recruiting Communities Section
                        if (state.recruitingCommunities.isNotEmpty()) {
                            item {
                                CommunitySectionHeader(
                                    title = stringResource(R.string.browse_recruiting),
                                    count = state.recruitingCommunities.size,
                                    description = stringResource(R.string.browse_recruiting_description),
                                    icon = {
                                        Icon(
                                            Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }

                            items(state.recruitingCommunities, key = { "recruiting-${it.id}" }) { community ->
                                BrowseCommunityCard(
                                    community = community,
                                    isRecruiting = true,
                                    onClick = { onCommunityClick(community.slug) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        // Ongoing Communities Section
                        if (state.ongoingCommunities.isNotEmpty()) {
                            item {
                                CommunitySectionHeader(
                                    title = stringResource(R.string.browse_ongoing),
                                    count = state.ongoingCommunities.size,
                                    description = stringResource(R.string.browse_ongoing_description),
                                    icon = {
                                        Icon(
                                            Icons.Filled.PlayCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                )
                            }

                            items(state.ongoingCommunities, key = { "ongoing-${it.id}" }) { community ->
                                BrowseCommunityCard(
                                    community = community,
                                    isRecruiting = false,
                                    onClick = { onCommunityClick(community.slug) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun CommunitySectionHeader(
    title: String,
    count: Int,
    description: String,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MyCommunityCard(
    community: Community,
    isSelected: Boolean,
    onClick: () -> Unit,
    onApplicationsClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Banner Image
            if (community.bannerImageUrl != null) {
                AsyncImage(
                    model = community.bannerImageUrl,
                    contentDescription = stringResource(R.string.cd_community_banner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = community.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Community Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = community.slug,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Role badge
                    community.role?.let { role ->
                        Surface(
                            color = when (role.lowercase()) {
                                "owner" -> MaterialTheme.colorScheme.error
                                "moderator" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = role.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (role.lowercase()) {
                                    "owner" -> MaterialTheme.colorScheme.onError
                                    "moderator" -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pending Applications Badge
                    val pendingCount = community.pendingApplicationCount ?: 0
                    if (pendingCount > 0) {
                        Surface(
                            onClick = onApplicationsClick,
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.communities_pending, pendingCount),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Edit button (only for owner/admin)
                    if (community.role?.lowercase() in listOf("owner", "admin", "moderator")) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.community_edit_title),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseCommunityCard(
    community: Community,
    isRecruiting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Banner Image
            if (community.bannerImageUrl != null) {
                AsyncImage(
                    model = community.bannerImageUrl,
                    contentDescription = stringResource(R.string.cd_community_banner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = community.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Community Info
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = community.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isRecruiting) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.status_recruiting),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Hashtags
                if (community.hashtags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(community.hashtags, key = { it.id }) { hashtag ->
                            Text(
                                text = "#${hashtag.tag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
