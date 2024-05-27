package com.github.gurgenky.epubifydemo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.gurgenky.epubify.ui.EpubViewer
import com.github.gurgenky.epubifydemo.ui.theme.EpubifyDemoTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EpubifyDemoTheme {
                Scaffold {
                    Box(
                        modifier = Modifier.padding(it)
                    ) {
                        Content()
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Content() {
    val resources = LocalContext.current.resources
    val epub = resources.openRawResource(R.raw.test2)

    EpubViewer(
        epubInputStream = epub,
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun Preview() {
    EpubifyDemoTheme {
        Content()
    }
}