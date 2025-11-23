package ng.commu.ui.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ng.commu.R
import ng.commu.ui.app.components.PostCard
import ng.commu.viewmodel.BookmarksViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {},
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    bookmarksViewModel: BookmarksViewModel = hiltViewModel()
) {
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val posts by bookmarksViewModel.posts.collectAsState()
    val isLoading by bookmarksViewModel.isLoading.collectAsState()
    val isLoadingMore by bookmarksViewModel.isLoadingMore.collectAsState()
    val hasMore by bookmarksViewModel.hasMore.collectAsState()

    val listState = rememberLazyListState()

    // Load bookmarks when profile changes
    LaunchedEffect(currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            bookmarksViewModel.loadBookmarks(profileId)
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
                    bookmarksViewModel.loadMoreBookmarks()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_bookmarks)) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                posts.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.profile_no_bookmarks),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.profile_bookmarks_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
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
                                        bookmarksViewModel.toggleReaction(post.id, profileId, emoji)
                                    }
                                },
                                onBookmarkClick = {
                                    currentProfile?.id?.let { profileId ->
                                        bookmarksViewModel.unbookmarkPost(post.id, profileId)
                                    }
                                },
                                onProfileClick = onProfileClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }

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
