package com.sduduzog.slimlauncher.ui.home

/** Pure decision for a drawer-cell press: tap, long-press (ring), or scroll. */
object CellPressLogic {
    const val LONG_PRESS_MS = 450L

    enum class Outcome { PENDING, TAP, LONG_PRESS, SCROLL }

    /**
     * @param elapsedMs time since finger down
     * @param movedPx distance from the down point
     * @param released finger lifted in this event
     */
    fun classify(elapsedMs: Long, movedPx: Float, released: Boolean, touchSlop: Float): Outcome = when {
        movedPx > touchSlop && elapsedMs < LONG_PRESS_MS -> Outcome.SCROLL
        elapsedMs >= LONG_PRESS_MS -> Outcome.LONG_PRESS
        released -> Outcome.TAP
        else -> Outcome.PENDING
    }

    /** After a long-press began, releasing must never launch the app. */
    fun launchOnRelease(longPressed: Boolean): Boolean = !longPressed
}
