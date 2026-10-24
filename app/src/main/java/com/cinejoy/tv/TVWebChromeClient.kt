package com.cinejoy.tv

import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar

/**
 * Custom WebChromeClient tailored for Android TV:
 * - Handles HTML5 fullscreen video playback seamlessly.
 * - Suppresses rogue window.open popup tabs.
 * - Updates the loading bar.
 */
class TVWebChromeClient(
    private val mainContainer: FrameLayout,
    private val customViewContainer: FrameLayout,
    private val webView: WebView,
    private val progressBar: ProgressBar,
    private val onFullscreenChanged: (Boolean) -> Unit
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    val isFullscreen: Boolean
        get() = customView != null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress in 1..99) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = newProgress
        } else {
            progressBar.visibility = View.GONE
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }

        customView = view
        customViewCallback = callback

        // Hide regular WebView and show fullscreen video container
        webView.visibility = View.GONE
        customViewContainer.visibility = View.VISIBLE
        customViewContainer.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        onFullscreenChanged(true)
    }

    override fun onHideCustomView() {
        if (customView == null) return

        // Hide fullscreen container and restore WebView
        customViewContainer.removeView(customView)
        customView = null
        customViewContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        onFullscreenChanged(false)
    }

    /**
     * Prevents creation of secondary popup windows often triggered by streaming ads.
     */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        // Return false to block unwanted pop-up windows
        return false
    }
}
