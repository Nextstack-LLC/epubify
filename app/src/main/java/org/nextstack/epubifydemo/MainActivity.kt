package org.nextstack.epubifydemo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.nextstack.epubify.model.ParseOptions
import org.nextstack.epubify.model.style.CustomFont
import org.nextstack.epubify.model.style.Style
import org.nextstack.epubify.ui.EpubViewer
import org.nextstack.epubify.ui.rememberEpubViewerState
import org.nextstack.epubifydemo.ui.theme.EpubifyDemoTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EpubifyDemoTheme {
                Content()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Content() {
    val resources = LocalContext.current.resources
    val epub = resources.openRawResource(R.raw.epub4)
    val state = rememberEpubViewerState()

    Scaffold(
        bottomBar = {
            Column {
                Button(
                    onClick = {
                        val zoomLevel = state.zoomLevel - 1
                        state.setZoomLevel(zoomLevel)
                    }
                ) {
                    Text(text = "- Zoom Level")
                }
                Button(
                    onClick = {
                        val zoomLevel = state.zoomLevel + 1
                        state.setZoomLevel(zoomLevel)
                    }
                ) {
                    Text(text = "+ Zoom Level")
                }
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Page: ${state.currentPageIndex + 1} / ${state.totalPages}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    ) {
        EpubViewer(
            state = state,
            modifier = Modifier
                .padding(it),
            epubInputStream = epub,
            parseOptions = ParseOptions(
                parseEpubFonts = true,
                customStyles = listOf(
                    Style(
                        css = "a { color: red; }"
                    )
                )
            ),
            loading = {
                CircularProgressIndicator()
            },
            error = {
                Text(
                    text = "Error loading book",
                    color = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun Preview() {
    EpubifyDemoTheme {
        Content()
    }
}