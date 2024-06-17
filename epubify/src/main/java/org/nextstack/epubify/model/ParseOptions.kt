package org.nextstack.epubify.model

import org.nextstack.epubify.model.style.CustomFont
import org.nextstack.epubify.model.style.Style

/**
 * Data class representing the options for parsing an EPUB file.
 *
 * @property parseEpubFonts Whether to parse fonts in the EPUB file.
 * @property customFonts The list of custom fonts to use when parsing fonts if parseFonts is false.
 * @property customStyles The list of custom CSS styles to use when parsing the EPUB file.
 */
data class ParseOptions(
    val parseEpubFonts: Boolean = true,
    val customFonts: List<CustomFont> = emptyList(),
    val customStyles: List<Style> = emptyList()
)