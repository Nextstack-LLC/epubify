package com.github.gurgenky.epubify.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeGestureListener(
    private val onSwipeLeft: () -> Unit = {},
    private val onSwipeRight: () -> Unit = {}
) : GestureDetector.SimpleOnGestureListener() {

    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val diffX = e2.x.minus(e1?.x ?: 0f)
        val diffY = e2.y.minus(e1?.y ?: 0f)

        return if (abs(diffX) > abs(diffY)) {
            if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    onSwipeLeft()
                } else {
                    onSwipeRight()
                }
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}
