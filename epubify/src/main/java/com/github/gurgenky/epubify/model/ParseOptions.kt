package com.github.gurgenky.epubify.model

import com.github.gurgenky.epubify.model.style.CustomFont

/**
 * Data class representing the options for parsing an EPUB file.
 *
 * @property parseEpubFonts Whether to parse fonts in the EPUB file.
 * @property customFonts The list of custom fonts to use when parsing fonts if parseFonts is false.
 */
data class ParseOptions(
    val parseEpubFonts: Boolean = true,
    val customFonts: List<CustomFont> = emptyList()
)