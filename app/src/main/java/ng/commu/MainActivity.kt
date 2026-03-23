package ng.commu

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Tab(val id: String, val name: String, val url: String)

class MainActivity : ComponentActivity() {
    private val tabs = mutableListOf<Tab>()
    private var selectedTabId = "console"
    private val webViews = mutableMapOf<String, WebView>()
    private val loadedTabs = mutableSetOf<String>()
    private var communitiesFetched = false

    private lateinit var urlBarText: TextView
    private lateinit var refreshButton: ImageView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var tabBarContainer: LinearLayout
    private lateinit var tabBarScroll: HorizontalScrollView
    private lateinit var tabBarDivider: View

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        clearNotificationBadge()

        CookieManager.getInstance().setAcceptCookie(true)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val isDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

        val homeButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_home)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener {
                val homeUrl = if (selectedTabId == "console") "https://commu.ng"
                    else "https://$selectedTabId.commu.ng"
                selectTab(selectedTabId, loadUrl = homeUrl)
            }
        }

        urlBarText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setTextColor(0xFF8E8E93.toInt())
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            gravity = Gravity.CENTER
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            background = GradientDrawable().apply {
                setColor(if (isDark) 0xFF2C2C2E.toInt() else 0xFFF2F2F7.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            setOnClickListener {
                val url = text.toString()
                if (url.isNotEmpty()) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                    Toast.makeText(this@MainActivity, "URL copied", Toast.LENGTH_SHORT).show()
                }
            }
        }

        refreshButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_refresh)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener {
                webViews[selectedTabId]?.reload()
            }
        }

        val urlBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            addView(homeButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(urlBarText, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(dpToPx(4), 0, dpToPx(4), 0) })
            addView(refreshButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        rootLayout.addView(urlBarContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        webViewContainer = FrameLayout(this)
        rootLayout.addView(webViewContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        tabBarDivider = View(this).apply {
            visibility = View.GONE
        }
        rootLayout.addView(tabBarDivider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0
        ))

        tabBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }

        tabBarScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabBarContainer)
            visibility = View.GONE
        }
        rootLayout.addView(tabBarScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(rootLayout)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val wv = webViews[selectedTabId]
                if (wv != null && wv.canGoBack()) {
                    wv.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        tabs.add(Tab("console", getString(R.string.nav_console), "https://commu.ng"))
        createWebView("console")
        val notificationUrl = intent?.getStringExtra("url")
        selectTab("console", loadUrl = notificationUrl ?: "https://commu.ng")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.getStringExtra("url") ?: return
        val tabId = findTabIdForUrl(url)
        selectTab(tabId, loadUrl = url)
    }

    override fun onResume() {
        super.onResume()
        clearNotificationBadge()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tabId: String): WebView {
        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportMultipleWindows(false)
            webViewClient = CommungWebViewClient(tabId)
            webChromeClient = WebChromeClient()

        }
        val swipeRefresh = SwipeRefreshLayout(this).apply {
            addView(wv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            setOnRefreshListener { wv.reload() }
            visibility = View.GONE
        }
        webViews[tabId] = wv
        webViewContainer.addView(swipeRefresh, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return wv
    }

    private fun selectTab(tabId: String, loadUrl: String? = null) {
        selectedTabId = tabId

        for ((id, wv) in webViews) {
            val container = wv.parent as? View ?: wv
            container.visibility = if (id == tabId) View.VISIBLE else View.GONE
        }

        val wv = webViews[tabId] ?: return
        urlBarText.text = wv.url ?: ""

        val url = loadUrl ?: tabs.find { it.id == tabId }?.url ?: return
        if (!loadedTabs.contains(tabId) || loadUrl != null) {
            loadedTabs.add(tabId)
            wv.loadUrl(url)
        }

        updateTabBar()
    }

    private fun updateTabBar() {
        tabBarScroll.visibility = View.VISIBLE
        tabBarContainer.removeAllViews()

        val isDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (!communitiesFetched) {
            val loginButton = TextView(this).apply {
                text = getString(R.string.auth_login)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(0xFF007AFF.toInt())
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                background = GradientDrawable().apply {
                    setColor(0x1A007AFF)
                    cornerRadius = dpToPx(20).toFloat()
                }
                setOnClickListener {
                    selectTab("console", loadUrl = "https://commu.ng/login")
                }
            }
            tabBarContainer.addView(loginButton)
            return
        }

        for (tab in tabs) {
            val isSelected = tab.id == selectedTabId
            val button = TextView(this).apply {
                text = tab.name
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create(
                    "sans-serif-medium",
                    if (isSelected) Typeface.BOLD else Typeface.NORMAL
                )
                setTextColor(
                    if (isSelected) 0xFF007AFF.toInt()
                    else if (isDark) 0xFF8E8E93.toInt() else 0xFF8E8E93.toInt()
                )
                setPadding(dpToPx(14), dpToPx(7), dpToPx(14), dpToPx(7))
                background = GradientDrawable().apply {
                    setColor(
                        if (isSelected) 0x1A007AFF
                        else if (isDark) 0x1AFFFFFF else 0x0D000000
                    )
                    cornerRadius = dpToPx(20).toFloat()
                }
                setOnClickListener { selectTab(tab.id) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dpToPx(3), 0, dpToPx(3), 0)
            tabBarContainer.addView(button, lp)
        }
    }

    private fun onCommunitiesLoaded(communities: List<Pair<String, String>>) {
        tabs.removeAll { it.id != "console" }
        for ((slug, name) in communities) {
            val ssoUrl = "https://api.commu.ng/auth/sso?return_to=https://$slug.commu.ng/"
            tabs.add(Tab(slug, name, ssoUrl))
            if (!webViews.containsKey(slug)) {
                createWebView(slug)
            }
        }
        updateTabBar()
    }

    private fun findTabIdForUrl(url: String): String {
        for (tab in tabs) {
            if (tab.id != "console" && url.contains("${tab.id}.commu.ng")) {
                return tab.id
            }
        }
        return "console"
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun clearNotificationBadge() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        NotificationManagerCompat.from(this).cancelAll()
    }

    private var isFetchingCommunities = false

    private fun checkSessionState() {
        val cookies = CookieManager.getInstance().getCookie("https://api.commu.ng")
        val hasSession = cookies != null && cookies.contains("session_token")

        if (hasSession && !communitiesFetched && !isFetchingCommunities) {
            fetchCommunities()
        } else if (!hasSession && communitiesFetched) {
            handleLogout()
        }
    }

    private fun handleLogout() {
        // Deregister push token
        val cookie = CookieManager.getInstance().getCookie("https://api.commu.ng")
        val pushToken = getSharedPreferences("commung", Context.MODE_PRIVATE)
            .getString("fcm_token", null)
        if (pushToken != null && cookie != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://api.commu.ng/console/devices/$pushToken")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "DELETE"
                    conn.setRequestProperty("Cookie", cookie)
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }
        }

        // Remove community webviews
        for (tab in tabs.filter { it.id != "console" }) {
            val wv = webViews.remove(tab.id)
            (wv?.parent as? View)?.let { webViewContainer.removeView(it) }
            loadedTabs.remove(tab.id)
        }

        // Reset state — don't reload console, the web app already navigated to login
        communitiesFetched = false
        tabs.removeAll { it.id != "console" }
        selectedTabId = "console"
        updateTabBar()
    }

    private fun fetchCommunities() {
        val cookie = CookieManager.getInstance().getCookie("https://api.commu.ng") ?: return
        isFetchingCommunities = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.commu.ng/console/communities/mine")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Cookie", cookie)
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val obj = JSONObject(body)
                    val data = obj.getJSONArray("data")
                    val communities = mutableListOf<Pair<String, String>>()
                    for (i in 0 until data.length()) {
                        val c = data.getJSONObject(i)
                        communities.add(c.getString("slug") to c.getString("name"))
                    }
                    withContext(Dispatchers.Main) {
                        isFetchingCommunities = false
                        communitiesFetched = true
                        onCommunitiesLoaded(communities)
                        requestNotificationPermission()
                        fetchAndRegisterPushToken()
                    }
                } else {
                    withContext(Dispatchers.Main) { isFetchingCommunities = false }
                }
                conn.disconnect()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isFetchingCommunities = false }
            }
        }
    }

    private fun fetchAndRegisterPushToken() {
        val cookie = CookieManager.getInstance().getCookie("https://api.commu.ng") ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            getSharedPreferences("commung", Context.MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply()
            // Register device natively
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://api.commu.ng/console/devices")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Cookie", cookie)
                    conn.doOutput = true
                    val body = JSONObject().apply {
                        put("push_token", token)
                        put("platform", "android")
                        put("device_model", Build.MODEL)
                        put("os_version", "Android ${Build.VERSION.RELEASE}")
                        put("app_version", packageManager.getPackageInfo(packageName, 0).versionName)
                    }
                    conn.outputStream.write(body.toString().toByteArray())
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private val spinAnimation = RotateAnimation(
        0f, 360f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        duration = 700
        repeatCount = Animation.INFINITE
        interpolator = LinearInterpolator()
    }

    private fun setRefreshLoading(loading: Boolean) {
        if (loading) {
            refreshButton.startAnimation(spinAnimation)
            refreshButton.setOnClickListener { webViews[selectedTabId]?.stopLoading() }
        } else {
            refreshButton.clearAnimation()
            refreshButton.setOnClickListener { webViews[selectedTabId]?.reload() }
        }
    }

    private inner class CommungWebViewClient(private val tabId: String) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url ?: return false
            val host = url.host ?: return false

            // External links → system browser
            if (!host.endsWith("commu.ng")) {
                startActivity(Intent(Intent.ACTION_VIEW, url))
                return true
            }

            // Cross-subdomain link → open in appropriate tab
            val currentHost = view?.url?.let { android.net.Uri.parse(it).host } ?: ""
            if (host != currentHost && host != "api.commu.ng") {
                val targetTabId = findTabIdForUrl(url.toString())
                selectTab(targetTabId, loadUrl = url.toString())
                return true
            }

            return false
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            if (tabId == selectedTabId) {
                urlBarText.text = url ?: ""
            }
            if (tabId == "console") {
                checkSessionState()
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (tabId == selectedTabId) {
                urlBarText.text = url ?: ""
                setRefreshLoading(true)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            (view?.parent as? SwipeRefreshLayout)?.isRefreshing = false
            if (tabId == selectedTabId) {
                urlBarText.text = url ?: ""
                setRefreshLoading(false)
            }
            if (tabId == "console") {
                checkSessionState()
            }
        }
    }

}
