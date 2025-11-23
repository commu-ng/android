package ng.commu.ui.navigation

sealed class Screen(val route: String) {
    // Console routes (existing board system)
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Boards : Screen("boards")
    object BoardDetail : Screen("board_detail/{boardId}") {
        fun createRoute(boardId: String) = "board_detail/$boardId"
    }
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object CreatePost : Screen("create_post/{boardSlug}") {
        fun createRoute(boardSlug: String) = "create_post/$boardSlug"
    }
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")

    // App routes (new community system)
    object HomeFeed : Screen("home_feed")
    object AppPostDetail : Screen("app_post_detail/{postId}") {
        fun createRoute(postId: String) = "app_post_detail/$postId"
    }
    object PostComposer : Screen("post_composer?inReplyToId={inReplyToId}") {
        fun createRoute(inReplyToId: String? = null) =
            if (inReplyToId != null) "post_composer?inReplyToId=$inReplyToId"
            else "post_composer"
    }
    object Messages : Screen("messages")
    object Chat : Screen("chat/{otherProfileId}/{otherProfileName}") {
        fun createRoute(otherProfileId: String, otherProfileName: String) =
            "chat/$otherProfileId/$otherProfileName"
    }
    object Search : Screen("search")
    object Bookmarks : Screen("bookmarks")
    object ScheduledPosts : Screen("scheduled_posts")
    object Announcements : Screen("announcements")
    object AppProfile : Screen("app_profile")
    object ProfileSettings : Screen("profile_settings")
    object ProfileDetail : Screen("profile_detail/{username}") {
        fun createRoute(username: String) = "profile_detail/$username"
    }
    object ConsoleCommunities : Screen("console_communities")
    object AccountSettings : Screen("account_settings")
}
