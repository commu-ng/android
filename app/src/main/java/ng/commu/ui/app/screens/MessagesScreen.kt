package ng.commu.ui.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ng.commu.R
import ng.commu.data.model.Conversation
import ng.commu.data.model.Profile
import ng.commu.utils.DateUtils
import ng.commu.viewmodel.ConversationsViewModel
import ng.commu.viewmodel.MemberDirectoryViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (String, String) -> Unit,
    onSwitchToConsole: () -> Unit = {},
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    conversationsViewModel: ConversationsViewModel = hiltViewModel(),
    memberDirectoryViewModel: MemberDirectoryViewModel = hiltViewModel()
) {
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val conversations by conversationsViewModel.conversations.collectAsState()
    val isLoading by conversationsViewModel.isLoading.collectAsState()
    val isLoadingMore by conversationsViewModel.isLoadingMore.collectAsState()
    val hasMore by conversationsViewModel.hasMore.collectAsState()
    val unreadCount by conversationsViewModel.unreadCount.collectAsState()
    val errorMessage by conversationsViewModel.errorMessage.collectAsState()

    // Member directory for new message dialog
    val allProfiles by memberDirectoryViewModel.profiles.collectAsState()
    val isLoadingProfiles by memberDirectoryViewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()
    var showNewMessageDialog by remember { mutableStateOf(false) }

    // Load conversations when profile changes
    LaunchedEffect(currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            conversationsViewModel.loadConversations(profileId)
            conversationsViewModel.loadUnreadCount(profileId)
        }
    }

    // Infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= conversations.size - 3 &&
                    hasMore &&
                    !isLoadingMore) {
                    conversationsViewModel.loadMoreConversations()
                }
            }
    }

    // New message dialog
    if (showNewMessageDialog) {
        NewMessageDialog(
            profiles = allProfiles.filter { it.id != currentProfile?.id },
            currentProfileId = currentProfile?.id,
            isLoading = isLoadingProfiles,
            onDismiss = { showNewMessageDialog = false },
            onProfileSelected = { profile ->
                showNewMessageDialog = false
                onConversationClick(profile.id, profile.name)
            },
            onLoadProfiles = {
                currentProfile?.id?.let { profileId ->
                    memberDirectoryViewModel.loadProfiles(profileId)
                }
            }
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
                        text = if (unreadCount > 0) stringResource(R.string.messages_with_count, unreadCount) else stringResource(R.string.messages_title)
                    )
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { conversationsViewModel.markAllAsRead() }
                        ) {
                            Text(stringResource(R.string.messages_mark_all_read))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentProfile != null) {
                FloatingActionButton(onClick = { showNewMessageDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.messages_new))
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when {
                currentProfile == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.messages_no_profile),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                isLoading && conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && conversations.isEmpty() -> {
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                currentProfile?.id?.let {
                                    conversationsViewModel.refresh(it)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                conversations.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.messages_no_messages),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.messages_start_conversation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(conversations, key = { it.otherProfile.id }) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    onConversationClick(
                                        conversation.otherProfile.id,
                                        conversation.otherProfile.name
                                    )
                                }
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

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (conversation.unreadCount.toIntOrNull() ?: 0 > 0)
            MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            AsyncImage(
                model = conversation.otherProfile.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherProfile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (conversation.unreadCount.toIntOrNull() ?: 0 > 0)
                            androidx.compose.ui.text.font.FontWeight.Bold
                        else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    conversation.lastMessage?.let { message ->
                        Text(
                            text = DateUtils.getRelativeTime(message.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage?.content ?: stringResource(R.string.messages_no_messages_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount.toIntOrNull() ?: 0 > 0) {
                        Badge {
                            Text(conversation.unreadCount)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessageDialog(
    profiles: List<Profile>,
    currentProfileId: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onProfileSelected: (Profile) -> Unit,
    onLoadProfiles: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Load profiles when dialog opens
    LaunchedEffect(Unit) {
        onLoadProfiles()
    }

    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isBlank()) {
            profiles
        } else {
            profiles.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.messages_new)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.messages_search_members)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    filteredProfiles.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.messages_no_members_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredProfiles, key = { it.id }) { profile ->
                                Surface(
                                    onClick = { onProfileSelected(profile) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = profile.profilePictureUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "@${profile.username}",
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
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
