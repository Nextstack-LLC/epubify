package org.nextstack.epubify.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nextstack.epubify.model.Book
import org.nextstack.epubify.model.ParseOptions
import org.nextstack.epubify.parser.EpubParser
import java.io.File
import java.io.InputStream

/**
 * EpubViewer composable function
 *
 * @param epubPath Path to the epub file
 * @param modifier Modifier
 * @param state EpubViewerState for controlling the viewer
 * @param parseOptions Options for parsing the epub file
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 *
 * Example usage:
 *
 * ```
 * EpubViewer(
 *        state = state,
 *        modifier = Modifier,
 *        epubPath = "path/to/epub",
 *        parseOptions = ParseOptions(
 *            parseEpubFonts = true,
 *            customStyles = listOf(
 *                Style(
 *                    css = "a { color: red; }"
 *                )
 *            )
 *        ),
 *        loading = {
 *            // Loading
 *        },
 *        error = {
 *            // Error
 *        }
 *    )
 * ```
 */
@Composable
fun EpubViewer(
    epubPath: String,
    modifier: Modifier = Modifier,
    state: EpubViewerState = rememberEpubViewerState(),
    parseOptions: ParseOptions = ParseOptions(),
    innerPaddingValues: PaddingValues = PaddingValues(0.dp),
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val path by rememberUpdatedState(newValue = epubPath)

    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        book = book,
        state = state,
        modifier = modifier,
        innerPaddingValues = innerPaddingValues,
        loading = loading,
        error = error,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(path) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(path, parseOptions)
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
 * @param state EpubViewerState for controlling the viewer
 * @param parseOptions Options for parsing the epub file
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 *
 * Example usage :
 *
 * ```
 * EpubViewer(
 *        state = state,
 *        modifier = Modifier,
 *        epub = epub,
 *        parseOptions = ParseOptions(
 *            parseEpubFonts = true,
 *            customStyles = listOf(
 *                Style(
 *                    css = "a { color: red; }"
 *                )
 *            )
 *        ),
 *        loading = {
 *            // Loading
 *        },
 *        error = {
 *            // Error
 *        }
 *    )
 * ```
 */
@Composable
fun EpubViewer(
    epub: File,
    modifier: Modifier = Modifier,
    state: EpubViewerState = rememberEpubViewerState(),
    parseOptions: ParseOptions = ParseOptions(),
    innerPaddingValues: PaddingValues = PaddingValues(0.dp),
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val file by rememberUpdatedState(newValue = epub)

    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        book = book,
        state = state,
        modifier = modifier,
        innerPaddingValues = innerPaddingValues,
        loading = loading,
        error = error,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(file) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(file, parseOptions)
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
 * @param state EpubViewerState for controlling the viewer
 * @param parseOptions Options for parsing the epub file
 * @param loading Composable function for loading state
 * @param error Composable function for error state
 * @param onInitialized Callback for when the epub file is initialized
 * @param onBookPageChanged Callback for when the book page is changed
 * @param onSingleTap Callback for when the user single taps
 *
 * Example usage :
 *
 * ```
 * EpubViewer(
 *        state = state,
 *        modifier = Modifier,
 *        epubInputStream = epub,
 *        parseOptions = ParseOptions(
 *            parseEpubFonts = true,
 *            customStyles = listOf(
 *                Style(
 *                    css = "a { color: red; }"
 *                )
 *            )
 *        ),
 *        loading = {
 *            // Loading
 *        },
 *        error = {
 *            // Error
 *        }
 *    )
 * ```
 *
 * Requires API level 26
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpubViewer(
    epubInputStream: InputStream,
    modifier: Modifier = Modifier,
    state: EpubViewerState = rememberEpubViewerState(),
    parseOptions: ParseOptions = ParseOptions(),
    innerPaddingValues: PaddingValues = PaddingValues(0.dp),
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
    onInitialized: ((Int) -> Unit)? = null,
    onBookPageChanged: ((Int, Int) -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val stream by rememberUpdatedState(newValue = epubInputStream)

    var book by remember {
        mutableStateOf<Book?>(null)
    }

    var parseError by remember {
        mutableStateOf(false)
    }

    ViewerContent(
        book = book,
        state = state,
        modifier = modifier,
        innerPaddingValues = innerPaddingValues,
        loading = loading,
        error = error,
        onInitialized = onInitialized,
        onBookPageChanged = onBookPageChanged,
        onSingleTap = onSingleTap,
        parseError = parseError
    )

    LaunchedEffect(stream) {
        coroutineScope.launch {
            book = try {
                EpubParser.parse(stream, parseOptions)
            } catch (e: Exception) {
                parseError = true
                null
            }
        }
    }
}