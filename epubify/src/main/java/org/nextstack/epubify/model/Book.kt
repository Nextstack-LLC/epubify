package org.nextstack.epubify.model

import android.os.Parcelable
import org.nextstack.epubify.model.style.Style
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
) : Parcelable