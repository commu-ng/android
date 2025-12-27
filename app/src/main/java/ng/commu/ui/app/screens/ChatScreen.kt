package ng.commu.ui.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ng.commu.R
import ng.commu.data.model.Message
import ng.commu.utils.DateUtils
import ng.commu.viewmodel.ChatViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherProfileId: String,
    otherProfileName: String,
    onBackClick: () -> Unit,
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val messageText by chatViewModel.messageText.collectAsState()
    val selectedImageUris by chatViewModel.selectedImageUris.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val isSending by chatViewModel.isSending.collectAsState()
    val isUploadingImages by chatViewModel.isUploadingImages.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            chatViewModel.addImages(uris)
        }
    }

    // Track if user is at bottom of the list
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem == null || lastVisibleItem.index >= messages.size - 1
        }
    }

    // Load messages when screen opens and start polling
    LaunchedEffect(otherProfileId, currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            chatViewModel.loadMessages(otherProfileId, profileId)
            chatViewModel.startPolling()
        }
    }

    // Stop polling when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.stopPolling()
        }
    }

    // Scroll to bottom on initial load
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() && !isLoading && !hasScrolledToBottom) {
            listState.scrollToItem(messages.size - 1)
            hasScrolledToBottom = true
        }
    }

    // Auto-scroll to bottom when new messages arrive (only if user is at bottom)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isAtBottom && hasScrolledToBottom) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherProfileName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selected images preview
                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedImageUris) { uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected image",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { chatViewModel.removeImage(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .size(16.dp)
                                        )
                                    }
                                    if (isUploadingImages) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .then(
                                                    Modifier.background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = selectedImageUris.size < 4 && !isSending
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Attach image")
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { chatViewModel.updateMessageText(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.chat_message_placeholder)) },
                            maxLines = 5
                        )

                        IconButton(
                            onClick = { chatViewModel.sendMessage(context) },
                            enabled = (messageText.isNotBlank() || selectedImageUris.isNotEmpty()) && !isSending && currentProfile != null
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
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
                isLoading && messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Failed to load messages",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { chatViewModel.refresh() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Messages",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.sender?.id == currentProfile?.id
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isOwnMessage)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isOwnMessage) {
                    Text(
                        text = message.sender?.name ?: "Unknown",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (message.content.isNotBlank() && message.content != " ") {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Display images
                if (message.images.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.images.forEach { image ->
                            AsyncImage(
                                model = image.url,
                                contentDescription = "Message image",
                                modifier = Modifier
                                    .widthIn(max = 250.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Text(
                    text = DateUtils.getRelativeTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
