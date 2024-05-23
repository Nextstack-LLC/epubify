package com.github.gurgenky.epubify.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * A gesture listener that detects swipe gestures.
 */
class SwipeGestureListener(
    private val onSwipeLeft: () -> Unit = {},
    private val onSwipeRight: () -> Unit = {}
) : GestureDetector.SimpleOnGestureListener() {

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    /**
     *  Detects a swipe gesture based on the difference between the start and end coordinates.
     */
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

    /**
     * Constants for swipe detection.
     */
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
