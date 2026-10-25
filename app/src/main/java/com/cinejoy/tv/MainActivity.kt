package com.cinejoy.tv

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mainRoot: FrameLayout
    private lateinit var webView: android.webkit.WebView
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

        mainRoot       = findViewById(R.id.main_root)
        webView        = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)
        virtualMouseView    = findViewById(R.id.virtualMouseView)
        loadingProgressBar  = findViewById(R.id.loadingProgressBar)

        virtualMouseView.attachTargetView(webView)
        volumeControlStream = AudioManager.STREAM_MUSIC

        setupWebView()
        webView.loadUrl(CINEJOY_HOME_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled   = true
        s.cacheMode         = WebSettings.LOAD_DEFAULT
        s.userAgentString   = DESKTOP_TV_USER_AGENT
        s.useWideViewPort   = true
        s.loadWithOverviewMode  = true
        s.setSupportZoom(false)
        s.builtInZoomControls   = false
        s.displayZoomControls   = false
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webChromeClient = TVWebChromeClient(
            mainContainer       = mainRoot,
            customViewContainer = customViewContainer,
            webView             = webView,
            progressBar         = loadingProgressBar
        ) { /* fullscreen callback */ }
        webView.webChromeClient = webChromeClient
        webView.webViewClient   = TVWebViewClient {
            webView.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)
        }

        // Le WebView ne doit JAMAIS recevoir le focus clavier
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // dispatchKeyEvent = PREMIER point d'entrée de toute touche.
    // On consomme TOUT ici — rien ne passe jamais à la WebView ni au système.
    // ─────────────────────────────────────────────────────────────────────────
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val a = event.action

        return when (event.keyCode) {

            // ⬆ ou Volume+ → curseur HAUT
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (a == KeyEvent.ACTION_DOWN || a == KeyEvent.ACTION_UP)
                    virtualMouseView.handleRemoteKey(KeyEvent.KEYCODE_DPAD_UP, event)
                true
            }

            // ⬇ ou Volume- → curseur BAS
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (a == KeyEvent.ACTION_DOWN || a == KeyEvent.ACTION_UP)
                    virtualMouseView.handleRemoteKey(KeyEvent.KEYCODE_DPAD_DOWN, event)
                true
            }

            // ⬅ → curseur GAUCHE (et pas avance rapide !)
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                if (a == KeyEvent.ACTION_DOWN || a == KeyEvent.ACTION_UP)
                    virtualMouseView.handleRemoteKey(KeyEvent.KEYCODE_DPAD_LEFT, event)
                true
            }

            // ➡ → curseur DROITE (et pas retour arrière !)
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                if (a == KeyEvent.ACTION_DOWN || a == KeyEvent.ACTION_UP)
                    virtualMouseView.handleRemoteKey(KeyEvent.KEYCODE_DPAD_RIGHT, event)
                true
            }

            // ✅ OK / Sélection → clic souris
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (a == KeyEvent.ACTION_DOWN || a == KeyEvent.ACTION_UP)
                    virtualMouseView.handleRemoteKey(KeyEvent.KEYCODE_DPAD_CENTER, event)
                true
            }

            // ⏮ Retour → historique / quitter (seule touche "normale" qui passe)
            KeyEvent.KEYCODE_BACK -> {
                if (a == KeyEvent.ACTION_UP) handleBack()
                true
            }

            // TOUT LE RESTE → silencieusement ignoré (chaînes, play/pause, info…)
            else -> true
        }
    }

    private fun handleBack() {
        if (webChromeClient.isFullscreen) { webChromeClient.onHideCustomView(); return }
        if (webView.canGoBack())          { webView.goBack(); return }
        if (backPressedOnce)              { finish(); return }
        backPressedOnce = true
        Toast.makeText(this, getString(R.string.toast_exit), Toast.LENGTH_SHORT).show()
        mainHandler.postDelayed({ backPressedOnce = false }, 2500)
    }

    override fun onResume()  { super.onResume();  webView.onResume() }
    override fun onPause()   { super.onPause();   webView.onPause()  }
    override fun onDestroy() { webView.destroy();  super.onDestroy() }
}
