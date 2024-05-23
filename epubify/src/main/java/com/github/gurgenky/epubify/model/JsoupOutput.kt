package com.github.gurgenky.epubify.model

/**
 * Data class representing the output of a Jsoup parse operation.
 *
 * @property title The title of the parsed content.
 * @property content The content of the parsed document.
 */
internal data class JsoupOutput(
    val title: String?,
    val content: String
)

internal typealias TempChapter = JsoupOutput