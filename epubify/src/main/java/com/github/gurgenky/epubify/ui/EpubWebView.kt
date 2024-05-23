package com.github.gurgenky.epubify.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.gurgenky.epubify.utils.SwipeGestureListener
import com.github.gurgenky.epubify.utils.toDp
import com.github.gurgenky.epubify.utils.toPx


/**
 * A WebView that displays an epub file.
 */
class EpubWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var currentPage = 0
    private var totalPages = 0

    private var touchDownY = 0f

    private val webViewResizer = WebViewResizer()

    private val gestureDetector = GestureDetector(context, SwipeGestureListener(
        onSwipeLeft = {
            val page = currentPage - 1
            if (page >= 0) {
                loadPage(page)
            }
        },
        onSwipeRight = {
            val page = currentPage + 1
            if (page <= totalPages) {
                loadPage(page)
            }
        }
    ))

    init {
        settings.javaScriptEnabled = true
        webViewClient = EpubWebViewClient()
        addJavascriptInterface(webViewResizer, "WebViewResizer")
        initTouchListeners()
    }

    /**
     * Initializes the touch listeners for the WebView.
     */
    private fun initTouchListeners() {
        setOnTouchListener { _, motionEvent ->
            // multitouch is ignored
            if (motionEvent.pointerCount > 1) {
                return@setOnTouchListener true
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> touchDownY = motionEvent.y
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->
                    motionEvent.setLocation(
                        motionEvent.x,
                        touchDownY
                    )
            }

            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    /**
     * Loads the page at the given index.
     */
    private fun loadPage(page: Int) {
        webViewResizer.loadPage(page)
    }

    /**
     * A WebViewClient that calculates the total number of pages in the epub file.
     */
    private inner class EpubWebViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String?) {
            view.loadUrl("javascript:(function(){ document.body.style.margin=\"6%\"})();")
            view.loadUrl("javascript:window.WebViewResizer.processHeight(document.querySelector('body').offsetHeight);")
        }
    }

    /**
     * A JavaScript interface that resizes the WebView.
     */
    private inner class WebViewResizer {

        /**
         * Processes the height of the WebView.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun processHeight(height: String?) {
            val contentHeight = context.toPx(height?.toInt() ?: 0)
            val viewHeight = this@EpubWebView.height.toFloat()

            totalPages = if (viewHeight == 0f) 1 else (contentHeight / viewHeight).toInt()
            modifyWebViewHeight(contentHeight)
            loadPage(0)
        }

        /**
         *  Evaluates the JavaScript to scroll to the given page.
         */
        fun loadPage(page: Int) {
            val position = context.toDp(page * height)

            post {
                evaluateJavascript(
                    "(function() { window.scrollTo(0, $position); })();",
                    null
                )
                currentPage = page
            }
        }

        private fun modifyWebViewHeight(
            contentHeight: Float
        ) {
            val bottomMargin = height * (totalPages + 1) - contentHeight.toInt()

            post {
                loadUrl("javascript:(function(){ document.body.style.marginBottom=\"${bottomMargin}px\"})();")
            }
        }
    }
}