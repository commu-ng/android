package ng.commu

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ng.commu.ui.navigation.AppNavigation
import ng.commu.ui.theme.CommungTheme

data class NotificationTapData(
    val type: String?,
    val communityUrl: String?,
    val boardSlug: String?,
    val boardPostId: String?,
    val postId: String?,
    val navigateToNotificationsTab: Boolean = false
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationTapData by mutableStateOf<NotificationTapData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        android.util.Log.d("MainActivity", "onCreate called")
        android.util.Log.d("MainActivity", "Intent: action=${intent?.action}, extras=${intent?.extras}")

        // Clear all notifications and badge when app launches
        clearNotificationBadge()

        setContent {
            CommungTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    notificationTapData = notificationTapData,
                    onNotificationHandled = { notificationTapData = null }
                )
            }
        }

        // Handle notification tap after setContent to ensure context is ready
        // Use a slight delay to ensure the activity is fully initialized
        android.util.Log.d("MainActivity", "Posting handleNotificationIntent to decorView")
        window.decorView.post {
            android.util.Log.d("MainActivity", "decorView.post callback executing")
            handleNotificationIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Clear badge when app comes to foreground
        clearNotificationBadge()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "handleNotificationIntent: action=${intent?.action}")

        // Log all extras
        intent?.extras?.let { extras ->
            android.util.Log.d("MainActivity", "Intent extras:")
            for (key in extras.keySet()) {
                android.util.Log.d("MainActivity", "  $key = ${extras.getString(key)}")
            }
        }

        // Check if this is a notification tap by looking for our custom extras
        val type = intent?.getStringExtra("type")
        val communityUrl = intent?.getStringExtra("community_url")

        if (type != null) {
            android.util.Log.d("MainActivity", "Notification tapped - type: $type, communityUrl: $communityUrl")

            when (type) {
                "board_post_comment", "board_post_reply" -> {
                    // Set data for navigation to board post
                    notificationTapData = NotificationTapData(
                        type = type,
                        communityUrl = null,
                        boardSlug = intent?.getStringExtra("board_slug"),
                        boardPostId = intent?.getStringExtra("board_post_id"),
                        postId = null,
                        navigateToNotificationsTab = false
                    )
                }
                "reaction", "reply", "mention", "direct_message" -> {
                    // Navigate to notifications tab and open community URL in browser
                    notificationTapData = NotificationTapData(
                        type = type,
                        communityUrl = communityUrl,
                        boardSlug = null,
                        boardPostId = null,
                        postId = null,
                        navigateToNotificationsTab = true
                    )
                }
                else -> {
                    android.util.Log.w("MainActivity", "Unknown notification type: $type")
                }
            }
        }
    }

    private fun clearNotificationBadge() {
        // Cancel all notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // Clear badge count
        NotificationManagerCompat.from(this).cancelAll()
    }
}