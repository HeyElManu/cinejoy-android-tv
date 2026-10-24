package com.cinejoy.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mainRoot: FrameLayout
    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var virtualMouseView: VirtualMouseView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tvMenuOverlay: View

    private lateinit var webChromeClient: TVWebChromeClient
    private lateinit var webViewClient: TVWebViewClient

    private var backPressedOnce = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val CINEJOY_HOME_URL = "https://cinejoy.pk"
        const val DESKTOP_TV_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 TV"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupMenuButtons()

        // Load Cinejoy homepage
        webView.loadUrl(CINEJOY_HOME_URL)
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.main_root)
        webView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        virtualMouseView = findViewById(R.id.virtualMouseView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        tvMenuOverlay = findViewById(R.id.tvMenuOverlay)

        virtualMouseView.attachTargetView(webView)
        virtualMouseView.visibility = View.VISIBLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        settings.userAgentString = DESKTOP_TV_USER_AGENT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webChromeClient = TVWebChromeClient(
            mainContainer = mainRoot,
            customViewContainer = customViewContainer,
            webView = webView,
            progressBar = loadingProgressBar
        ) { isFullscreen ->
            // Cursor remains permanently available in fullscreen to control playback
            virtualMouseView.visibility = View.VISIBLE
        }
        webView.webChromeClient = webChromeClient

        webViewClient = TVWebViewClient {
            webView.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)
        }
        webView.webViewClient = webViewClient
    }

    private fun setupMenuButtons() {
        val btnHome = tvMenuOverlay.findViewById<Button>(R.id.btnMenuHome)
        val btnReload = tvMenuOverlay.findViewById<Button>(R.id.btnMenuReload)
        val btnBack = tvMenuOverlay.findViewById<Button>(R.id.btnMenuBack)
        val btnClose = tvMenuOverlay.findViewById<Button>(R.id.btnMenuClose)
        val btnExit = tvMenuOverlay.findViewById<Button>(R.id.btnMenuExit)

        btnHome.setOnClickListener {
            toggleMenu(false)
            webView.loadUrl(CINEJOY_HOME_URL)
        }

        btnReload.setOnClickListener {
            toggleMenu(false)
            webView.reload()
        }

        btnBack.setOnClickListener {
            toggleMenu(false)
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        btnClose.setOnClickListener {
            toggleMenu(false)
        }

        btnExit.setOnClickListener {
            finish()
        }
    }

    private fun toggleMenu(show: Boolean? = null) {
        val shouldShow = show ?: (tvMenuOverlay.visibility != View.VISIBLE)
        if (shouldShow) {
            tvMenuOverlay.visibility = View.VISIBLE
            tvMenuOverlay.findViewById<Button>(R.id.btnMenuHome).requestFocus()
        } else {
            tvMenuOverlay.visibility = View.GONE
        }
    }

    // ==========================================
    // Remote Control Key Management
    // Exclusively routes D-Pad to Virtual Mouse
    // ==========================================

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 1. Menu button triggers Quick Menu HUD
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleMenu()
            return true
        }

        // 2. If Quick Menu is showing, let Android handle D-Pad navigation between buttons
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                toggleMenu(false)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 3. Exclusively handle all D-Pad directional & enter keys via Virtual Mouse
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                virtualMouseView.handleRemoteKey(keyCode, event)
                return true // Strictly consumed: will never reach video player
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            return super.onKeyUp(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                virtualMouseView.handleRemoteKey(keyCode, event)
                return true
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            toggleMenu(false)
            return
        }

        if (webChromeClient.isFullscreen) {
            webChromeClient.onHideCustomView()
            return
        }

        if (webView.canGoBack()) {
            webView.goBack()
            return
        }

        if (backPressedOnce) {
            super.onBackPressed()
            return
        }

        backPressedOnce = true
        Toast.makeText(this, getString(R.string.toast_exit), Toast.LENGTH_SHORT).show()
        mainHandler.postDelayed({ backPressedOnce = false }, 2500)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
