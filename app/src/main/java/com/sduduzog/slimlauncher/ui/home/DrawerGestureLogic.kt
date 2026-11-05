package com.sduduzog.slimlauncher.ui.home

import kotlin.math.abs

/**
 * Pure swipe rules for opening (home, swipe up) and closing (drawer, swipe
 * down at the top of the list). Opening needs a longer intentional swipe
 * than closing so a tiny upward nudge on home does not open the drawer.
 */
object DrawerGestureLogic {
    /** Distance (dp) before a drawer *close* swipe commits. */
    const val COMMIT_DP = 28f
    /** Distance (dp) before a home *open* swipe commits. Higher than close so a nudge does not open the drawer. */
    const val OPEN_COMMIT_DP = 52f
    /** Even a fast flick must travel this far (dp) before opening. */
    const val OPEN_MIN_DP = 28f
    /** Fling velocity (dp/s) that commits a close on release. */
    const val FLING_DP_PER_S = 400f
    /** Faster flick required to open from home. */
    const val OPEN_FLING_DP_PER_S = 720f
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
        return -dy >= OPEN_COMMIT_DP * density && isVertical(dx, dy)
    }

    fun shouldOpenOnRelease(dy: Float, dx: Float, velocityY: Float, density: Float): Boolean {
        if (-dy < OPEN_MIN_DP * density || !isVertical(dx, dy)) return false
        return -velocityY > OPEN_FLING_DP_PER_S * density || -dy >= OPEN_COMMIT_DP * density
    }
}
