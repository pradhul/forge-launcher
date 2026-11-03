package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class RingMenuLogicTest {
    private fun lay(cx: Float, cy: Float) = RingMenuLogic.layout(cx, cy, 360f, 1080f, 2400f, 130f, 410f)

    @Test
    fun threeOptionsInOrderAboveIconWithCancelBelow() {
        val l = lay(540f, 1200f)
        assertThat(l.options.map { it.first }).containsExactly(
            RingAction.APP_INFO, RingAction.DOCK, RingAction.UNINSTALL
        ).inOrder()
        assertThat(l.flipped).isFalse()
        l.options.forEach { assertThat(it.second.y).isLessThan(1200f) }
        assertThat(l.cancel.y).isGreaterThan(1200f)
        assertThat(l.options[1].second.x).isWithin(1f).of(540f)
    }

    @Test
    fun flipsBelowNearTopEdge() {
        val l = lay(540f, 200f)
        assertThat(l.flipped).isTrue()
        l.options.forEach { assertThat(it.second.y).isGreaterThan(200f) }
        // Cancel may sit above, below or beside; it just must not be under the finger.
        assertThat(RingMenuLogic.hitTest(l, 540f, 200f, 540f, 200f, 140f)).isEqualTo(RingHit.Center)
    }

    @Test
    fun optionsStayOnScreenAtEdges() {
        val l = lay(40f, 1200f)
        l.options.forEach { assertThat(it.second.x).isAtLeast(130f) }
    }

    @Test
    fun hitTestPicksOptionCancelCenterOrNone() {
        val l = lay(540f, 1200f)
        val dock = l.options[1].second
        assertThat(RingMenuLogic.hitTest(l, 540f, 1200f, dock.x + 10f, dock.y, 140f))
            .isEqualTo(RingHit.Option(RingAction.DOCK))
        assertThat(RingMenuLogic.hitTest(l, 540f, 1200f, l.cancel.x, l.cancel.y, 140f)).isEqualTo(RingHit.Cancel)
        assertThat(RingMenuLogic.hitTest(l, 540f, 1200f, 545f, 1195f, 140f)).isEqualTo(RingHit.Center)
        assertThat(RingMenuLogic.hitTest(l, 540f, 1200f, 1000f, 2300f, 140f)).isEqualTo(RingHit.None)
    }
}

class RingMenuLogicSafetyTest {
    @Test
    fun zeroOrTinyContainerNeverThrows() {
        for ((w, h) in listOf(0f to 0f, 1f to 1f, 100f to 80f, 0f to 2400f, 1080f to 0f)) {
            val l = RingMenuLogic.layout(540f, 1200f, 360f, w, h, 130f, 410f)
            assertThat(l.options).hasSize(3)
            l.options.forEach {
                assertThat(it.second.x.isNaN()).isFalse()
                assertThat(it.second.y.isNaN()).isFalse()
            }
        }
    }

    @Test
    fun canLayoutRejectsUnknownSizes() {
        assertThat(RingMenuLogic.canLayout(0f, 0f)).isFalse()
        assertThat(RingMenuLogic.canLayout(Float.POSITIVE_INFINITY, 2400f)).isFalse()
        assertThat(RingMenuLogic.canLayout(1080f, 2400f)).isTrue()
    }

    @Test
    fun reportedCrashInputsAreSafe() {
        // maximum -105.8 < minimum 106.8 (container narrower than 2x margin)
        val l = RingMenuLogic.layout(50f, 50f, 360f, 1f, 1f, 106.8f, 410f)
        assertThat(l.options).hasSize(3)
    }
}

/** Phone geometry: 1080x2400 @ 2.625 density, sizes exactly as in RingMenuOverlay. */
class RingMenuEdgeLayoutTest {
    private val dp = 2.625f
    private val w = 1080f
    private val h = 2400f
    private val radius = 132 * dp
    private val bubble = 64 * dp
    private val label = 28 * dp
    private val margin = 8 * dp
    private val cancelW = 132 * dp
    private val cancelH = 48 * dp
    private val cols = listOf(135f, 405f, 675f, 945f)
    private val rows = mapOf("top" to 300f, "middle" to 1200f, "bottom" to 2100f)

    private fun lay(cx: Float, cy: Float) =
        RingMenuLogic.layout(cx, cy, radius, w, h, margin, 150 * dp, bubble, label, cancelW, cancelH)

    @Test
    fun everyColumnAndRowFitsWithoutOverlap() {
        for (cx in cols) for ((name, cy) in rows) {
            val l = lay(cx, cy)
            val where = "col x=$cx row=$name"
            val rects = l.options.map { RingMenuLogic.itemRect(it.second, bubble, label) }
            val cancel = RingMenuLogic.cancelRect(l.cancel, cancelW, cancelH)
            rects.forEach { assertWithMessage("$where in bounds $it").that(it.inside(w, h, margin)).isTrue() }
            assertWithMessage("$where cancel").that(cancel.inside(w, h, margin - 0.5f)).isTrue()
            for (i in rects.indices) {
                assertWithMessage("$where opt$i vs cancel").that(rects[i].overlaps(cancel)).isFalse()
                for (j in i + 1 until rects.size) {
                    assertWithMessage("$where opt$i vs opt$j").that(rects[i].overlaps(rects[j])).isFalse()
                    val a = l.options[i].second; val b = l.options[j].second
                    assertWithMessage("$where spacing").that(Math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())).isAtLeast(bubble * 1.2)
                }
            }
        }
    }

    @Test
    fun leftColumnFansRightAndRightColumnFansLeft() {
        val left = lay(cols[0], 1200f)
        left.options.forEach { assertThat(it.second.x).isGreaterThan(cols[0]) }
        val right = lay(cols[3], 1200f)
        right.options.forEach { assertThat(it.second.x).isLessThan(cols[3]) }
    }

    @Test
    fun cancelBelowIconAndOnScreenForMiddleRows() {
        for (cx in cols) {
            val l = lay(cx, 1200f)
            assertThat(l.cancel.y).isGreaterThan(1200f)
            assertThat(l.flipped).isFalse()
        }
    }

    @Test
    fun topRowFlipsBelow() {
        for (cx in cols) assertThat(lay(cx, 300f).flipped).isTrue()
    }
}

/** v28: top-left / bottom-left corners (WhatsApp in Frequent row, BookMyShow near search bar). */
class RingMenuCornerTest {
    private val dp = 2.625f
    private val radius = 132 * dp
    private val bubble = 64 * dp
    private val label = 28 * dp
    private val margin = 8 * dp
    private val cancelW = 132 * dp
    private val cancelH = 48 * dp
    private val hit = 52 * dp

    private fun lay(cx: Float, cy: Float, h: Float) =
        RingMenuLogic.layout(cx, cy, radius, 1080f, h, margin, 150 * dp, bubble, label, cancelW, cancelH)

    @Test
    fun cornersFitAndFingerIsNeverOnCancelOrOption() {
        for (h in listOf(2400f, 2268f, 2050f)) for (cx in listOf(135f, 945f))
            for (cy in listOf(200f, 300f, 360f, h - 400f, h - 250f, h - 160f)) {
                val l = lay(cx, cy, h)
                val where = "cx=$cx cy=$cy h=$h"
                assertWithMessage("$where finger hit").that(RingMenuLogic.hitTest(l, cx, cy, cx, cy, hit))
                    .isEqualTo(RingHit.Center)
                val rects = l.options.map { RingMenuLogic.itemRect(it.second, bubble, label) }
                val cancel = RingMenuLogic.cancelRect(l.cancel, cancelW, cancelH)
                assertWithMessage("$where cancel on screen").that(cancel.inside(1080f, h, margin - 0.5f)).isTrue()
                rects.forEach { assertWithMessage("$where opt on screen $it").that(it.inside(1080f, h, margin - 0.5f)).isTrue() }
                for (i in rects.indices) {
                    assertWithMessage("$where opt$i vs cancel").that(rects[i].overlaps(cancel)).isFalse()
                    for (j in i + 1 until rects.size)
                        assertWithMessage("$where opt$i vs opt$j").that(rects[i].overlaps(rects[j])).isFalse()
                }
            }
    }

    @Test
    fun stationaryHoldReleaseKeepsRingOpen() {
        for (h in listOf(RingHit.Cancel, RingHit.None, RingHit.Center, RingHit.Option(RingAction.DOCK)))
            assertThat(RingMenuLogic.onRelease(h, moved = false)).isEqualTo(RingMenuLogic.Release.KEEP_OPEN)
    }

    @Test
    fun dragReleaseActsOrDismisses() {
        assertThat(RingMenuLogic.onRelease(RingHit.Option(RingAction.APP_INFO), true)).isEqualTo(RingMenuLogic.Release.ACTION)
        assertThat(RingMenuLogic.onRelease(RingHit.Cancel, true)).isEqualTo(RingMenuLogic.Release.DISMISS)
        assertThat(RingMenuLogic.onRelease(RingHit.None, true)).isEqualTo(RingMenuLogic.Release.DISMISS)
        assertThat(RingMenuLogic.onRelease(RingHit.Center, true)).isEqualTo(RingMenuLogic.Release.KEEP_OPEN)
    }

    @Test
    fun movedIsStickyAndNeedsSlop() {
        val p = RingPoint(100f, 100f)
        assertThat(RingMenuLogic.movedFrom(p, 105f, 103f, 42f, false)).isFalse()
        assertThat(RingMenuLogic.movedFrom(p, 200f, 100f, 42f, false)).isTrue()
        assertThat(RingMenuLogic.movedFrom(p, 100f, 100f, 42f, true)).isTrue()
    }
}
