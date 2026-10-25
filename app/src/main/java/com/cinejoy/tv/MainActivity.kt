package com.cinejoy.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
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

    private lateinit var webChromeClient: TVWebChromeClient

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

        mainRoot = findViewById(R.id.main_root)
        webView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        virtualMouseView = findViewById(R.id.virtualMouseView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        virtualMouseView.attachTargetView(webView)

        setupWebView()
        webView.loadUrl(CINEJOY_HOME_URL)
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
        ) { /* fullscreen change – nothing extra needed */ }
        webView.webChromeClient = webChromeClient

        webView.webViewClient = TVWebViewClient {
            webView.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)
        }
    }

    // ─── Seules 5 touches sont gérées ───────────────────────────────────────
    // ⬆️ ⬇️ ⬅️ ➡️ → déplace le curseur
    // ✅ OK  → clic à l'emplacement du curseur
    // Tout le reste (volume, retour, menu…) est ignoré.
    // ────────────────────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                virtualMouseView.handleRemoteKey(keyCode, event)
                true // consommé : ne passe JAMAIS au lecteur vidéo
            }
            KeyEvent.KEYCODE_BACK -> {
                handleBack()
                true
            }
            else -> true // toutes les autres touches sont silencieusement ignorées
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                virtualMouseView.handleRemoteKey(keyCode, event)
                true
            }
            else -> true
        }
    }

    private fun handleBack() {
        if (webChromeClient.isFullscreen) {
            webChromeClient.onHideCustomView()
            return
        }
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        if (backPressedOnce) {
            finish()
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
