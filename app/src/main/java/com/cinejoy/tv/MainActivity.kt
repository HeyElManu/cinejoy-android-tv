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
    private lateinit var modeIndicatorPill: TextView
    private lateinit var tvMenuOverlay: View

    private lateinit var webChromeClient: TVWebChromeClient
    private lateinit var webViewClient: TVWebViewClient

    // Enabled by default so arrow keys move the pointer instead of triggering volume/seek
    private var isMouseMode = true
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

        // Show brief initial pill notification then fade out
        showModeIndicator("🖱️ Mode Souris Activé (Appui long OK pour changer)")
    }

    private fun initViews() {
        mainRoot = findViewById(R.id.main_root)
        webView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        virtualMouseView = findViewById(R.id.virtualMouseView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        modeIndicatorPill = findViewById(R.id.modeIndicatorPill)
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
            if (isFullscreen) {
                // In fullscreen, mouse mode remains accessible
                virtualMouseView.visibility = if (isMouseMode) View.VISIBLE else View.GONE
            }
        }
        webView.webChromeClient = webChromeClient

        webViewClient = TVWebViewClient { loadedUrl ->
            // Re-apply TV script once page completes
            webView.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)
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

        btnToggleMouse.text = if (isMouseMode) "🎬 Passer en Mode Lecteur Vidéo" else "🖱️ Activer Mode Souris"

        btnHome.setOnClickListener {
            toggleMenu(false)
            webView.loadUrl(CINEJOY_HOME_URL)
        }

        btnToggleMouse.setOnClickListener {
            toggleMouseMode(!isMouseMode)
            btnToggleMouse.text = if (isMouseMode) "🎬 Passer en Mode Lecteur Vidéo" else "🖱️ Activer Mode Souris"
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
            val btnToggle = tvMenuOverlay.findViewById<Button>(R.id.btnMenuToggleMouse)
            btnToggle.text = if (isMouseMode) "🎬 Passer en Mode Lecteur Vidéo" else "🖱️ Activer Mode Souris"
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
            showModeIndicator("🖱️ Mode Souris (Flèches = Déplacer le curseur)")
        } else {
            virtualMouseView.visibility = View.GONE
            showModeIndicator("🎬 Mode Vidéo (Flèches = Volume & Avance/Retour)")
        }
    }

    private fun showModeIndicator(message: String) {
        modeIndicatorPill.text = message
        modeIndicatorPill.alpha = 1f
        modeIndicatorPill.visibility = View.VISIBLE

        mainHandler.removeCallbacksAndMessages("PILL_ANIM")
        mainHandler.postDelayed({
            modeIndicatorPill.animate()
                .alpha(0f)
                .setDuration(600)
                .withEndAction { modeIndicatorPill.visibility = View.GONE }
                .start()
        }, 3200)
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

        // 3. Appui long sur OK / DPAD_CENTER bascule instantanément entre Souris et Mode Lecteur
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.isLongPress) {
                toggleMouseMode(!isMouseMode)
                return true
            }
        }

        // 4. In Mouse Mode: Arrows move pointer instead of volume/seek
        if (isMouseMode) {
            if (virtualMouseView.handleRemoteKey(keyCode, event)) {
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
