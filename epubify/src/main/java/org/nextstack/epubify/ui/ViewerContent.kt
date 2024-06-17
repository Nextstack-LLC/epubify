package org.nextstack.epubify.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.nextstack.epubify.model.Book
import org.nextstack.epubify.utils.asHtml
import kotlin.math.roundToInt

/**
 * Shared content for all EpubViewer composable functions
 *
 * Creates an [EpubWebView] with the given [book] and [state]
 *
 * @see EpubViewer
 * @see EpubWebView
 */
@Composable
internal fun ViewerContent(
    book: Book?,
    state: EpubViewerState,
    modifier: Modifier = Modifier,
    innerPaddingValues: PaddingValues,
    loading: @Composable() (BoxScope.() -> Unit),
    error: @Composable() (BoxScope.() -> Unit),
    onInitialized: ((Int) -> Unit)?,
    onBookPageChanged: ((Int, Int) -> Unit)?,
    onSingleTap: (() -> Unit)?,
    parseError: Boolean
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val epubViewer = remember {
        EpubWebView(context)
    }

    var htmlContent by remember {
        mutableStateOf("")
    }

    var isLoaded by remember {
        mutableStateOf(false)
    }

    var isError by remember {
        mutableStateOf(false)
    }

    var latestPage by rememberSaveable(key = book?.title) {
        mutableIntStateOf(0)
    }

    var totalPages by rememberSaveable {
        mutableIntStateOf(0)
    }

    var zoomLevelChanged by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EpubViewer"
                )
            }
        } else {
            AndroidView(
                modifier = Modifier,
                factory = { _ ->
                    epubViewer.apply {
                        // Set callbacks
                        onLoading = {
                            isLoaded = it.not()
                        }
                        onPagesInitialized = {
                            state.setTotalPages(it)
                            onInitialized?.invoke(it)
                        }
                        onPageChanged = { page, totalPages ->
                            onBookPageChanged?.invoke(page, totalPages)
                            state.setCurrentPage(page)
                        }
                        onTap = onSingleTap
                        onError = {
                            isError = true
                        }

                        setInternalPadding(innerPaddingValues)
                        // Set layout params
                        isScrollContainer = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    epubViewer
                },
                update = { webView ->
                    webView.loadEpubHtml(htmlContent)
                }
            )
        }

        if (book == null || !isLoaded) {
            Surface {
                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    loading()
                }
            }
        }

        if (isError) {
            Surface {
                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    error()
                }
            }
        }
    }

    // Jump to page
    LaunchedEffect(state.pendingJumpPageIndex) {
        if (state.pendingJumpPageIndex != -1) {
            epubViewer.loadPage(state.pendingJumpPageIndex)
            state.clearPendingJump()
        }
    }

    LaunchedEffect(isPreview) {
        if (isPreview) {
            delay(3000) // Simulate loading
            isLoaded = true
        }
    }

    LaunchedEffect(parseError) {
        if (parseError) {
            isError = true
            isLoaded = true
        }
    }

    // Load html content
    LaunchedEffect(book) {
        withContext(Dispatchers.IO) {
            htmlContent = book?.asHtml(context) ?: ""
        }
    }


    // Set zoom level
    LaunchedEffect(state.zoomLevel) {
        zoomLevelChanged = true
        latestPage = state.currentPageIndex
        epubViewer.setZoomLevel(state.zoomLevel)
    }

    // Restore latest page from state
    LaunchedEffect(state.totalPages) {
        if (totalPages != 0 && state.totalPages != totalPages) {
            val currentProgress = latestPage / totalPages.toFloat()
            val page = (currentProgress * state.totalPages).roundToInt()
            epubViewer.loadPage(page, !zoomLevelChanged)
        }
        if (state.totalPages != 0) {
            totalPages = state.totalPages
            zoomLevelChanged = false
        }
    }

    // Save latest page in state
    DisposableEffect(Unit) {
        onDispose {
            latestPage = state.currentPageIndex
        }
    }
}

@Preview
@Composable
private fun EpubViewerPreview() {
    EpubViewer(epubPath = "path/to/epub")
}