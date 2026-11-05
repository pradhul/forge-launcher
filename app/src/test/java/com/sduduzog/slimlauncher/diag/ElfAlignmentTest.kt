package com.sduduzog.slimlauncher.diag

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ElfAlignmentTest {

    @Test
    fun sixteenKbLoadsPassAndFourKbLoadsFail() {
        val aligned = ElfAlignment.syntheticElf64(0x4000, 0x10000)
        assertThat(ElfAlignment.isAligned(aligned)).isTrue()
        assertThat(ElfAlignment.loadSegments(aligned).map { it.align })
            .containsExactly(0x4000L, 0x10000L)
            .inOrder()
        assertThat(ElfAlignment.reportSo("ok.so", aligned)).startsWith("OK")

        val bad = ElfAlignment.syntheticElf64(0x4000, 0x1000)
        assertThat(ElfAlignment.isAligned(bad)).isFalse()
        assertThat(ElfAlignment.unalignedLoads(bad).map { it.align })
            .containsExactly(0x1000L)
        assertThat(ElfAlignment.reportSo("bad.so", bad)).startsWith("FAIL")
    }

    @Test
    fun magicAndClassAreRequired() {
        val bytes = ElfAlignment.syntheticElf64(0x4000)
        bytes[0] = 0
        try {
            ElfAlignment.loadSegments(bytes)
            throw AssertionError("expected not ELF")
        } catch (error: IllegalArgumentException) {
            assertThat(error.message).contains("not ELF")
        }
        assertThat(ElfAlignment.MIN_P_ALIGN).isEqualTo(0x4000L)
    }
}
