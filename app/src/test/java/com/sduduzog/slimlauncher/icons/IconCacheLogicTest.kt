package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconCacheLogicTest {

    @Test
    fun keyIncludesPackageThemeAndSize() {
        val theme = PackTheme.STOCK
        val a = IconCacheLogic.key("pkg", "act", 0L, "pack", IconCacheLogic.themeHash("pack", theme), 128)
        val b = IconCacheLogic.key("pkg", "act", 0L, "pack", IconCacheLogic.themeHash("pack", theme), 128)
        val c = IconCacheLogic.key("pkg", "act", 0L, "pack", IconCacheLogic.themeHash("pack", theme), 256)
        val otherTheme = theme.copy(seed = 0xFF112233.toInt(), accent = 0xFF445566.toInt())
        val d = IconCacheLogic.key(
            "pkg",
            "act",
            0L,
            "pack",
            IconCacheLogic.themeHash("pack", otherTheme),
            128
        )
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c)
        assertThat(a).isNotEqualTo(d)
    }

    @Test
    fun missingKeysSkipsCachedAndRefreshRespectsStaleWindow() {
        assertThat(IconCacheLogic.missingKeys(setOf("a", "b"), listOf("a", "c", "b")))
            .containsExactly("c")
        assertThat(
            IconCacheLogic.shouldRefreshCatalog(
                lastLoadMs = 1_000,
                nowMs = 2_000,
                staleAfterMs = 15_000,
                hasCache = true
            )
        ).isFalse()
        assertThat(
            IconCacheLogic.shouldRefreshCatalog(
                lastLoadMs = 1_000,
                nowMs = 20_000,
                staleAfterMs = 15_000,
                hasCache = true
            )
        ).isTrue()
        assertThat(
            IconCacheLogic.shouldRefreshCatalog(
                lastLoadMs = 1_000,
                nowMs = 1_100,
                staleAfterMs = 15_000,
                hasCache = false
            )
        ).isTrue()
    }
}
