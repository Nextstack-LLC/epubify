package org.nextstack.epubify.model.style

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a CSS style.
 * Fonts are included in the CSS style as Base64 encoded strings.
 *
 * @property css The CSS style.
 */
@Parcelize
data class Style(
    val css: String
) : Parcelable