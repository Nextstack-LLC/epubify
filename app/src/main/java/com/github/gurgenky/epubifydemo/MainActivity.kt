package com.github.gurgenky.epubifydemo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.gurgenky.epubify.R
import com.github.gurgenky.epubify.ui.EpubViewer
import com.github.gurgenky.epubifydemo.ui.theme.EpubifyDemoTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EpubifyDemoTheme {
                Scaffold {
                    val epub = resources.openRawResource(R.raw.test2)
                    EpubViewer(
                        modifier = Modifier.padding(it),
                        epubInputStream = epub
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    EpubifyDemoTheme {

    }
}