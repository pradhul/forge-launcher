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
/** coerceIn that never throws: an empty range (tiny/unknown container) keeps the value. */
internal fun Float.safeCoerce(min: Float, max: Float): Float {
    if (isNaN() || min.isNaN() || max.isNaN()) return this
    if (max < min) return this
    return coerceIn(min, max)
}

object RingMenuLogic {
    val ORDER = listOf(RingAction.APP_INFO, RingAction.DOCK, RingAction.UNINSTALL)

    /** Angular step between neighbouring bubbles (degrees). */
    const val STEP_DEG = 55f

    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float) {
        fun overlaps(o: Rect) = l < o.r && o.l < r && t < o.b && o.t < b
        fun grow(d: Float) = Rect(l - d, t - d, r + d, b + d)
        fun inside(w: Float, h: Float, m: Float) = l >= m && t >= m && r <= w - m && b <= h - m
    }

    /** Footprint of a bubble + its label below, centred on the bubble centre. */
    fun itemRect(p: RingPoint, bubble: Float, label: Float): Rect {
        val half = bubble * 0.8f // label text is ~1.6 bubbles wide max
        return Rect(p.x - half, p.y - bubble / 2, p.x + half, p.y + bubble / 2 + label)
    }

    fun cancelRect(p: RingPoint, cw: Float, ch: Float) =
        Rect(p.x - cw / 2, p.y - ch / 2, p.x + cw / 2, p.y + ch / 2)

    /**
     * Fan the 3 options around the icon. The arc centre direction prefers
     * "up, tilted toward the screen centre" (below when near the top), and we
     * search nearby directions / slightly larger radii until every bubble +
     * label is on screen, none overlap each other, the icon or Cancel.
     * Never clamps bubbles individually; never throws.
     */
    fun layout(
        cx: Float,
        cy: Float,
        radius: Float,
        width: Float,
        height: Float,
        margin: Float,
        cancelDistance: Float,
        bubble: Float = radius * 64f / 132f,
        label: Float = radius * 28f / 132f,
        cancelW: Float = radius,
        cancelH: Float = radius * 48f / 132f
    ): RingLayout {
        // Options go up when there is room; Cancel then sits below the icon
        // (shifted fully on screen). Near the top, options flip below and
        // Cancel goes above.
        val roomAbove = cy - radius - bubble / 2 - margin >= 0f
        val cancelBelow = roomAbove
        val cancel = RingPoint(
            cx.safeCoerce(margin + cancelW / 2, width - margin - cancelW / 2),
            (if (cancelBelow) cy + cancelDistance else cy - cancelDistance)
                .safeCoerce(margin + cancelH / 2, height - margin - cancelH / 2)
        )
        val iconRect = Rect(cx - bubble / 2, cy - bubble / 2, cx + bubble / 2, cy + bubble / 2)
        fun clampCancel(x: Float, y: Float) = RingPoint(
            x.safeCoerce(margin + cancelW / 2, width - margin - cancelW / 2),
            y.safeCoerce(margin + cancelH / 2, height - margin - cancelH / 2)
        )
        val toCentre = if (cx < width / 2) 1f else -1f
        val besideDx = cancelW / 2 + bubble * 0.75f + margin
        // Cancel candidates in preference order; any that would sit on the
        // icon (under the finger) is skipped.
        val cancelCandidates = (listOf(cancel) + listOf(1f, 1.4f, 1.8f).flatMap { k ->
            listOf(
                clampCancel(cx, cy + cancelDistance * k),
                clampCancel(cx + toCentre * besideDx * k, cy),
                clampCancel(cx + toCentre * besideDx * k, cy + cancelDistance * 0.7f * k),
                clampCancel(cx, cy - cancelDistance * k),
                clampCancel(cx + toCentre * besideDx * k, cy - cancelDistance * 0.7f * k),
                clampCancel(cx - toCentre * besideDx * k, cy)
            )
        }        ).filter { !cancelRect(it, cancelW, cancelH).overlaps(iconRect.grow(bubble * 0.25f)) }
            .ifEmpty { listOf(cancel) }

        val t = if (width > 0f) ((cx - width / 2) / (width / 2)).coerceIn(-1f, 1f) else 0f
        val preferred = if (roomAbove) 270f - 50f * t else 90f + 50f * t

        fun build(center: Float, r: Float, step: Float = STEP_DEG): List<RingPoint> {
            // Keep App info / Dock / Uninstall reading left -> right.
            val up = sin(Math.toRadians(center.toDouble())) <= 0
            val dir = if (up) 1f else -1f
            return (0 until 3).map { i ->
                val a = Math.toRadians((center + dir * (i - 1) * step).toDouble())
                RingPoint((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat())
            }
        }

        fun fits(pts: List<RingPoint>, cRect: Rect): Boolean {
            val rects = pts.map { itemRect(it, bubble, label) }
            if (rects.any { !it.inside(width, height, margin) || it.overlaps(cRect) || it.overlaps(iconRect) }) return false
            for (i in rects.indices) for (j in i + 1 until rects.size) if (rects[i].overlaps(rects[j])) return false
            return true
        }

        val candidates = (0 until 72).map { it * 5f }.sortedBy { angDist(it, preferred) }
        var chosen: List<RingPoint>? = null
        var chosenCenter = preferred
        var cancelP = cancelCandidates.first()
        search@ for (cp in cancelCandidates) {
            val cRect = cancelRect(cp, cancelW, cancelH)
            for (scale in floatArrayOf(1f, 1.15f, 1.3f, 1.5f, 1.7f)) for (step in floatArrayOf(STEP_DEG, 45f, 42f, 38f)) {
                for (c in candidates) {
                    val pts = build(c, radius * scale, step)
                    if (fits(pts, cRect)) { chosen = pts; chosenCenter = c; cancelP = cp; break@search }
                }
            }
        }
        // No arrangement fits (tiny container): best effort, clamped on screen.
        val pts = chosen ?: build(preferred, radius).map {
            RingPoint(
                it.x.safeCoerce(margin + bubble * 0.8f, width - margin - bubble * 0.8f),
                it.y.safeCoerce(margin + bubble / 2, height - margin - bubble / 2 - label)
            )
        }
        val flipped = sin(Math.toRadians(chosenCenter.toDouble())) > 0
        return RingLayout(ORDER.zip(pts), cancelP, flipped)
    }

    private fun angDist(a: Float, b: Float): Float {
        val d = ((a - b) % 360f + 360f) % 360f
        return if (d > 180f) 360f - d else d
    }

    enum class Release { ACTION, DISMISS, KEEP_OPEN }

    /**
     * What a finger-up does. A long-press released without dragging always
     * keeps the ring open for a tap choice, whatever lies under the finger.
     */
    fun onRelease(hit: RingHit, moved: Boolean): Release = when {
        !moved -> Release.KEEP_OPEN
        hit is RingHit.Option -> Release.ACTION
        hit == RingHit.Center -> Release.KEEP_OPEN
        else -> Release.DISMISS
    }

    /** Has the pointer left the press point by more than [slop]? Sticky once true. */
    fun movedFrom(press: RingPoint?, x: Float, y: Float, slop: Float, already: Boolean): Boolean =
        already || (press != null && hypot(x - press.x, y - press.y) > slop)

    /** Size is known and usable (BoxWithConstraints may report 0 / Infinity first). */
    fun canLayout(width: Float, height: Float): Boolean =
        width.isFinite() && height.isFinite() && width > 0f && height > 0f && width < 100_000f && height < 100_000f

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
