package com.github.gurgenky.epubify.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.gurgenky.epubify.R
import com.github.gurgenky.epubify.utils.ViewerGestureListener
import com.github.gurgenky.epubify.utils.readRawResource


/**
 * A WebView that displays an epub file.
 */
@SuppressLint("SetJavaScriptEnabled")
internal class EpubWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    /**
     * OnInit callback for the WebView that returns the total number of pages in the epub file.
     */
    internal var onInit: ((totalPages: Int) -> Unit)? = null

    /**
     * OnPageChanged callback for the WebView.
     */
    internal var onPageChanged: ((currentPage: Int, totalPages: Int) -> Unit)? = null

    /**
     * OnSingleTap callback for the WebView.
     */
    internal var onTap: (() -> Unit)? = null

    /**
     * OnError callback for the WebView that is called when an error occurs.
     */
    internal var onError: (() -> Unit)? = null

    /**
     * The current page number.
     */
    private var currentPage = 0

    /**
     * The total number of pages in the epub file.
     */
    private var totalPages = 0

    /**
     * A WebViewResizer instance for resizing the WebView.
     */
    private val webViewResizer = WebViewResizer()

    /**
     * A gesture detector for swipe gestures.
     */
    private val gestureDetector = GestureDetector(context, ViewerGestureListener(
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
        },
        onTap = {
            onTap?.invoke()
        }
    ))


    /**
     * Initializes the WebView settings and listeners.
     */
    init {
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        webViewClient = EpubWebViewClient()
        addJavascriptInterface(webViewResizer, "WebViewResizer")
        initTouchListeners()
    }

    /**
     * Loads the specified epub HTML content in the WebView.
     */
    internal fun loadEpubHtml(
        html: String
    ) {
        loadDataWithBaseURL(
            null,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    /**
     * Initializes the touch listeners for the WebView.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListeners() {
        setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    /**
     * Loads the specified epub page in the WebView.
     */
    private fun loadPage(page: Int) {
        webViewResizer.loadPage(page)
    }

    /**
     * A WebViewClient that calculates the total number of pages in the epub file.
     */
    private inner class EpubWebViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String?) {
            view.loadUrl("javascript:window.WebViewResizer.processPages();")
            if (totalPages > 0) {
                onInit?.invoke(totalPages)
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (error != null) {
                onError?.invoke()
            }
        }
    }

    /**
     * A JavaScript interface that resizes the WebView.
     */
    private inner class WebViewResizer {

        /**
         * Processes the pages in the WebView.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun processPages() {
            initChapterColumns { pages ->
                totalPages = pages
            }
        }

        /**
         * Loads the specified page in the WebView.
         */
        fun loadPage(page: Int) {
            post {
                evaluateJavascript("javascript:window.scrollTo((window.innerWidth / 4) * $page, 0);") {
                    currentPage = page
                    onPageChanged?.invoke(page, totalPages)
                }
            }
        }

        /**
         * Initializes the chapter columns in the WebView.
         */
        private fun initChapterColumns(onComplete: ((pages: Int) -> Unit)? = null) {
            val chapterPaddingScript = context.resources.readRawResource(R.raw.chapter_column_script)

            post {
                evaluateJavascript(chapterPaddingScript) {
                    onComplete?.invoke(it.toIntOrNull() ?: 1)
                }
            }
        }
    }
}