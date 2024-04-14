package com.github.gurgenky.epubify.model

/**
 * Data class representing a book in an EPUB file.
 *
 * @property title The title of the book.
 * @property author The author of the book.
 * @property cover The cover image of the book.
 * @property chapters The list of chapters in the book.
 */
internal data class Book(
    val title: String,
    val author: String,
    val cover: Image?,
    val chapters: List<Chapter>,
    val images: List<Image>
)