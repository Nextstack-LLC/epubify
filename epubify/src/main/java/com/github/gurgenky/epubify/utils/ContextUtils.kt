package com.github.gurgenky.epubify.utils

import android.content.Context
import android.util.TypedValue

internal fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    resources.displayMetrics
)

internal fun Context.toDp(px: Int): Float = px / resources.displayMetrics.density