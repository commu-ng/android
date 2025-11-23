package ng.commu.ui.boards

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.noties.markwon.Markwon
import ng.commu.R
import ng.commu.data.model.BoardPostReply
import ng.commu.data.model.Post
import ng.commu.utils.formatFullDate
import ng.commu.utils.formatRelativeTime
import ng.commu.viewmodel.AuthViewModel
import ng.commu.viewmodel.BoardsViewModel
import ng.commu.viewmodel.PostDetailUiState
import ng.commu.viewmodel.RepliesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    boardSlug: String,
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: BoardsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val postDetailState by viewModel.postDetailState.collectAsState()
    val selectedBoard by viewModel.selectedBoard.collectAsState()
    val repliesState by viewModel.repliesState.collectAsState()
    val isCreatingReply by viewModel.isCreatingReply.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var replyText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<BoardPostReply?>(null) }

    LaunchedEffect(boardSlug, postId) {
        viewModel.loadBoard(boardSlug)
        viewModel.loadPostDetail(boardSlug, postId)
    }

    // Load replies when board is loaded and allows comments
    LaunchedEffect(selectedBoard?.id, selectedBoard?.allowComments) {
        if (selectedBoard?.allowComments == true) {
            viewModel.loadReplies(boardSlug, postId, refresh = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_post)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Reply composition bar (only for authenticated users)
            if (selectedBoard?.allowComments == true && currentUser != null) {
                ReplyCompositionBar(
                    replyText = replyText,
                    onReplyTextChange = { replyText = it },
                    replyingTo = replyingTo,
                    onCancelReply = { replyingTo = null },
                    onSubmitReply = {
                        viewModel.createReply(
                            boardSlug = boardSlug,
                            postId = postId,
                            content = replyText,
                            inReplyToId = replyingTo?.id
                        )
                        replyText = ""
                        replyingTo = null
                    },
                    isSubmitting = isCreatingReply
                )
            }
        }
    ) { padding ->
        when (val state = postDetailState) {
            is PostDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PostDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PostContent(
                            post = state.post,
                            currentUserId = currentUser?.id,
                            boardSlug = boardSlug,
                            viewModel = viewModel,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    // Show replies only if board allows comments
                    if (selectedBoard?.allowComments == true) {
                        item {
                            HorizontalDivider()
                        }

                        item {
                            Text(
                                text = stringResource(R.string.comments_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        when (repliesState) {
                            is RepliesUiState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            is RepliesUiState.Success -> {
                                val replies = (repliesState as RepliesUiState.Success).replies
                                if (replies.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.empty_no_comments),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    items(replies) { reply ->
                                        ReplyItem(
                                            reply = reply,
                                            depth = 0,
                                            currentUserId = currentUser?.id,
                                            boardSlug = boardSlug,
                                            postId = postId,
                                            viewModel = viewModel,
                                            onReplyClick = { replyingTo = it }
                                        )
                                    }
                                }
                            }
                            is RepliesUiState.Error -> {
                                item {
                                    Text(
                                        text = (repliesState as RepliesUiState.Error).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is PostDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadPostDetail(boardSlug, postId) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostContent(
	post: Post,
	currentUserId: String?,
	boardSlug: String,
	viewModel: BoardsViewModel,
	onNavigateBack: () -> Unit
) {
	var showImageDialog by remember { mutableStateOf(false) }
	var showDeleteDialog by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val markwon = remember { Markwon.create(context) }
	val isAuthor = currentUserId != null && currentUserId == post.author.id

	Column(
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (post.author.avatarUrl != null) {
				AsyncImage(
					model = post.author.avatarUrl,
					contentDescription = "User avatar",
					modifier = Modifier
						.size(32.dp)
						.clip(CircleShape)
				)
			} else {
				Icon(
					imageVector = Icons.Filled.Person,
					contentDescription = "User avatar",
					modifier = Modifier.size(32.dp),
					tint = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = post.author.loginName,
					style = MaterialTheme.typography.bodyMedium
				)
				Text(
					text = formatFullDate(post.createdAt),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			// Delete button (only for author)
			if (isAuthor) {
				IconButton(onClick = { showDeleteDialog = true }) {
					Icon(
						Icons.Default.Delete,
						contentDescription = "Delete post",
						tint = MaterialTheme.colorScheme.error
					)
				}
			}
		}

		if (post.image != null) {
			AsyncImage(
				model = post.image.url,
				contentDescription = "Post image",
				modifier = Modifier
					.fillMaxWidth()
					.clip(MaterialTheme.shapes.medium)
					.clickable { showImageDialog = true }
			)
		}

		Text(
			text = post.title,
			style = MaterialTheme.typography.headlineSmall
		)

		if (post.content != null) {
			val textColor = MaterialTheme.colorScheme.onSurface
			AndroidView(
				factory = { context ->
					TextView(context)
				},
				update = { textView ->
					textView.setTextColor(textColor.hashCode())
					markwon.setMarkdown(textView, post.content)
				},
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	if (showImageDialog && post.image != null) {
		ImageZoomDialog(
			imageUrl = post.image.url,
			onDismiss = { showImageDialog = false }
		)
	}

	// Delete confirmation dialog
	if (showDeleteDialog) {
		AlertDialog(
			onDismissRequest = { showDeleteDialog = false },
			title = { Text(stringResource(R.string.dialog_delete_post_title)) },
			text = { Text(stringResource(R.string.dialog_delete_post_message)) },
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteDialog = false
						viewModel.deletePost(
							boardSlug = boardSlug,
							postId = post.id,
							onSuccess = { onNavigateBack() },
							onError = { /* Handle error */ }
						)
					}
				) {
					Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(onClick = { showDeleteDialog = false }) {
					Text(stringResource(R.string.action_cancel))
				}
			}
		)
	}
}

@Composable
fun ReplyCompositionBar(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    replyingTo: BoardPostReply?,
    onCancelReply: () -> Unit,
    onSubmitReply: () -> Unit,
    isSubmitting: Boolean = false
) {
    Column {
        // Show replying indicator
        if (replyingTo != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.comments_replying_to, replyingTo.author.loginName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = replyingTo.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onCancelReply) {
                        Icon(Icons.Default.Close, "Cancel reply")
                    }
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = onReplyTextChange,
                placeholder = { Text(stringResource(R.string.comments_placeholder)) },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onSubmitReply,
                enabled = replyText.isNotBlank() && !isSubmitting
            ) {
                Text(stringResource(if (isSubmitting) R.string.action_posting else R.string.action_post))
            }
        }
    }
}

@Composable
fun ReplyItem(
    reply: BoardPostReply,
    depth: Int,
    currentUserId: String?,
    boardSlug: String,
    postId: String,
    viewModel: BoardsViewModel,
    onReplyClick: (BoardPostReply) -> Unit
) {
    val visualDepth = minOf(depth, 5)
    val indentPadding = (visualDepth * 20).dp
    val isAuthor = currentUserId != null && currentUserId == reply.author.id
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column {
        // Main reply content with indentation
        Column(
            modifier = Modifier.padding(start = indentPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (visualDepth > 0) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reply.author.avatarUrl != null) {
                            AsyncImage(
                                model = reply.author.avatarUrl,
                                contentDescription = "User avatar",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User avatar",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = reply.author.loginName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = formatRelativeTime(reply.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Delete button (only for author)
                        if (isAuthor) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete comment",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = reply.content,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Reply button (only for authenticated users)
                    if (currentUserId != null) {
                        TextButton(
                            onClick = { onReplyClick(reply) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Reply",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_reply), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Nested replies (outside the padded column)
        reply.replies?.forEach { nestedReply ->
            Spacer(modifier = Modifier.height(8.dp))
            ReplyItem(
                reply = nestedReply,
                depth = depth + 1,
                currentUserId = currentUserId,
                boardSlug = boardSlug,
                postId = postId,
                viewModel = viewModel,
                onReplyClick = onReplyClick
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_comment_title)) },
            text = { Text(stringResource(R.string.dialog_delete_comment_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteReply(
                            boardSlug = boardSlug,
                            postId = postId,
                            replyId = reply.id,
                            onSuccess = { /* Replies will auto-refresh */ }
                        )
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
