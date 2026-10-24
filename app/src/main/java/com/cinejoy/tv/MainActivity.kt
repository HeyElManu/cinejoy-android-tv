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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mainRoot: FrameLayout
    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var virtualMouseView: VirtualMouseView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tvHintText: TextView
    private lateinit var tvMenuOverlay: View

    private lateinit var webChromeClient: TVWebChromeClient
    private lateinit var webViewClient: TVWebViewClient

    private var isMouseMode = false
    private var backPressedOnce = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val CINEJOY_HOME_URL = "https://cinejoy.pk"
        // Desktop user-agent ensures full 1080p/4K TV layout with complete video controls
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

        // Automatically fade out the hint bar after 6 seconds
        mainHandler.postDelayed({
            tvHintText.animate().alpha(0f).setDuration(800).withEndAction {
                tvHintText.visibility = View.GONE
            }.start()
        }, 6000)
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.main_root)
        webView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        virtualMouseView = findViewById(R.id.virtualMouseView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        tvHintText = findViewById(R.id.tvHintText)
        tvMenuOverlay = findViewById(R.id.tvMenuOverlay)

        virtualMouseView.attachTargetView(webView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        // Enable essential web features for streaming
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // TV optimizations
        settings.userAgentString = DESKTOP_TV_USER_AGENT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // HTML5 Video & audio playback on TV without requiring a physical touch gesture
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Web clients
        webChromeClient = TVWebChromeClient(
            mainContainer = mainRoot,
            customViewContainer = customViewContainer,
            webView = webView,
            progressBar = loadingProgressBar
        ) { isFullscreen ->
            if (isFullscreen) {
                virtualMouseView.visibility = View.GONE
                tvMenuOverlay.visibility = View.GONE
            } else if (isMouseMode) {
                virtualMouseView.visibility = View.VISIBLE
            }
        }
        webView.webChromeClient = webChromeClient

        webViewClient = TVWebViewClient { loadedUrl ->
            // Injected when page finishes loading
        }
        webView.webViewClient = webViewClient
    }

    private fun setupMenuButtons() {
        val btnHome = tvMenuOverlay.findViewById<Button>(R.id.btnMenuHome)
        val btnToggleMouse = tvMenuOverlay.findViewById<Button>(R.id.btnMenuToggleMouse)
        val btnReload = tvMenuOverlay.findViewById<Button>(R.id.btnMenuReload)
        val btnBack = tvMenuOverlay.findViewById<Button>(R.id.btnMenuBack)
        val btnClose = tvMenuOverlay.findViewById<Button>(R.id.btnMenuClose)
        val btnExit = tvMenuOverlay.findViewById<Button>(R.id.btnMenuExit)

        btnHome.setOnClickListener {
            toggleMenu(false)
            webView.loadUrl(CINEJOY_HOME_URL)
        }

        btnToggleMouse.setOnClickListener {
            toggleMouseMode(!isMouseMode)
            btnToggleMouse.text = if (isMouseMode) {
                "🕹️ Activer Mode D-Pad Focus"
            } else {
                "🖱️ Activer Mode Curseur Souris"
            }
            toggleMenu(false)
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
            webView.requestFocus()
        }
    }

    private fun toggleMouseMode(enable: Boolean) {
        isMouseMode = enable
        if (isMouseMode) {
            virtualMouseView.visibility = View.VISIBLE
            Toast.makeText(this, getString(R.string.toast_mouse_enabled), Toast.LENGTH_SHORT).show()
        } else {
            virtualMouseView.visibility = View.GONE
            Toast.makeText(this, getString(R.string.toast_dpad_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // Remote Control Key Management
    // ==========================================

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 1. Menu button triggers HUD
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleMenu()
            return true
        }

        // 2. If Quick Menu is showing, let Android handle navigation between menu buttons
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                toggleMenu(false)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 3. If in Mouse Mode, route D-Pad and Center to the VirtualMouseView
        if (isMouseMode) {
            if (virtualMouseView.handleRemoteKey(keyCode, event)) {
                return true
            }
        }

        // 4. Quick shortcut: Long press on DPAD_CENTER toggles mouse mode
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.isLongPress) {
                toggleMouseMode(!isMouseMode)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            return super.onKeyUp(keyCode, event)
        }

        if (isMouseMode) {
            if (virtualMouseView.handleRemoteKey(keyCode, event)) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Case 1: TV menu is open -> close it
        if (tvMenuOverlay.visibility == View.VISIBLE) {
            toggleMenu(false)
            return
        }

        // Case 2: In Fullscreen Video playback -> exit fullscreen
        if (webChromeClient.isFullscreen) {
            webChromeClient.onHideCustomView()
            return
        }

        // Case 3: Can navigate back in browser history
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }

        // Case 4: Double back press confirmation before exiting app
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
