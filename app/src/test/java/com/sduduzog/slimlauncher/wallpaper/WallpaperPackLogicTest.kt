package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WallpaperPackLogicTest {

    @Test
    fun regeneratesWhenWallpaperIdChanges() {
        assertThat(
            WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = "sys:12:70",
                cachedKey = "sys:11:70",
                inFlightKey = null
            )
        ).isTrue()
    }

    @Test
    fun skipsWhenCachedOrInFlight() {
        assertThat(
            WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = "sys:12:70",
                cachedKey = "sys:12:70",
                inFlightKey = null
            )
        ).isFalse()
        assertThat(
            WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = "sys:13:70",
                cachedKey = "sys:12:70",
                inFlightKey = "sys:13:70"
            )
        ).isFalse()
    }

    @Test
    fun skipsWhenStyledIconsDisabledOrIdMissing() {
        assertThat(
            WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = false,
                currentKey = "sys:12:70",
                cachedKey = null,
                inFlightKey = null
            )
        ).isFalse()
        assertThat(
            WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = null,
                cachedKey = null,
                inFlightKey = null
            )
        ).isFalse()
    }

    @Test
    fun cacheKeyIncludesWallpaperIdAndStrength() {
        assertThat(WallpaperPackLogic.cacheKey("sys:4", 70)).isEqualTo("sys:4:70")
        assertThat(WallpaperPackLogic.cacheKey("sys:4", 40))
            .isNotEqualTo(WallpaperPackLogic.cacheKey("sys:4", 70))
        assertThat(WallpaperPackLogic.strengthHundredths(0.7f)).isEqualTo(70)
    }

    @Test
    fun sampleHashChangesWhenPixelsChange() {
        val first = WallpaperPackLogic.hashSamples(32, 32, intArrayOf(0xFF112233.toInt(), 1))
        val second = WallpaperPackLogic.hashSamples(32, 32, intArrayOf(0xFF445566.toInt(), 1))
        assertThat(first).isNotEqualTo(second)
        assertThat(WallpaperPackLogic.systemWallpaperId(9)).isEqualTo("sys:9")
        assertThat(WallpaperPackLogic.systemWallpaperId(-1)).isNull()
    }
}
