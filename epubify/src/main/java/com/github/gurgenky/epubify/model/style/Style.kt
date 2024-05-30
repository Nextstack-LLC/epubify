package com.github.gurgenky.epubify.model.style

/**
 * Data class representing a CSS style.
 * Fonts are included in the CSS style as Base64 encoded strings.
 *
 * @property css The CSS style.
 */
data class Style(
    val css: String
)