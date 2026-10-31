package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.data.model.App
import org.junit.Test

class AppDrawerLogicTest {

    private val apps = listOf(
        App("Chrome", "com.android.chrome", "c", 0L),
        App("Camera", "com.android.camera", "c", 0L),
        App("Settings", "com.android.settings", "s", 0L)
    )

    @Test
    fun blankQueryKeepsAlphabeticalOrder() {
        val filtered = AppDrawerLogic.filter(apps.shuffled(), "")
        assertThat(filtered.map { it.appName }).containsExactly("Camera", "Chrome", "Settings").inOrder()
    }

    @Test
    fun queryMatchesNameOrPackage() {
        assertThat(AppDrawerLogic.filter(apps, "chr").map { it.appName }).containsExactly("Chrome")
        assertThat(AppDrawerLogic.filter(apps, "android.settings").map { it.appName })
            .containsExactly("Settings")
    }

    @Test
    fun pinLookupUsesPackageAndUser() {
        val home = listOf("com.android.chrome" to 0L)
        assertThat(AppDrawerLogic.isPinned(home, "com.android.chrome", 0L)).isTrue()
        assertThat(AppDrawerLogic.isPinned(home, "com.android.chrome", 10L)).isFalse()
        assertThat(AppDrawerLogic.isPinned(home, "com.android.camera", 0L)).isFalse()
    }

    @Test
    fun phoneGridIsFourColumnsWithFiftySixDpIcons() {
        assertThat(AppDrawerGrid.PHONE_COLUMNS).isEqualTo(4)
        assertThat(AppDrawerGrid.ICON_DP).isEqualTo(56)
    }

    @Test
    fun gridKeysStayUniqueWhenPackageHasTwoLaunchers() {
        val apps = listOf(
            App("Google", "com.google.android.googlequicksearchbox", "SearchActivity", 0L),
            App("Voice Search", "com.google.android.googlequicksearchbox", "VoiceSearchActivity", 0L),
            App("Google", "com.google.android.googlequicksearchbox", "SearchActivity", 0L)
        )
        val keys = AppDrawerLogic.filter(apps, "").map { AppDrawerLogic.itemKey(it) }
        assertThat(keys).containsNoDuplicates()
        assertThat(keys).hasSize(2)
    }
}
