package com.github.gurgenky.epubify.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.gurgenky.epubify.model.Book
import com.github.gurgenky.epubify.parser.EpubParser
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

// TODO: Implement EpubViewer composable and add documentation
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
    epubInputStream: FileInputStream,
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
    Column {
        Text(text = book?.title ?: "Loading...", modifier = modifier)
    }
}

@Preview
@Composable
private fun EpubViewerPreview() {
    ViewerContent(null)
}