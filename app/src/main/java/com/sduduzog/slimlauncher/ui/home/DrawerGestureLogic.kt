package com.sduduzog.slimlauncher.ui.home

import kotlin.math.abs

/**
 * Pure swipe rules for opening (home, swipe up) and closing (drawer, swipe
 * down at the top of the list). Decisions are made during the move, not at
 * fling end, so a single short swipe acts immediately.
 */
object DrawerGestureLogic {
    /** Distance (dp) before a vertical swipe commits. */
    const val COMMIT_DP = 24f
    /** Fling velocity (dp/s) that commits even a short swipe on release. */
    const val FLING_DP_PER_S = 480f
    /** Vertical must dominate horizontal by this factor. */
    const val DOMINANCE = 1.3f

    fun isVertical(dx: Float, dy: Float): Boolean = abs(dy) > abs(dx) * DOMINANCE

    /** Drawer close while dragging: [dy] > 0 is downward. */
    fun shouldCloseOnMove(dy: Float, dx: Float, startedAtTop: Boolean, density: Float): Boolean {
        return startedAtTop && dy > COMMIT_DP * density && isVertical(dx, dy)
    }

    fun shouldCloseOnRelease(
        dy: Float,
        dx: Float,
        velocityY: Float,
        startedAtTop: Boolean,
        density: Float
    ): Boolean {
        if (!startedAtTop || dy <= 8f * density || !isVertical(dx, dy)) return false
        return velocityY > FLING_DP_PER_S * density || dy > COMMIT_DP * density
    }

    /** Home open while dragging: upward means [dy] < 0. */
    fun shouldOpenOnMove(dy: Float, dx: Float, density: Float): Boolean {
        return -dy > COMMIT_DP * density && isVertical(dx, dy)
    }

    fun shouldOpenOnRelease(dy: Float, dx: Float, velocityY: Float, density: Float): Boolean {
        if (-dy <= 8f * density || !isVertical(dx, dy)) return false
        return -velocityY > FLING_DP_PER_S * density || -dy > COMMIT_DP * density
    }
}
