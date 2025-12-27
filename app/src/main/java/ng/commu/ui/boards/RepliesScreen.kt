package ng.commu.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ng.commu.R
import ng.commu.data.model.BoardPostReply
import ng.commu.viewmodel.BoardsViewModel
import ng.commu.viewmodel.RepliesUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@Composable
fun RepliesScreen(
    boardSlug: String,
    postId: String,
    viewModel: BoardsViewModel = hiltViewModel()
) {
    val repliesState by viewModel.repliesState.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMoreReplies.collectAsStateWithLifecycle()
    val isCreatingReply by viewModel.isCreatingReply.collectAsStateWithLifecycle()

    var replyText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<BoardPostReply?>(null) }

    LaunchedEffect(boardSlug, postId) {
        viewModel.loadReplies(boardSlug, postId, refresh = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Replies list
        when (val state = repliesState) {
            is RepliesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is RepliesUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.replies) { reply ->
                        ReplyItem(
                            reply = reply,
                            depth = 0,
                            onReplyClick = { replyingTo = it }
                        )
                    }

                    if (state.hasMore) {
                        item {
                            Button(
                                onClick = { viewModel.loadMoreReplies(boardSlug, postId) },
                                enabled = !isLoadingMore,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(stringResource(R.string.replies_load_more))
                                }
                            }
                        }
                    }
                }
            }
            is RepliesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Reply composition bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replying to @${replyingTo?.author?.loginName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { replyingTo = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.replies_write_placeholder)) },
                        maxLines = 3,
                        enabled = !isCreatingReply
                    )

                    Button(
                        onClick = {
                            viewModel.createReply(
                                boardSlug = boardSlug,
                                postId = postId,
                                content = replyText,
                                inReplyToId = replyingTo?.id,
                                onSuccess = {
                                    replyText = ""
                                    replyingTo = null
                                }
                            )
                        },
                        enabled = replyText.trim().isNotEmpty() && !isCreatingReply
                    ) {
                        if (isCreatingReply) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.action_post))
                        }
                    }
                }
            }
        }
    }
}

// Blue colors for depth-based left border (matching web version)
private val depthBorderColors = listOf(
    Color(0xFF93C5FD), // blue-300
    Color(0xFF60A5FA), // blue-400
    Color(0xFF3B82F6), // blue-500
    Color(0xFF2563EB), // blue-600
    Color(0xFF1D4ED8)  // blue-700
)

@Composable
fun ReplyItem(
    reply: BoardPostReply,
    depth: Int,
    onReplyClick: (BoardPostReply) -> Unit
) {
    // Visual depth is capped at 5
    val visualDepth = min(depth, 5)
    val indentationPadding = (visualDepth * 16).dp
    val isReply = depth > 0
    val borderColor = if (isReply) {
        depthBorderColors[min(depth - 1, depthBorderColors.size - 1)]
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentationPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left border indicator (like web version)
            if (isReply) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(borderColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Author and metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isReply) {
                            Text(
                                text = "↳",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "@${reply.author.loginName}",
                            style = if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formatRelativeTime(reply.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Content
                Text(
                    text = reply.content,
                    style = if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                )

                // Actions
                TextButton(
                    onClick = { onReplyClick(reply) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Reply",
                        fontSize = if (isReply) 11.sp else 12.sp
                    )
                }
            }
        }

        // Nested replies
        reply.replies?.forEach { nestedReply ->
            Spacer(modifier = Modifier.height(8.dp))
            ReplyItem(
                reply = nestedReply,
                depth = depth + 1,
                onReplyClick = onReplyClick
            )
        }
    }
}

private fun formatRelativeTime(dateString: String): String {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val date = formatter.parse(dateString)

        if (date != null) {
            val now = Date()
            val diff = now.time - date.time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "just now"
            }
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
