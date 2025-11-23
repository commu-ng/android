package ng.commu.ui.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ng.commu.R
import ng.commu.ui.app.components.PostCard
import ng.commu.ui.app.components.ProfileSwitcherDialog
import ng.commu.viewmodel.CommunityContextViewModel
import ng.commu.viewmodel.PostListViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onPostClick: (String) -> Unit,
    onComposeClick: () -> Unit,
    onSwitchToConsole: () -> Unit = {},
    onCreateProfile: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    communityViewModel: CommunityContextViewModel = hiltViewModel(),
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    postListViewModel: PostListViewModel = hiltViewModel()
) {
    val currentCommunity by communityViewModel.currentCommunity.collectAsState()
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val profiles by profileViewModel.profiles.collectAsState()
    val isLoadingProfiles by profileViewModel.isLoading.collectAsState()
    val posts by postListViewModel.posts.collectAsState()
    val isLoading by postListViewModel.isLoading.collectAsState()
    val isLoadingMore by postListViewModel.isLoadingMore.collectAsState()
    val hasMore by postListViewModel.hasMore.collectAsState()
    val errorMessage by postListViewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    var showProfileSwitcher by remember { mutableStateOf(false) }

    // Load posts when profile changes
    LaunchedEffect(currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            postListViewModel.loadPosts(profileId)
        }
    }

    // Infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= posts.size - 3 &&
                    hasMore &&
                    !isLoadingMore) {
                    postListViewModel.loadMorePosts()
                }
            }
    }

    // Show profile switcher dialog
    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            profiles = profiles,
            currentProfileId = currentProfile?.id,
            onDismiss = { showProfileSwitcher = false },
            onProfileSelected = { profile ->
                profileViewModel.switchProfile(profile)
            },
            onCreateProfile = onCreateProfile,
            isLoading = isLoadingProfiles
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onSwitchToConsole) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.nav_console)
                        )
                    }
                },
                title = {
                    Text(
                        text = currentCommunity?.name ?: stringResource(R.string.home_select_community),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (currentCommunity != null) {
                        TextButton(onClick = { showProfileSwitcher = true }) {
                            Text(
                                if (currentProfile != null) "@${currentProfile?.username}"
                                else "Select Profile"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentProfile != null) {
                FloatingActionButton(onClick = onComposeClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_new_post))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                currentCommunity == null -> {
                    // No community selected
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_community),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_select_community_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                currentProfile == null -> {
                    // No profile selected
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_profile),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_create_profile, currentCommunity?.name ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                isLoading && posts.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && posts.isEmpty() -> {
                    // Error state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                currentProfile?.id?.let { postListViewModel.refresh(it) }
                            }
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                posts.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_posts),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_be_first),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Posts list with pull-to-refresh
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = {
                            currentProfile?.id?.let { postListViewModel.refresh(it) }
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentProfileId = currentProfile?.id,
                                onPostClick = onPostClick,
                                onReactionClick = { emoji ->
                                    currentProfile?.id?.let { profileId ->
                                        postListViewModel.toggleReaction(post.id, profileId, emoji)
                                    }
                                },
                                onBookmarkClick = {
                                    currentProfile?.id?.let { profileId ->
                                        if (post.isBookmarked == true) {
                                            postListViewModel.unbookmarkPost(post.id, profileId)
                                        } else {
                                            postListViewModel.bookmarkPost(post.id, profileId)
                                        }
                                    }
                                },
                                onProfileClick = onProfileClick,
                                onDeleteClick = {
                                    currentProfile?.id?.let { profileId ->
                                        postListViewModel.deletePost(post.id, profileId)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }

                        // Loading more indicator
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
