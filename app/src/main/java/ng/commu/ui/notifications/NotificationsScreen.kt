package ng.commu.ui.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ng.commu.R
import ng.commu.data.model.Notification
import ng.commu.data.model.User
import ng.commu.viewmodel.NotificationViewModel
import ng.commu.viewmodel.NotificationsUiState
import ng.commu.viewmodel.ProfileContextViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onPostClick: (String) -> Unit = {},
    onSwitchToConsole: () -> Unit = {},
    profileViewModel: ProfileContextViewModel = hiltViewModel(),
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val currentProfile by profileViewModel.currentProfile.collectAsState()
    val notificationsState by viewModel.notificationsState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentProfile?.id) {
        viewModel.loadNotifications(currentProfile?.id, refresh = true)
        viewModel.loadUnreadCount(currentProfile?.id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onSwitchToConsole) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.nav_console)
                    )
                }
            },
            title = { Text(stringResource(R.string.nav_notifications)) },
            actions = {
                if (notificationsState is NotificationsUiState.Success) {
                    IconButton(
                        onClick = {
                            viewModel.markAllAsRead()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Done,
                            contentDescription = stringResource(R.string.notification_mark_all_read)
                        )
                    }
                }
            }
        )
        when (val state = notificationsState) {
            is NotificationsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is NotificationsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message.ifEmpty { stringResource(R.string.error_load_notifications) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = {
                            viewModel.loadNotifications(currentProfile?.id, refresh = true)
                        }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }

            is NotificationsUiState.Success, is NotificationsUiState.LoadingMore -> {
                val notifications = when (state) {
                    is NotificationsUiState.Success -> state.notifications
                    is NotificationsUiState.LoadingMore -> state.notifications
                    else -> emptyList()
                }

                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = {
                        viewModel.loadNotifications(currentProfile?.id, refresh = true)
                        viewModel.loadUnreadCount(currentProfile?.id)
                    }
                ) {
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.notification_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(notifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)

                                        // Navigate to related post if available
                                        notification.relatedPost?.id?.let { postId ->
                                            onPostClick(postId)
                                        }
                                    }
                                )
                                HorizontalDivider()

                                // Trigger infinite scroll when user reaches the last item
                                if (notification.id == notifications.lastOrNull()?.id) {
                                    LaunchedEffect(notification.id) {
                                        viewModel.loadNotifications(currentProfile?.id, refresh = false)
                                    }
                                }
                            }

                            if (state is NotificationsUiState.LoadingMore) {
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

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isUnread = notification.readAt == null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isUnread) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification type icon
            Icon(
                imageVector = when (notification.type.orEmpty()) {
                    "reply" -> Icons.Filled.Edit
                    "mention" -> Icons.Filled.Email
                    "reaction" -> Icons.Filled.Favorite
                    else -> Icons.Filled.Notifications
                },
                contentDescription = null,
                tint = when (notification.type.orEmpty()) {
                    "reply" -> MaterialTheme.colorScheme.primary
                    "mention" -> MaterialTheme.colorScheme.secondary
                    "reaction" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.content.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notification.communityName != null) {
                        Text(
                            text = notification.communityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatTimestamp(context, notification.createdAt.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
        }
    }
}

private fun formatTimestamp(context: Context, timestamp: String): String {
    return try {
        // Parse PostgreSQL timestamp format: "2025-11-17 20:43:27.110565+09"
        // First, try the ISO 8601 format with 'Z' suffix
        var date: Date? = null
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            date = isoFormat.parse(timestamp)
        } catch (e: Exception) {
            // Try PostgreSQL timestamp format with timezone offset
            val pgFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSXXX", Locale.US)
            date = pgFormat.parse(timestamp)
        }

        val now = Date()
        val diffInMillis = now.time - (date?.time ?: 0)
        val diffInSeconds = diffInMillis / 1000
        val diffInMinutes = diffInSeconds / 60
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24

        when {
            diffInSeconds < 60 -> context.getString(R.string.time_just_now)
            diffInMinutes < 60 -> context.getString(R.string.time_minutes_ago, diffInMinutes)
            diffInHours < 24 -> context.getString(R.string.time_hours_ago, diffInHours)
            diffInDays < 7 -> context.getString(R.string.time_days_ago, diffInDays)
            else -> {
                val displaySdf = SimpleDateFormat("MMM d", Locale.US)
                displaySdf.format(date ?: Date())
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}
