package com.cinejoy.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private var chromeCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenView: View? = null

    companion object {
        const val URL = "https://cinejoy.pk"
        const val UA  = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView              = findViewById(R.id.webView)
        customViewContainer  = findViewById(R.id.customViewContainer)
        progressBar          = findViewById(R.id.loadingProgressBar)

        // ── Paramètres WebView ─────────────────────────────────────────────
        webView.settings.apply {
            javaScriptEnabled              = true
            domStorageEnabled              = true
            databaseEnabled                = true
            userAgentString                = UA
            useWideViewPort                = true
            loadWithOverviewMode           = true
            setSupportZoom(true)
            builtInZoomControls            = true
            displayZoomControls            = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode               = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode                      = WebSettings.LOAD_DEFAULT
        }

        // ── WebViewClient : navigation dans la même WebView ────────────────
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }

        // ── WebChromeClient : barre de progression + plein écran vidéo ─────
        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                fullscreenView  = view
                chromeCallback  = callback
                webView.visibility              = View.GONE
                customViewContainer.visibility  = View.VISIBLE
                customViewContainer.addView(view)
            }

            override fun onHideCustomView() {
                customViewContainer.removeView(fullscreenView)
                fullscreenView = null
                customViewContainer.visibility = View.GONE
                webView.visibility             = View.VISIBLE
                chromeCallback?.onCustomViewHidden()
                chromeCallback = null
            }

            override fun onCreateWindow(
                view: WebView, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: android.os.Message?
            ) = false   // bloque les pop-ups
        }

        webView.loadUrl(URL)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            fullscreenView != null        -> webView.webChromeClient
                ?.also { (it as? WebChromeClient)?.onHideCustomView() }
            webView.canGoBack()           -> webView.goBack()
            else                          -> super.onBackPressed()
        }
    }

    override fun onResume()  { super.onResume();  webView.onResume() }
    override fun onPause()   { super.onPause();   webView.onPause()  }
    override fun onDestroy() { webView.destroy();  super.onDestroy() }
}
