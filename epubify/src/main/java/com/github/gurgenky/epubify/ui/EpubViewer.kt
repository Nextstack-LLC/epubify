package com.github.gurgenky.epubify.ui

import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.github.gurgenky.epubify.model.Book
import com.github.gurgenky.epubify.parser.EpubParser
import com.github.gurgenky.epubify.utils.asHtml
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

@Composable
fun EpubViewer(
    epubPath: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    ViewerContent(
        modifier = modifier,
        book = book
    )

    LaunchedEffect(epubPath) {
        coroutineScope.launch {
            book = EpubParser.parse(epubPath)
        }
    }
}

@Composable
fun EpubViewer(
    epub: File,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    ViewerContent(
        modifier = modifier,
        book = book
    )

    LaunchedEffect(epub) {
        coroutineScope.launch { 
            book = EpubParser.parse(epub)
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpubViewer(
    epubInputStream: InputStream,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var book by remember {
        mutableStateOf<Book?>(null)
    }

    ViewerContent(
        modifier = modifier,
        book = book
    )

    LaunchedEffect(epubInputStream) {
        coroutineScope.launch {
            book = EpubParser.parse(epubInputStream)
        }
    }
}

@Composable
private fun ViewerContent(
    book: Book?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var htmlContent by remember {
        mutableStateOf("")
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { _ ->
            val view = EpubWebView(context)
            view.isScrollContainer = false
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            view
        },
        update = { webView ->
            webView.loadEpubHtml(htmlContent)
        }
    )

    LaunchedEffect(book) {
        htmlContent = book?.asHtml(context) ?: ""
    }
}

@Preview
@Composable
private fun EpubViewerPreview() {
    ViewerContent(null)
}