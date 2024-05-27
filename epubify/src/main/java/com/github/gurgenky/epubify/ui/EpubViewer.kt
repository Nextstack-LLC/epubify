package com.github.gurgenky.epubify.ui

import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.github.gurgenky.epubify.model.Book
import com.github.gurgenky.epubify.parser.EpubParser
import com.github.gurgenky.epubify.utils.asHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * EpubViewer composable function
 *
 * @param epubPath Path to the epub file
 * @param modifier Modifier
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 */
@Composable
fun EpubViewer(
    epubPath: String,
    modifier: Modifier = Modifier,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        modifier = modifier,
        book = book,
        loading = loading,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        error = error,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(epubPath) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(epubPath)
            } catch (e: Exception) {
                parseError = true
                null
            }
        }
    }
}

/**
 * EpubViewer composable function
 *
 * @param epub File of the epub
 * @param modifier Modifier
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 */
@Composable
fun EpubViewer(
    epub: File,
    modifier: Modifier = Modifier,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        modifier = modifier,
        book = book,
        loading = loading,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        error = error,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(epub) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(epub)
            } catch (e: Exception) {
                parseError = true
                null
            }
        }
    }
}


/**
 * EpubViewer composable function
 *
 * @param epubInputStream InputStream of the epub file
 * @param modifier Modifier
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 *
 * Requires API level 26
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpubViewer(
    epubInputStream: InputStream,
    modifier: Modifier = Modifier,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        modifier = modifier,
        book = book,
        loading = loading,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        error = error,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(epubInputStream) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(epubInputStream)
            } catch (e: Exception) {
                parseError = true
                null
            }
        }
    }
}


/**
 * Shared content for all EpubViewer composable functions
 */
@Composable
private fun ViewerContent(
    book: Book?,
    modifier: Modifier = Modifier,
    loading: @Composable BoxScope.() -> Unit,
    error: @Composable BoxScope.() -> Unit,
    onInitialized: ((Int) -> Unit)?,
    onBookPageChanged: ((Int, Int) -> Unit)?,
    onSingleTap: (() -> Unit)?,
    parseError: Boolean
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    var isLoaded by remember {
        mutableStateOf(false)
    }
    var isError by remember {
        mutableStateOf(false)
    }

    val transparencyModifier = if (isLoaded && !isError) {
        Modifier.alpha(1f)
    } else {
        Modifier.alpha(0f)
    }

    var htmlContent by remember {
        mutableStateOf("")
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
                    .then(transparencyModifier)
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
                modifier = Modifier
                    .then(transparencyModifier),
                factory = { _ ->
                    val view = EpubWebView(context).apply {
                        // Set callbacks
                        onInit = {
                            isLoaded = true
                            onInitialized?.invoke(it)
                        }
                        onPageChanged = onBookPageChanged
                        onTap = onSingleTap
                        onError = {
                            isError = true
                        }

                        // Set layout params
                        isScrollContainer = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    view
                },
                update = { webView ->
                    webView.loadEpubHtml(htmlContent)
                }
            )
        }

        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                loading()
            }
        }

        if (isError) {
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

    LaunchedEffect(book) {
        withContext(Dispatchers.IO) {
            htmlContent = book?.asHtml(context) ?: ""
        }
    }
}

@Preview
@Composable
private fun EpubViewerPreview() {
    EpubViewer(epubPath = "path/to/epub")
}