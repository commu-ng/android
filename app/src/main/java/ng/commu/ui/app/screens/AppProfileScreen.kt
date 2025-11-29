package ng.commu.ui.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import ng.commu.ui.app.components.ProfileSwitcherDialog
import ng.commu.viewmodel.CommunityContextViewModel
import ng.commu.viewmodel.ProfileContextViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileScreen(
    communityContextViewModel: CommunityContextViewModel = hiltViewModel(),
    profileContextViewModel: ProfileContextViewModel = hiltViewModel(),
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProfileSettings: () -> Unit = {},
    onNavigateToScheduledPosts: () -> Unit = {},
    onNavigateToAnnouncements: () -> Unit = {}
) {
    val currentProfile by profileContextViewModel.currentProfile.collectAsState()
    val profiles by profileContextViewModel.availableProfiles.collectAsState()
    val currentProfileId by profileContextViewModel.currentProfileId.collectAsState()

    var showProfileSwitcher by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item {
                currentProfile?.let { profile ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.profilePictureUrl != null) {
                                AsyncImage(
                                    model = profile.profilePictureUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        profile.bio?.let { bio ->
                            if (bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (profile.isPrimary) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.profile_primary),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(onClick = onNavigateToProfileSettings) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.profile_edit))
                        }
                    }
                } ?: run {
                    // No profile selected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.profile_no_profile),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.profile_select_community),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Profile Switcher
            item {
                if (profiles.size > 1) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProfileSwitcher = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_switch_profile),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.profile_profiles_available, profiles.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Text(
                    text = stringResource(R.string.profile_quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        // Bookmarks
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_bookmarks)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable(onClick = onNavigateToBookmarks)
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                        // Search
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_search_posts)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable(onClick = onNavigateToSearch)
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                        // Scheduled Posts
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_scheduled_posts)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable(onClick = onNavigateToScheduledPosts)
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                        // Announcements
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_announcements)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable(onClick = onNavigateToAnnouncements)
                        )
                    }
                }
            }
        }
    }

    // Profile Switcher Dialog
    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onDismiss = { showProfileSwitcher = false },
            onProfileSelected = { profile ->
                profileContextViewModel.switchProfile(profile)
            },
            onCreateProfile = {
                // Navigate to profile creation
                showProfileSwitcher = false
            }
        )
    }
}
