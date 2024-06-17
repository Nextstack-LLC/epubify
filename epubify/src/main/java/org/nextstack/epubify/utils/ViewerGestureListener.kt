package org.nextstack.epubify.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * A gesture listener that detects swipe gestures and single taps.
 */
internal class ViewerGestureListener(
    private val onSwipeLeft: () -> Unit = {},
    private val onSwipeRight: () -> Unit = {},
    private val onTap: () -> Unit = {}
) : GestureDetector.SimpleOnGestureListener() {

    /**
     * Empty implementation to override the default behavior.
     */
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    /**
     * Detects a single tap gesture.
     */
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        onTap()
        return super.onSingleTapConfirmed(e)
    }

    /**
     * Detects a down gesture.
     */
    override fun onDown(e: MotionEvent): Boolean {
        return when (e.action) {
            MotionEvent.ACTION_SCROLL -> {
                true
            }

            MotionEvent.ACTION_MOVE -> {
                true
            }
            else -> {
                false
            }
        }
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
                true
            }
        } else {
            true
        }
    }

    /**
     * Empty implementation to override the default behavior.
     */
    override fun onLongPress(e: MotionEvent) {
        // do nothing
    }

    /**
     * Constants for swipe detection.
     */
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
