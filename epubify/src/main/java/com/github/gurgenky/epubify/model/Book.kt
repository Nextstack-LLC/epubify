package com.github.gurgenky.epubify.model

import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.github.gurgenky.epubify.model.style.Style
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a book in an EPUB file.
 *
 * @property title The title of the book.
 * @property author The author of the book.
 * @property cover The cover image of the book.
 * @property chapters The list of chapters in the book.
 * @property images The list of images in the book.
 * @property styles The list of styles in the book.
 */

@Parcelize
internal data class Book(
    val title: String,
    val author: String,
    val cover: Image?,
    val chapters: List<Chapter>,
    val images: List<Image>,
    val styles: List<Style>
) : Parcelable {

    companion object {

        fun Saver() : Saver<MutableState<Book?>, List<Any?>> = Saver(
            save = {
                val book = it.value
                listOf(
                    book?.title,
                    book?.author,
                    book?.cover,
                    book?.chapters,
                    book?.images,
                    book?.styles
                )
            },
            restore = {
                mutableStateOf(
                    Book(
                        it[0] as String,
                        it[1] as String,
                        it[2] as Image?,
                        it[3] as List<Chapter>,
                        it[4] as List<Image>,
                        it[5] as List<Style>
                    )
                )
            }
        )
    }
}