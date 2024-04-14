package com.github.gurgenky.epubify.model

/**
 * Data class representing a chapter in an EPUB file.
 *
 * @property title The title of the chapter.
 * @property content The content of the chapter.
 */
internal data class Chapter(
    val title: String,
    val content: String
)