package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContrastPickerTest {

    @Test
    fun blackOnWhiteMeetsWcagAaa() {
        val ratio = ContrastPicker.contrastRatio(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        assertThat(ratio).isWithin(0.05).of(21.0)
        assertThat(ratio).isAtLeast(ContrastPicker.WCAG_AA)
    }

    @Test
    fun midGreyOnMidGreyFailsAa() {
        val ratio = ContrastPicker.contrastRatio(0xFF777777.toInt(), 0xFF888888.toInt())
        assertThat(ratio).isLessThan(ContrastPicker.WCAG_AA)
    }

    @Test
    fun pickReadableChoosesWhiteOnDarkPurple() {
        val chosen = ContrastPicker.pickReadable(0xFF1A0433.toInt())
        assertThat(ContrastPicker.contrastRatio(0xFF1A0433.toInt(), chosen))
            .isAtLeast(ContrastPicker.WCAG_AA)
        assertThat(ContrastPicker.relativeLuminance(chosen))
            .isGreaterThan(ContrastPicker.relativeLuminance(0xFF1A0433.toInt()))
    }

    @Test
    fun pickReadableChoosesBlackOnLightCream() {
        val chosen = ContrastPicker.pickReadable(0xFFF4EDE4.toInt())
        assertThat(ContrastPicker.contrastRatio(0xFFF4EDE4.toInt(), chosen))
            .isAtLeast(ContrastPicker.WCAG_AA)
        assertThat(ContrastPicker.relativeLuminance(chosen))
            .isLessThan(ContrastPicker.relativeLuminance(0xFFF4EDE4.toInt()))
    }

    @Test
    fun ensureDarkBackgroundLowersLightSwatch() {
        val darkened = ContrastPicker.ensureDarkBackground(0xFFFFB7C5.toInt())
        assertThat(ContrastPicker.relativeLuminance(darkened)).isAtMost(0.22)
    }
}
