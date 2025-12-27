package ng.commu.ui.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ng.commu.R
import ng.commu.data.model.CommunityPost
import ng.commu.ui.app.components.PostCard
import ng.commu.ui.components.MarkdownText
import ng.commu.viewmodel.PostDetailViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBackClick: () -> Unit,
    onReplyClick: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    postDetailViewModel: PostDetailViewModel = hiltViewModel()
) {
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val post by postDetailViewModel.post.collectAsState()
    val replies by postDetailViewModel.replies.collectAsState()
    val isLoading by postDetailViewModel.isLoading.collectAsState()
    val errorMessage by postDetailViewModel.errorMessage.collectAsState()

    // Load post when screen opens
    LaunchedEffect(postId, currentProfile?.id) {
        postDetailViewModel.loadPost(postId, currentProfile?.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_post)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && post == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && post == null -> {
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
                            text = errorMessage ?: stringResource(R.string.post_failed_to_load),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                postDetailViewModel.refresh(postId, currentProfile?.id)
                            }
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                post != null -> {
                    val currentPost = post ?: return@Scaffold
                    val parentThread = currentPost.parentThread ?: emptyList()

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Parent thread (if this is a reply)
                        if (parentThread.isNotEmpty()) {
                            items(parentThread, key = { it.id }) { parentPost ->
                                ParentPostCard(
                                    post = parentPost,
                                    onPostClick = onPostClick,
                                    onProfileClick = onProfileClick,
                                    modifier = Modifier.padding(16.dp)
                                )
                                HorizontalDivider()
                            }
                        }

                        // Main post
                        item {
                            PostCard(
                                post = currentPost,
                                currentProfileId = currentProfile?.id,
                                onPostClick = {},
                                onReactionClick = { emoji ->
                                    currentProfile?.id?.let { profileId ->
                                        postDetailViewModel.toggleReaction(postId, emoji, profileId)
                                    }
                                },
                                onBookmarkClick = {
                                    currentProfile?.id?.let { profileId ->
                                        postDetailViewModel.toggleBookmark(
                                            postId,
                                            currentPost.isBookmarked == true,
                                            profileId
                                        )
                                    }
                                },
                                onProfileClick = onProfileClick,
                                onDeleteClick = {
                                    currentProfile?.id?.let { profileId ->
                                        postDetailViewModel.deletePost(postId, profileId)
                                        onBackClick()
                                    }
                                },
                                onReportClick = { reason ->
                                    currentProfile?.id?.let { profileId ->
                                        postDetailViewModel.reportPost(postId, profileId, reason)
                                    }
                                },
                                isDetail = true,
                                modifier = Modifier.padding(16.dp)
                            )
                            HorizontalDivider()
                        }

                        // Reply button
                        if (currentProfile != null) {
                            item {
                                Button(
                                    onClick = onReplyClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_reply))
                                }
                                HorizontalDivider()
                            }
                        }

                        // Replies section
                        if (replies.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.post_replies_count, replies.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            items(replies, key = { it.id }) { reply ->
                                ThreadedReplyView(
                                    reply = reply,
                                    depth = reply.depth ?: 0,
                                    currentProfileId = currentProfile?.id,
                                    onPostClick = onPostClick,
                                    onReactionClick = { emoji ->
                                        currentProfile?.id?.let { profileId ->
                                            postDetailViewModel.toggleReaction(reply.id, emoji, profileId)
                                        }
                                    },
                                    onBookmarkClick = {
                                        currentProfile?.id?.let { profileId ->
                                            postDetailViewModel.toggleBookmark(
                                                reply.id,
                                                reply.isBookmarked == true,
                                                profileId
                                            )
                                        }
                                    },
                                    onProfileClick = onProfileClick,
                                    onDeleteClick = {
                                        currentProfile?.id?.let { profileId ->
                                            postDetailViewModel.deletePost(reply.id, profileId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentPostCard(
    post: ng.commu.data.model.CommunityPostParent,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClick(post.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick(post.author.username) }
            ) {
                // Avatar
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                )

                Column {
                    Text(
                        text = post.author.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = "@${post.author.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            MarkdownText(
                markdown = post.content
            )
        }
    }
}

// Blue colors for depth-based left border (matching web version)
private val depthBorderColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF93C5FD), // blue-300
    androidx.compose.ui.graphics.Color(0xFF60A5FA), // blue-400
    androidx.compose.ui.graphics.Color(0xFF3B82F6), // blue-500
    androidx.compose.ui.graphics.Color(0xFF2563EB), // blue-600
    androidx.compose.ui.graphics.Color(0xFF1D4ED8)  // blue-700
)

@Composable
private fun ThreadedReplyView(
    reply: CommunityPost,
    depth: Int,
    currentProfileId: String?,
    onPostClick: (String) -> Unit,
    onReactionClick: (String) -> Unit,
    onBookmarkClick: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    onDeleteClick: (() -> Unit)? = null
) {
    val indentationWidth = (minOf(depth, 5) * 16).dp
    val isReply = depth > 0
    val borderColor = if (isReply) {
        depthBorderColors[minOf(depth - 1, depthBorderColors.size - 1)]
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentationWidth)
    ) {
        // Left border indicator (like web version)
        if (isReply) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Reply indicator
            if (isReply) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↳",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PostCard(
                post = reply,
                currentProfileId = currentProfileId,
                onPostClick = onPostClick,
                onReactionClick = onReactionClick,
                onBookmarkClick = onBookmarkClick,
                onProfileClick = onProfileClick,
                onDeleteClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isReply) 8.dp else 16.dp),
                isDetail = false,
                isReply = isReply
            )
            HorizontalDivider()
        }
    }
}
