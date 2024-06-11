package com.github.gurgenky.epubify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * State class for the [EpubViewer] composable.
 *
 * @param currentPage The current page being displayed.
 * @param pendingJumpPage The page to jump to once the current page is displayed.
 * @param totalPages The total number of pages in the EPUB.
 * @param zoomLevel The current zoom level.
 */
class EpubViewerState internal constructor(
    currentPage: Int,
    pendingJumpPage: Int,
    totalPages: Int,
    zoomLevel: Int
) {

    private var _currentPage: MutableState<Int> = mutableIntStateOf(currentPage)

    /**
     * The index of the current page being displayed.
     */
    val currentPageIndex: Int get() = _currentPage.value


    private var _pendingJumpPage: MutableState<Int> = mutableIntStateOf(pendingJumpPage)

    /**
     * The page to jump to once the current page is displayed.
     */
    internal val pendingJumpPageIndex: Int get() = _pendingJumpPage.value


    private var _totalPages: MutableState<Int> = mutableIntStateOf(totalPages)

    /**
     * The total number of pages in the EPUB.
     */
    val totalPages: Int get() = _totalPages.value

    private var _zoomLevel: MutableState<Int> = mutableIntStateOf(zoomLevel)

    /**
     * The total number of pages in the EPUB.
     */
    val zoomLevel: Int get() = _zoomLevel.value

    /**
     * Jumps to the specified page.
     *
     * @param pageIndex The page index to jump to.
     */
    fun jumpTo(pageIndex: Int) {
        _pendingJumpPage.value = pageIndex
    }

    /**
     * Sets the zoom level.
     *
     * @param zoomLevel The zoom level to set.
     */
    fun setZoomLevel(zoomLevel: Int) {
        if (zoomLevel in minimumZoomLevel..maxZoomLevel) {
            _zoomLevel.value = zoomLevel
        }
    }

    /**
     * Sets the current page.
     *
     * @param pageIndex The page to set.
     */
    internal fun setCurrentPage(pageIndex: Int) {
        _currentPage.value = pageIndex
    }

    /**
     * Sets the total number of pages in the EPUB.
     *
     * @param totalPages The total number of pages.
     */
    internal fun setTotalPages(totalPages: Int) {
        _totalPages.value = totalPages
    }

    /**
     * Clears the pending jump page.
     */
    internal fun clearPendingJump() {
        _pendingJumpPage.value = -1
    }

    companion object {
        /**
         * The default zoom level.
         */
        const val defaultZoomLevel = 1

        /**
         * The minimum zoom level.
         */
        const val minimumZoomLevel = 1

        /**
         * The maximum zoom level.
         */
        const val maxZoomLevel = 4

        /**
         * Saver for the [EpubViewerState] class.
         */
        fun Saver(): Saver<EpubViewerState, List<Int>> = Saver(
            save = { listOf(it.currentPageIndex, it.pendingJumpPageIndex, it.totalPages, it.zoomLevel) },
            restore = { EpubViewerState(it[0], it[1], it[2], it[3]) }
        )
    }
}

/**
 * Remembers the [EpubViewerState] state.
 *
 * @param currentPage The current page being displayed.
 * @param pendingJumpPage The page to jump to once the current page is displayed.
 * @param saver The saver for the state.
 */
@Composable
fun rememberEpubViewerState(
    currentPage: Int = 0,
    pendingJumpPage: Int = -1,
    saver: Saver<EpubViewerState, List<Int>> = EpubViewerState.Saver()
): EpubViewerState {
    return rememberSaveable(saver = saver) {
        EpubViewerState(currentPage, pendingJumpPage, 0, EpubViewerState.defaultZoomLevel)
    }
}