package ng.commu.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ng.commu.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import ng.commu.data.model.Board
import ng.commu.data.model.Post
import ng.commu.data.model.User
import ng.commu.ui.auth.LoginScreen
import ng.commu.ui.auth.SignUpScreen
import ng.commu.ui.boards.BoardDetailScreen
import ng.commu.ui.boards.BoardsListScreen
import ng.commu.ui.boards.CreatePostScreen
import ng.commu.ui.boards.PostDetailScreen
import ng.commu.ui.profile.ProfileScreen
import ng.commu.ui.notifications.NotificationsScreen
import ng.commu.ui.app.screens.HomeFeedScreen
import ng.commu.ui.app.screens.PostDetailScreen as AppPostDetailScreen
import ng.commu.ui.app.screens.PostComposerScreen
import ng.commu.ui.app.screens.MessagesScreen
import ng.commu.ui.app.screens.ChatScreen
import ng.commu.ui.app.screens.SearchScreen
import ng.commu.ui.app.screens.BookmarksScreen
import ng.commu.ui.app.screens.ProfileDetailScreen
import ng.commu.ui.app.screens.ScheduledPostsScreen
import ng.commu.ui.app.screens.AnnouncementsScreen
import ng.commu.ui.app.screens.ProfileSettingsScreen
import ng.commu.viewmodel.AuthState
import ng.commu.viewmodel.AuthViewModel
import ng.commu.viewmodel.BoardsViewModel
import ng.commu.viewmodel.BoardsUiState
import ng.commu.viewmodel.NotificationViewModel
import ng.commu.viewmodel.AppMode
import ng.commu.viewmodel.AppModeViewModel
import ng.commu.viewmodel.CommunityContextViewModel
import ng.commu.viewmodel.ProfileContextViewModel
import androidx.compose.runtime.LaunchedEffect
import ng.commu.ui.app.screens.AppProfileScreen
import ng.commu.ui.console.screens.ConsoleCommunityScreen
import ng.commu.ui.console.screens.AccountSettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    notificationTapData: ng.commu.NotificationTapData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            MainScreen(
                navController = navController,
                authViewModel = authViewModel,
                authState = authState,
                currentUser = currentUser,
                notificationTapData = notificationTapData,
                onNotificationHandled = onNotificationHandled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    authState: AuthState,
    currentUser: User?,
    notificationTapData: ng.commu.NotificationTapData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthenticated = authState is AuthState.Authenticated

    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val appModeViewModel: AppModeViewModel = hiltViewModel()
    val currentMode by appModeViewModel.currentMode.collectAsState()

    // Shared context ViewModels for community and profile
    val communityContextViewModel: CommunityContextViewModel = hiltViewModel()
    val profileContextViewModel: ProfileContextViewModel = hiltViewModel()

    // Load communities when authenticated
    val currentCommunity by communityContextViewModel.currentCommunity.collectAsState()
    val currentProfile by profileContextViewModel.currentProfile.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            communityContextViewModel.loadCommunities()
        }
    }

    // Load profiles when community changes
    LaunchedEffect(currentCommunity?.id) {
        currentCommunity?.id?.let { communityId ->
            profileContextViewModel.loadProfiles(communityId)
        }
    }

    // Load unread notification count when profile changes
    LaunchedEffect(currentProfile?.id) {
        currentProfile?.id?.let { profileId ->
            notificationViewModel.loadUnreadCount(profileId)
        }
    }

    // Handle notification tap for board post navigation
    LaunchedEffect(notificationTapData) {
        notificationTapData?.let { data ->
            if ((data.type == "board_post_comment" || data.type == "board_post_reply") &&
                data.boardSlug != null && data.boardPostId != null) {
                // Navigate to the board post detail
                navController.navigate("post_detail/${data.boardSlug}/${data.boardPostId}") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                }
                onNotificationHandled()
            } else if (data.navigateToNotificationsTab && isAuthenticated) {
                // Navigate to notifications tab
                navController.navigate(Screen.Notifications.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }

                // Open community URL in browser after navigating to notifications tab
                data.communityUrl?.let { url ->
                    val context = navController.context
                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                }

                onNotificationHandled()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                if (isAuthenticated) {
                    when (currentMode) {
                        AppMode.APP -> {
                            // Home Feed
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_home_kr)) },
                                selected = currentRoute == Screen.HomeFeed.route,
                                onClick = {
                                    navController.navigate(Screen.HomeFeed.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            // Messages
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_messages_kr)) },
                                selected = currentRoute == Screen.Messages.route,
                                onClick = {
                                    navController.navigate(Screen.Messages.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            // Notifications
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge {
                                                    Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Notifications, contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(R.string.nav_notifications_kr)) },
                                selected = currentRoute == Screen.Notifications.route,
                                onClick = {
                                    navController.navigate(Screen.Notifications.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            // Profile
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_profile_kr)) },
                                selected = currentRoute == Screen.AppProfile.route,
                                onClick = {
                                    navController.navigate(Screen.AppProfile.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        AppMode.CONSOLE -> {
                            // Boards
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_boards_kr)) },
                                selected = currentRoute == Screen.Boards.route,
                                onClick = {
                                    navController.navigate(Screen.Boards.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            // Communities
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_communities_kr)) },
                                selected = currentRoute == Screen.ConsoleCommunities.route,
                                onClick = {
                                    navController.navigate(Screen.ConsoleCommunities.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            // Account Settings
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.ManageAccounts, contentDescription = null) },
                                label = { Text(stringResource(R.string.nav_account_kr)) },
                                selected = currentRoute == Screen.AccountSettings.route,
                                onClick = {
                                    navController.navigate(Screen.AccountSettings.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                } else {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                        label = { Text(stringResource(R.string.auth_sign_in)) },
                        selected = currentRoute == Screen.Login.route,
                        onClick = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) Screen.HomeFeed.route else Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    authState = authState,
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    authViewModel = authViewModel,
                    authState = authState,
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onSignUpSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Boards.route) {
                val boardsViewModel: BoardsViewModel = hiltViewModel()
                BoardsListScreen(
                    onBoardClick = { board ->
                        val boardSlug = board.slug ?: board.id
                        navController.navigate("board_detail/$boardSlug")
                    },
                    onSwitchToApp = {
                        appModeViewModel.switchMode(AppMode.APP)
                        navController.navigate(Screen.HomeFeed.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    viewModel = boardsViewModel
                )
            }

            composable(
                route = "board_detail/{boardSlug}",
                arguments = listOf(navArgument("boardSlug") { type = NavType.StringType })
            ) { backStackEntry ->
                val boardsViewModel: BoardsViewModel = hiltViewModel()
                val slug = backStackEntry.arguments?.getString("boardSlug") ?: return@composable

                LaunchedEffect(slug) {
                    boardsViewModel.loadBoard(slug)
                }

                val boardsState by boardsViewModel.boardsState.collectAsState()
                when (val state = boardsState) {
                    is BoardsUiState.Success -> {
                        val board = state.boards.firstOrNull()
                        if (board != null) {
                            val boardSlug = board.slug ?: slug

                            BoardDetailScreen(
                                board = board,
                                boardSlug = boardSlug,
                                isAuthenticated = isAuthenticated,
                                onNavigateBack = { navController.popBackStack() },
                                onPostClick = { post ->
                                    navController.navigate("post_detail/$boardSlug/${post.id}")
                                },
                                onCreatePost = { slug ->
                                    navController.navigate("create_post/$slug")
                                },
                                viewModel = boardsViewModel
                            )
                        }
                    }
                    else -> {
                        // Loading or error state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            composable(
                route = "post_detail/{boardSlug}/{postId}",
                arguments = listOf(
                    navArgument("boardSlug") { type = NavType.StringType },
                    navArgument("postId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val boardsViewModel: BoardsViewModel = hiltViewModel()
                val boardSlug = backStackEntry.arguments?.getString("boardSlug") ?: return@composable
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable

                PostDetailScreen(
                    boardSlug = boardSlug,
                    postId = postId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = boardsViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(
                route = "create_post/{boardSlug}",
                arguments = listOf(navArgument("boardSlug") { type = NavType.StringType })
            ) { backStackEntry ->
                val boardsViewModel: BoardsViewModel = hiltViewModel()
                val boardSlug = backStackEntry.arguments?.getString("boardSlug") ?: return@composable

                CreatePostScreen(
                    navController = navController,
                    boardSlug = boardSlug,
                    viewModel = boardsViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    currentUser = currentUser,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    profileViewModel = profileContextViewModel,
                    viewModel = notificationViewModel
                )
            }

            // App screens (new community system)
            composable(Screen.HomeFeed.route) {
                HomeFeedScreen(
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    onComposeClick = {
                        navController.navigate(Screen.PostComposer.createRoute())
                    },
                    onSwitchToConsole = {
                        appModeViewModel.switchMode(AppMode.CONSOLE)
                        navController.navigate(Screen.Boards.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateProfile = {
                        navController.navigate(Screen.ProfileSettings.route)
                    },
                    onProfileClick = { username ->
                        navController.navigate(Screen.ProfileDetail.createRoute(username))
                    },
                    communityViewModel = communityContextViewModel,
                    profileViewModel = profileContextViewModel
                )
            }

            composable(
                route = Screen.AppPostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                AppPostDetailScreen(
                    postId = postId,
                    onBackClick = { navController.popBackStack() },
                    onReplyClick = {
                        navController.navigate(Screen.PostComposer.createRoute(inReplyToId = postId))
                    },
                    onPostClick = { replyPostId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(replyPostId))
                    },
                    onProfileClick = { username ->
                        navController.navigate(Screen.ProfileDetail.createRoute(username))
                    },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(
                route = Screen.PostComposer.route,
                arguments = listOf(
                    navArgument("inReplyToId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val inReplyToId = backStackEntry.arguments?.getString("inReplyToId")
                PostComposerScreen(
                    inReplyToPostId = inReplyToId,
                    onDismiss = { navController.popBackStack() },
                    onPostCreated = {
                        navController.popBackStack()
                    },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(Screen.Messages.route) {
                MessagesScreen(
                    onConversationClick = { profileId, profileName ->
                        navController.navigate(Screen.Chat.createRoute(profileId, profileName))
                    },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("otherProfileId") { type = NavType.StringType },
                    navArgument("otherProfileName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val otherProfileId = backStackEntry.arguments?.getString("otherProfileId") ?: return@composable
                val otherProfileName = backStackEntry.arguments?.getString("otherProfileName") ?: return@composable
                ChatScreen(
                    otherProfileId = otherProfileId,
                    otherProfileName = otherProfileName,
                    onBackClick = { navController.popBackStack() },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    onProfileClick = { username ->
                        navController.navigate(Screen.ProfileDetail.createRoute(username))
                    },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    onProfileClick = { username ->
                        navController.navigate(Screen.ProfileDetail.createRoute(username))
                    },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(
                route = Screen.ProfileDetail.route,
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: return@composable
                ProfileDetailScreen(
                    username = username,
                    onBackClick = { navController.popBackStack() },
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    onChatClick = { profileId, profileName ->
                        navController.navigate(Screen.Chat.createRoute(profileId, profileName))
                    },
                    profileContextViewModel = profileContextViewModel
                )
            }

            composable(Screen.AppProfile.route) {
                AppProfileScreen(
                    onNavigateToBookmarks = {
                        navController.navigate(Screen.Bookmarks.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToProfileSettings = {
                        navController.navigate(Screen.ProfileSettings.route)
                    },
                    onNavigateToScheduledPosts = {
                        navController.navigate(Screen.ScheduledPosts.route)
                    },
                    onNavigateToAnnouncements = {
                        navController.navigate(Screen.Announcements.route)
                    },
                    communityContextViewModel = communityContextViewModel,
                    profileContextViewModel = profileContextViewModel
                )
            }

            composable(Screen.ScheduledPosts.route) {
                ScheduledPostsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(Screen.ProfileSettings.route) {
                ProfileSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    profileViewModel = profileContextViewModel
                )
            }

            composable(Screen.Announcements.route) {
                AnnouncementsScreen(
                    onBackClick = { navController.popBackStack() },
                    onPostClick = { postId ->
                        navController.navigate(Screen.AppPostDetail.createRoute(postId))
                    },
                    profileContextViewModel = profileContextViewModel
                )
            }

            composable(Screen.ConsoleCommunities.route) {
                ConsoleCommunityScreen(
                    communityContextViewModel = communityContextViewModel,
                    onSwitchToApp = {
                        appModeViewModel.switchMode(AppMode.APP)
                        navController.navigate(Screen.HomeFeed.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.AccountSettings.route) {
                AccountSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
