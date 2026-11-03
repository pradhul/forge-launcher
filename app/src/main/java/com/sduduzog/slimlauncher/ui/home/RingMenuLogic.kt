package com.sduduzog.slimlauncher.ui.home

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class RingAction { APP_INFO, DOCK, UNINSTALL }

data class RingPoint(val x: Float, val y: Float)

data class RingLayout(
    val options: List<Pair<RingAction, RingPoint>>,
    val cancel: RingPoint,
    val flipped: Boolean
)

/** Sentinel results of a hit test besides an option. */
sealed class RingHit {
    data class Option(val action: RingAction) : RingHit()
    object Cancel : RingHit()
    object Center : RingHit()
    object None : RingHit()
}

/**
 * Radial long-press menu geometry: options on an arc above the icon (below
 * if there is no room), Cancel zone on the opposite side. Pure math in px.
 */
object RingMenuLogic {
    val ORDER = listOf(RingAction.APP_INFO, RingAction.DOCK, RingAction.UNINSTALL)
    private val ANGLES_UP = floatArrayOf(215f, 270f, 325f)
    private val ANGLES_DOWN = floatArrayOf(145f, 90f, 35f)

    fun layout(
        cx: Float,
        cy: Float,
        radius: Float,
        width: Float,
        height: Float,
        margin: Float,
        cancelDistance: Float
    ): RingLayout {
        val flipped = cy - radius - margin < 0f
        val angles = if (flipped) ANGLES_DOWN else ANGLES_UP
        val opts = ORDER.mapIndexed { i, action ->
            val a = Math.toRadians(angles[i].toDouble())
            val x = (cx + radius * cos(a)).toFloat().coerceIn(margin, width - margin)
            val y = (cy + radius * sin(a)).toFloat().coerceIn(margin, height - margin)
            action to RingPoint(x, y)
        }
        val cancelY = if (flipped) cy - cancelDistance else cy + cancelDistance
        return RingLayout(
            opts,
            RingPoint(cx, cancelY.coerceIn(margin, height - margin)),
            flipped
        )
    }

    fun hitTest(
        layout: RingLayout,
        cx: Float,
        cy: Float,
        px: Float,
        py: Float,
        hitRadius: Float
    ): RingHit {
        var best: RingAction? = null
        var bestD = Float.MAX_VALUE
        for ((action, p) in layout.options) {
            val d = hypot(px - p.x, py - p.y)
            if (d < hitRadius && d < bestD) { best = action; bestD = d }
        }
        if (best != null) return RingHit.Option(best)
        if (hypot(px - layout.cancel.x, py - layout.cancel.y) < hitRadius) return RingHit.Cancel
        if (hypot(px - cx, py - cy) < hitRadius) return RingHit.Center
        return RingHit.None
    }
}
