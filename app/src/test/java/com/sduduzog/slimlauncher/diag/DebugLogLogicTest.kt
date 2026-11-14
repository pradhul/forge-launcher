package com.sduduzog.slimlauncher.diag

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DebugLogLogicTest {

    @Test
    fun stallLogsOnlyForegroundGapsBetweenThresholdAndBackgroundPause() {
        assertThat(DebugLogLogic.shouldLogStall(499, foreground = true)).isFalse()
        assertThat(DebugLogLogic.shouldLogStall(501, foreground = true)).isTrue()
        assertThat(DebugLogLogic.shouldLogStall(2_000, foreground = false)).isFalse()
        assertThat(DebugLogLogic.shouldLogStall(20_000, foreground = true)).isFalse()
    }

    @Test
    fun stallLineIncludesGapAndStack() {
        val line = DebugLogLogic.formatStall(800, "  at main", 1_700L)
        assertThat(line).contains("STALL gap=800ms")
        assertThat(line).contains("at=1700")
        assertThat(line).contains("at main")
    }

    @Test
    fun anrReasonIncludesTraceAndNamedReasons() {
        assertThat(DebugLogLogic.reasonName(DebugLogLogic.REASON_ANR)).isEqualTo("ANR")
        assertThat(DebugLogLogic.reasonName(DebugLogLogic.REASON_CRASH)).isEqualTo("CRASH")
        assertThat(DebugLogLogic.shouldIncludeAnrTrace(DebugLogLogic.REASON_ANR)).isTrue()
        assertThat(DebugLogLogic.shouldIncludeAnrTrace(DebugLogLogic.REASON_CRASH)).isFalse()
        val anr = DebugLogLogic.formatExit(10L, DebugLogLogic.REASON_ANR, "drawer", "trace-line")
        assertThat(anr).contains("reason=ANR")
        assertThat(anr).contains("desc=drawer")
        assertThat(anr).contains("trace-line")
        val crash = DebugLogLogic.formatExit(11L, DebugLogLogic.REASON_CRASH, null, "ignored")
        assertThat(crash).doesNotContain("ignored")
    }

    @Test
    fun headerAndTrimKeepTheNewestBytes() {
        val header = DebugLogLogic.header("1.0.0", 64, 36, "Google Pixel")
        assertThat(header).contains("Forge 1.0.0 (64)")
        assertThat(header).contains("sdk=36")
        assertThat(header).contains("Google Pixel")
        val trimmed = DebugLogLogic.trimToMax("abcdefghij", maxBytes = 4)
        assertThat(trimmed).isEqualTo("ghij")
        assertThat(DebugLogLogic.exitLimit(29)).isEqualTo(0)
        assertThat(DebugLogLogic.exitLimit(30)).isEqualTo(5)
    }
}
