package ng.commu.ui.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ng.commu.R
import ng.commu.viewmodel.PostComposerViewModel
import ng.commu.viewmodel.ProfileContextViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostComposerScreen(
    inReplyToPostId: String? = null,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    composerViewModel: PostComposerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val content by composerViewModel.content.collectAsState()
    val selectedImages by composerViewModel.selectedImages.collectAsState()
    val isAnnouncement by composerViewModel.isAnnouncement.collectAsState()
    val contentWarning by composerViewModel.contentWarning.collectAsState()
    val isUploading by composerViewModel.isUploading.collectAsState()
    val isSubmitting by composerViewModel.isSubmitting.collectAsState()
    val errorMessage by composerViewModel.errorMessage.collectAsState()

    // Mention system state
    val showMentionDropdown by composerViewModel.showMentionDropdown.collectAsState()
    val mentionProfiles by composerViewModel.mentionProfiles.collectAsState()

    // Scheduling state
    val isScheduled by composerViewModel.isScheduled.collectAsState()
    val scheduledDateTime by composerViewModel.scheduledDateTime.collectAsState()

    var showContentWarningDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Load profiles for mentions
    LaunchedEffect(Unit) {
        composerViewModel.loadProfiles()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            if (selectedImages.size < PostComposerViewModel.MAX_IMAGES) {
                composerViewModel.addImage(uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inReplyToPostId != null) stringResource(R.string.action_reply) else stringResource(R.string.composer_new_post)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            currentProfile?.id?.let { profileId ->
                                composerViewModel.createPost(
                                    profileId = profileId,
                                    inReplyToId = inReplyToPostId,
                                    onSuccess = {
                                        composerViewModel.reset()
                                        onPostCreated()
                                    }
                                )
                            }
                        },
                        enabled = content.isNotBlank() && !isSubmitting && !isUploading && currentProfile != null
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.action_post))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile info
            currentProfile?.let { profile ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.composer_posting_as),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }

            // Content input with mention dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { composerViewModel.updateContent(it) },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = {
                        Text(
                            if (inReplyToPostId != null) stringResource(R.string.composer_placeholder_reply)
                            else stringResource(R.string.composer_placeholder_post)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                // Mention dropdown
                if (showMentionDropdown && mentionProfiles.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp)
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 8.dp
                    ) {
                        LazyColumn {
                            items(mentionProfiles.take(5)) { profile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { composerViewModel.selectMention(profile) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = profile.profilePictureUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )

                                    Column {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
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

            // Image preview
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        ImagePreviewItem(
                            uri = uri,
                            onRemove = { composerViewModel.removeImage(uri) }
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add image button
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = selectedImages.size < PostComposerViewModel.MAX_IMAGES
                ) {
                    Icon(Icons.Filled.Image, contentDescription = stringResource(R.string.cd_add_image))
                }

                // Content warning
                FilterChip(
                    selected = contentWarning != null,
                    onClick = { showContentWarningDialog = true },
                    label = { Text(if (contentWarning != null) stringResource(R.string.composer_cw_label, contentWarning!!) else stringResource(R.string.composer_content_warning)) }
                )

                // Announcement (if not a reply)
                if (inReplyToPostId == null) {
                    FilterChip(
                        selected = isAnnouncement,
                        onClick = { composerViewModel.toggleAnnouncement() },
                        label = { Text(stringResource(R.string.composer_announcement)) }
                    )
                }
            }

            // Scheduling section (only for new posts, not replies)
            if (inReplyToPostId == null) {
                FilterChip(
                    selected = isScheduled,
                    onClick = { composerViewModel.toggleScheduling() },
                    label = { Text(stringResource(R.string.composer_schedule)) }
                )

                if (isScheduled && scheduledDateTime != null) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = scheduledDateTime!!.format(
                                        DateTimeFormatter.ofPattern("MMM d, yyyy")
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = scheduledDateTime!!.format(
                                        DateTimeFormatter.ofPattern("h:mm a")
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Change",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (scheduledDateTime!!.isBefore(ZonedDateTime.now())) {
                        Text(
                            text = stringResource(R.string.composer_scheduled_future),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Upload status
            if (isUploading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.composer_uploading_images),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Content warning dialog
    if (showContentWarningDialog) {
        var warningText by remember { mutableStateOf(contentWarning ?: "") }
        AlertDialog(
            onDismissRequest = { showContentWarningDialog = false },
            title = { Text(stringResource(R.string.composer_content_warning)) },
            text = {
                OutlinedTextField(
                    value = warningText,
                    onValueChange = { warningText = it },
                    label = { Text(stringResource(R.string.composer_cw_placeholder)) },
                    placeholder = { Text(stringResource(R.string.composer_cw_example)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        composerViewModel.setContentWarning(warningText.ifBlank { null })
                        showContentWarningDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showContentWarningDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledDateTime?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis() + 3600000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                            val currentDateTime = scheduledDateTime ?: ZonedDateTime.now().plusHours(1)
                            composerViewModel.setScheduledDateTime(
                                newDate.withHour(currentDateTime.hour)
                                    .withMinute(currentDateTime.minute)
                            )
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.composer_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val currentTime = scheduledDateTime ?: ZonedDateTime.now().plusHours(1)
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.composer_select_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduledDateTime?.let { date ->
                            composerViewModel.setScheduledDateTime(
                                date.withHour(timePickerState.hour)
                                    .withMinute(timePickerState.minute)
                            )
                        }
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ImagePreviewItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(2.dp)
                )
            }
        }
    }
}
