package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.ui.home.CellPressLogic.Outcome
import org.junit.Test

class CellPressLogicTest {
    private val slop = 24f

    @Test fun quickReleaseIsTap() =
        assertThat(CellPressLogic.classify(120, 2f, true, slop)).isEqualTo(Outcome.TAP)

    @Test fun stationaryHoldIsLongPress() {
        assertThat(CellPressLogic.classify(450, 0f, false, slop)).isEqualTo(Outcome.LONG_PRESS)
        assertThat(CellPressLogic.classify(1000, 0f, false, slop)).isEqualTo(Outcome.LONG_PRESS)
    }

    @Test fun releaseAfterHoldIsNotTap() =
        assertThat(CellPressLogic.classify(1000, 0f, true, slop)).isEqualTo(Outcome.LONG_PRESS)

    @Test fun earlyMoveIsScroll() =
        assertThat(CellPressLogic.classify(100, 60f, false, slop)).isEqualTo(Outcome.SCROLL)

    @Test fun stillHoldingBeforeTimeoutIsPending() =
        assertThat(CellPressLogic.classify(300, 3f, false, slop)).isEqualTo(Outcome.PENDING)

    @Test fun longPressNeverLaunchesOnRelease() {
        assertThat(CellPressLogic.launchOnRelease(true)).isFalse()
        assertThat(CellPressLogic.launchOnRelease(false)).isTrue()
    }

    @Test fun windowIsAbout450ms() =
        assertThat(CellPressLogic.LONG_PRESS_MS).isIn(com.google.common.collect.Range.closed(400L, 500L))
}
