package ng.commu.ui.console.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import ng.commu.data.model.CommunityDetails
import ng.commu.data.model.CommunityLink
import ng.commu.data.repository.CommunityRepository
import ng.commu.viewmodel.CommunityContextViewModel
import javax.inject.Inject

sealed class CommunityDetailUiState {
    object Loading : CommunityDetailUiState()
    data class Success(
        val details: CommunityDetails,
        val links: List<CommunityLink>
    ) : CommunityDetailUiState()
    data class Error(val message: String) : CommunityDetailUiState()
}

@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityDetailUiState>(CommunityDetailUiState.Loading)
    val uiState: StateFlow<CommunityDetailUiState> = _uiState.asStateFlow()

    fun loadCommunityDetails(slug: String) {
        viewModelScope.launch {
            _uiState.value = CommunityDetailUiState.Loading

            val detailsDeferred = async { communityRepository.getCommunityDetails(slug) }
            val linksDeferred = async { communityRepository.getCommunityLinks(slug) }

            val detailsResult = detailsDeferred.await()
            val linksResult = linksDeferred.await()

            if (detailsResult.isSuccess) {
                _uiState.value = CommunityDetailUiState.Success(
                    details = detailsResult.getOrThrow(),
                    links = linksResult.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = CommunityDetailUiState.Error(
                    detailsResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityDetailScreen(
    slug: String,
    onNavigateBack: () -> Unit,
    onSwitchToApp: (Community) -> Unit = {},
    onApply: (slug: String, name: String) -> Unit = { _, _ -> },
    communityContextViewModel: CommunityContextViewModel = hiltViewModel(),
    viewModel: CommunityDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableCommunities by communityContextViewModel.availableCommunities.collectAsState()
    val context = LocalContext.current

    // Check if user is a member of this community
    val memberCommunity = availableCommunities.find { it.slug == slug }

    LaunchedEffect(slug) {
        viewModel.loadCommunityDetails(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is CommunityDetailUiState.Success -> Text(
                            text = state.details.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        else -> Text("")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is CommunityDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CommunityDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadCommunityDetails(slug) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            is CommunityDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Banner Image
                    if (state.details.bannerImageUrl != null) {
                        AsyncImage(
                            model = state.details.bannerImageUrl,
                            contentDescription = stringResource(R.string.cd_community_banner),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title and Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.details.name,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.weight(1f)
                            )

                            if (state.details.isRecruiting) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = stringResource(R.string.status_recruiting),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Hashtags
                        if (state.details.hashtags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.details.hashtags.forEach { hashtag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "#${hashtag.tag}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Description
                        if (!state.details.description.isNullOrEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.community_description),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = state.details.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Apply Button (if recruiting and not a member)
                        if (state.details.isRecruiting && state.details.membershipStatus != "member") {
                            Button(
                                onClick = { onApply(slug, state.details.name) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.community_apply))
                            }
                        }

                        // Links Section
                        if (state.links.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.community_links),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                state.links.forEach { link ->
                                    Surface(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                            context.startActivity(intent)
                                        },
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Link,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = link.title,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = link.url,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Visit Community Button (only for members)
                        if (memberCommunity != null) {
                            Button(
                                onClick = { onSwitchToApp(memberCommunity) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.community_visit))
                            }
                        }
                    }
                }
            }
        }
    }
}
