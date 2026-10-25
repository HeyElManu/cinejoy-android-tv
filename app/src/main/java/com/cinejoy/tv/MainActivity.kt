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
        ) { /* fullscreen callback */ }
        webView.webChromeClient = webChromeClient

        webView.webViewClient = TVWebViewClient {
            webView.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)
        }

        // Le WebView ne doit JAMAIS prendre le focus clavier — le curseur s'en charge
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // dispatchKeyEvent est appelé EN PREMIER, avant onKeyDown et avant que
    // le système Android TV ou la WebView ne reçoivent quoi que ce soit.
    // On intercepte ici TOUTES les touches et on ne laisse passer que Back.
    // ─────────────────────────────────────────────────────────────────────────
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {

            // ⬆️⬇️⬅️➡️ → déplace le curseur, jamais le système
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            // ✅ OK / Sélection → clic souris
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                virtualMouseView.handleRemoteKey(event.keyCode, event)
                true  // consommé : le système et la WebView ne le voient jamais
            }

            // ⏮ Retour → navigation dans l'historique / quitter
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_UP) handleBack()
                true
            }

            // Toutes les autres touches (volume, chaîne, menu…) → ignorées
            else -> true
        }
    }

    // On ne garde plus onKeyDown / onKeyUp : dispatchKeyEvent suffit
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

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause()  { super.onPause();  webView.onPause()  }
    override fun onDestroy(){ webView.destroy(); super.onDestroy() }
}
