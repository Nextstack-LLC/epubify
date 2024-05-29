package com.github.gurgenky.epubify.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.gurgenky.epubify.R
import com.github.gurgenky.epubify.parser.EpubWhitelist.ANCHOR_SCROLL_SCHEME
import com.github.gurgenky.epubify.utils.ViewerGestureListener
import com.github.gurgenky.epubify.utils.readRawResource
import kotlin.math.roundToInt

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
     * onPagesInitialized callback for the WebView that is called when the pages are initialized.
     */
    internal var onPagesInitialized: ((totalPages: Int) -> Unit)? = null

    /**
     * onPageChanged callback for the WebView.
     */
    internal var onPageChanged: ((currentPage: Int, totalPages: Int) -> Unit)? = null

    /**
     * onSingleTap callback for the WebView.
     */
    internal var onTap: (() -> Unit)? = null

    /**
     * onError callback for the WebView that is called when an error occurs.
     */
    internal var onError: (() -> Unit)? = null

    /**
     * onLoadingFinished callback for the WebView that is called when the loading is finished.
     */
    internal var onLoadingFinished: (() -> Unit)? = null

    /**
     * The current page number.
     */
    private var currentPage = 0

    /**
     * The total number of pages in the epub file.
     */
    private var totalPages = 0

    /**
     * A WebViewBridge for the WebView.
     */
    private val bridge = WebViewBridge()

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
            if (page < totalPages) {
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
        addJavascriptInterface(bridge, "WebViewBridge")
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
     * Loads the specified epub page in the WebView.
     */
    internal fun loadPage(page: Int) {
        bridge.loadPage(page)
    }

    /**
     * Initializes the touch listeners for the WebView.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListeners() {
        setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }
        setOnLongClickListener { true }
        isLongClickable = false
        isHapticFeedbackEnabled = false
    }

    /**
     * A WebViewClient that calculates the total number of pages in the epub file.
     */
    private inner class EpubWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            if (request.url.scheme == ANCHOR_SCROLL_SCHEME) {
                val id = request.url.host
                println(id)
                evaluateJavascript("scrollToElement('${id}');", null)
                return true
            }

            val requestIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(request.url.toString())
            )
            val chooser = Intent.createChooser(requestIntent, title)
            context.startActivity(chooser)
            return true
        }

        /**
         * onPageFinished callback for the WebView that is called when the page is finished loading.
         */
        override fun onPageFinished(view: WebView, url: String?) {
            view.loadUrl("javascript:window.WebViewBridge.processPages();")
            if (totalPages > 0) {
                onLoadingFinished?.invoke()
            }
        }

        /**
         * onReceivedError callback for the WebView that is called when an error occurs.
         */
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
     * A JavaScript interface that provides methods for the WebView.
     */
    private inner class WebViewBridge {

        /**
         * Processes the pages in the WebView.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun processPages() {
            initChapterColumns { pages ->
                totalPages = pages
                if (pages > 0) {
                    onPagesInitialized?.invoke(pages)
                    loadPage(0)
                }
            }
        }

        /**
         * Loads the specified page number in the WebView.
         * Calls scrollToPage in the WebView and changes the current page number.
         * @param page The page number.
         */
        fun loadPage(page: Int) {
            post {
                evaluateJavascript("scrollToPage(${page});", null)
                setCurrentPage(page)
            }
        }

        /**
         * Sets the current page number and calls the onPageChanged callback.
         * @param page The current page number.
         */
        @JavascriptInterface
        fun setCurrentPage(page: Int) {
            currentPage = page
            onPageChanged?.invoke(page, totalPages)
        }

        /**
         * Scrolls to the specified position in the WebView.
         * @param pixelRatio The pixel ratio passed from the WebView.
         * @param position The position to scroll to.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun animateScrollToPosition(pixelRatio: Double, position: Int) {
            val positionInPixels = (position * pixelRatio).roundToInt()
            val anim = ObjectAnimator.ofInt(this@EpubWebView, "scrollX", scrollX, positionInPixels)
            anim.setDuration(500).start()
        }

        /**
         * Initializes the chapter columns in the WebView.
         * @param onComplete The callback to call when the chapter columns are initialized.
         */
        private fun initChapterColumns(onComplete: ((pages: Int) -> Unit)? = null) {
            val chapterPaddingScript = context.resources.readRawResource(R.raw.chapter_column_script)

            post {
                evaluateJavascript(chapterPaddingScript) {
                    onComplete?.invoke(it.toIntOrNull() ?: 0)
                }
            }
        }
    }
}