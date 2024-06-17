package org.nextstack.epubify.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing an image in an EPUB file.
 *
 * @property path The URL of the image file. This is typically a relative path
 *               from the directory where the EPUB file was extracted.
 */
@Parcelize
internal data class Image(
    val path: String,
    val image: ByteArray
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as org.nextstack.epubify.model.Image

        if (path != other.path) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}