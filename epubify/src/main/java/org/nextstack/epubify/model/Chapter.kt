package org.nextstack.epubify.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a chapter in an EPUB file.
 *
 * @property title The title of the chapter.
 * @property content The content of the chapter.
 */
@Parcelize
internal data class Chapter(
    val title: String,
    val content: String
) : Parcelable {

    companion object
}