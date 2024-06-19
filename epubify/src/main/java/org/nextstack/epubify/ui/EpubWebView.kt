package org.nextstack.epubify.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import org.nextstack.epubify.R
import org.nextstack.epubify.parser.EpubWhitelist.ANCHOR_SCROLL_SCHEME
import org.nextstack.epubify.utils.ViewerGestureListener
import org.nextstack.epubify.utils.readRawResource
import kotlin.math.roundToInt

/**
 * A [WebView] that displays an epub file.
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
     * onLoading callback for the WebView that is called when the WebView is loading.
     */
    internal var onLoading: ((Boolean) -> Unit)? = null

    /**
     * The current page number.
     */
    private var currentPage = 0

    /**
     * The current zoom level.
     */
    private var currentZoomLevel = EpubViewerState.defaultZoomLevel

    /**
     * The total number of pages in the epub file.
     */
    private var totalPages = 0

    /**
     * A WebViewBridge for the WebView.
     */
    private val bridge = WebViewBridge()

    /**
     * A flag to check if the epub file is loaded.
     */
    private var loaded = false

    /**
     * An internal padding for the WebView.
     */
    private var internalPadding: PaddingValues = PaddingValues(0.dp)

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
        webChromeClient = EpubWebChromeClient()
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
     * @param page The page number.
     * @param animated Whether to animate the scrolling.
     */
    internal fun loadPage(page: Int, animated: Boolean = true) {
        bridge.loadPage(page, animated)
    }

    /**
     * Sets the zoom level for the WebView.
     */
    internal fun setZoomLevel(zoomLevel: Int) {
        if (currentZoomLevel != zoomLevel) {
            currentZoomLevel = zoomLevel
            bridge.setZoom(zoomLevel)
        }
    }

    /**
     * Sets the internal vertical padding for the WebView.
     */
    internal fun setVerticalPadding(paddingValues: VerticalPaddingValues) {
        this.internalPadding = paddingValues
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
         * onPageStarted callback for the WebView that is called when the page starts loading.
         */
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view.loadUrl("javascript:window.WebViewBridge.processPreInitScript();")
        }

        /**
         * onPageFinished callback for the WebView that is called when the page is finished loading.
         */
        override fun onPageFinished(view: WebView, url: String?) {
            view.loadUrl("javascript:window.WebViewBridge.processPages();")
            if (totalPages == 0 && !loaded) {
                onLoading?.invoke(true)
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
     * A WebChromeClient that provides a callback for the WebView loading progress.
     */
    private inner class EpubWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onLoading?.invoke(newProgress < 100)
            loaded = newProgress == 100
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
            initChapterColumns()
        }

        /**
         * Processes the pre-init script in the WebView.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun processPreInitScript() {
            val preInitScript = context.resources.readRawResource(R.raw.pre_init_script)

            postDelayed({
                evaluateJavascript(preInitScript) {
                    // Set internal padding
                    setInternalPadding(internalPadding)
                }
            },100)
        }

        /**
         * Loads the specified page number in the WebView.
         * Calls scrollToPage in the WebView and changes the current page number.
         * @param page The page number.
         * @param animated Whether to animate the scrolling.
         */
        fun loadPage(page: Int, animated: Boolean) {
            post {
                evaluateJavascript("scrollToPage(${page}, ${animated});", null)
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
        fun scrollToPosition(pixelRatio: Double, position: Int, animated: Boolean) {
            post {
                if (animated) {
                    val positionInPixels = (position * pixelRatio).roundToInt()
                    val anim = ObjectAnimator.ofInt(this@EpubWebView, "scrollX", scrollX, positionInPixels)
                    anim.setDuration(500).start()
                } else {
                    val positionInPixels = (position * pixelRatio).roundToInt()
                    scrollTo(positionInPixels, scrollY)
                }
            }
        }

        /**
         * Updates column count in state.
         * @param columnCount The column count.
         */
        @JavascriptInterface
        @Suppress("unused")
        fun setColumnCount(columnCount: Int) {
            totalPages = columnCount
            if (totalPages > 0) {
                onPagesInitialized?.invoke(totalPages)
            }
        }

        /**
         * Sets the zoom level for the WebView.
         */
        fun setZoom(zoom: Int) {
            post {
                evaluateJavascript("setZoom($zoom);", null)
            }
        }

        /**
         * Sets the internal padding for the WebView.
         */
        private fun setInternalPadding(internalPadding: PaddingValues) {
            val top = internalPadding.calculateTopPadding().value
            val bottom = internalPadding.calculateBottomPadding().value

            post {
                evaluateJavascript(
                    "setVerticalPadding(${top},${bottom});",
                    null
                )
            }
        }

        /**
         * Initializes the chapter columns in the WebView.
         */
        private fun initChapterColumns() {
            val chapterColumnScript = context.resources.readRawResource(R.raw.chapter_column_script)

            post {
                evaluateJavascript(chapterColumnScript, null)
            }
        }
    }
}