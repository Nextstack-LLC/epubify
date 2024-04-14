package com.github.gurgenky.epubify.model

/**
 * Data class representing the metadata of a book in an EPUB file.
 *
 * @property title The title of the book.
 * @property author The author of the book.
 * @property cover The cover image of the book.
 */
internal data class Metadata(
    val title: String,
    val author: String,
    val cover: Image?
)