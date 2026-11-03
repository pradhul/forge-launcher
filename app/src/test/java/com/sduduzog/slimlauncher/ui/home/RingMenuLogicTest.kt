package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
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
        assertThat(l.cancel.y).isLessThan(l.options[1].second.y)
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
