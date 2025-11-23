package ng.commu.ui.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ng.commu.R
import ng.commu.data.model.CommunityPost
import ng.commu.data.model.Conversation
import ng.commu.ui.components.MarkdownText
import ng.commu.viewmodel.SharePostViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostSheet(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    shareViewModel: SharePostViewModel = hiltViewModel()
) {
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val conversations by shareViewModel.conversations.collectAsState()
    val isLoading by shareViewModel.isLoading.collectAsState()
    val isSending by shareViewModel.isSending.collectAsState()
    val searchQuery by shareViewModel.searchQuery.collectAsState()
    val selectedReceiverId by shareViewModel.selectedReceiverId.collectAsState()
    val message by shareViewModel.message.collectAsState()

    LaunchedEffect(currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            shareViewModel.loadConversations(profileId)
        }
    }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conversation ->
                conversation.otherProfile.name.contains(searchQuery, ignoreCase = true) ||
                conversation.otherProfile.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_post_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_close))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Post preview
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = post.author.profilePictureUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Column {
                            Text(
                                text = post.author.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "@${post.author.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownText(
                        markdown = post.content,
                        maxLines = 3
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { shareViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.share_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversations list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.share_no_conversations),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.share_start_conversation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(filteredConversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                isSelected = selectedReceiverId == conversation.otherProfile.id,
                                onClick = {
                                    shareViewModel.selectReceiver(conversation.otherProfile.id)
                                }
                            )
                        }
                    }
                }
            }

            // Message input and send button
            if (selectedReceiverId != null) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { shareViewModel.updateMessage(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.share_message_placeholder)) },
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            currentProfile?.id?.let { profileId ->
                                shareViewModel.sendMessage(
                                    post = post,
                                    profileId = profileId,
                                    onSuccess = onSent
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(conversation.otherProfile.name)
        },
        supportingContent = {
            Text("@${conversation.otherProfile.username}")
        },
        leadingContent = {
            AsyncImage(
                model = conversation.otherProfile.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.console_selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
