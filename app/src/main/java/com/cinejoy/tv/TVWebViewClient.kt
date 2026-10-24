package com.cinejoy.tv

import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * Custom WebViewClient that intercepts ads, redirects, and injects
 * TV-specific responsive CSS & JavaScript enhancements.
 */
class TVWebViewClient(
    private val onPageLoaded: (String?) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrlNavigation(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrlNavigation(view, url)
    }

    private fun handleUrlNavigation(view: WebView?, url: String): Boolean {
        // If it's an ad or unwanted redirect, block navigation completely
        if (AdBlocker.shouldBlockUrl(url)) {
            return true
        }

        // Allow legitimate website navigation to load inside the WebView
        view?.loadUrl(url)
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        // Block network loading of known ad domains/resources
        if (AdBlocker.shouldBlockUrl(url)) {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream("".toByteArray())
            )
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        // Inject TV custom CSS and anti-popup scripts
        view?.evaluateJavascript(AdBlocker.TV_INJECTED_CSS_JS, null)

        onPageLoaded(url)
    }
}
