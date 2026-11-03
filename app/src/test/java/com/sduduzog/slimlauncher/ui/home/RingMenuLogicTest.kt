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
