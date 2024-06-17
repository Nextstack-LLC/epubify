package org.nextstack.epubify.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A [PaddingValues] that only contains vertical padding.
 */
data class VerticalPaddingValues(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : PaddingValues {

    override fun calculateTopPadding(): Dp = top

    override fun calculateBottomPadding(): Dp = bottom

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
}

/**
 * Creates a [VerticalPaddingValues] of [vertical] dp along the 2 vertical edges.
 */
fun VerticalPaddingValues(vertical: Dp = 0.dp) = VerticalPaddingValues(vertical, vertical)