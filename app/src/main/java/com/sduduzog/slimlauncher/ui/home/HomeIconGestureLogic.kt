package com.sduduzog.slimlauncher.ui.home

/**
 * Home icons show the frosted popup on long-press. Drag starts only after
 * the finger then moves past slop, so ItemTouchHelper cannot steal the menu.
 */
object HomeIconGestureLogic {
    fun pastDragSlop(dx: Float, dy: Float, slop: Float): Boolean {
        return dx * dx + dy * dy > slop * slop
    }

    fun shouldStartDrag(
        longPressed: Boolean,
        alreadyDragging: Boolean,
        dx: Float,
        dy: Float,
        slop: Float
    ): Boolean {
        return longPressed && !alreadyDragging && pastDragSlop(dx, dy, slop)
    }
}
