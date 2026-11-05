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
    fun pinLookupUsesPackageActivityAndUserOnVisibleHome() {
        val home = listOf(
            com.sduduzog.slimlauncher.models.HomeApp(
                "Chrome", "com.android.chrome", "Main", 0, null, 0L
            )
        )
        assertThat(AppDrawerLogic.isPinned(home, "com.android.chrome", "Main", 0L)).isTrue()
        assertThat(AppDrawerLogic.isPinned(home, "com.android.chrome", "CustomTab", 0L)).isFalse()
        assertThat(AppDrawerLogic.isPinned(home, "com.android.chrome", "Main", 10L)).isFalse()
        assertThat(AppDrawerLogic.isPinned(home, "com.android.camera", "c", 0L)).isFalse()
    }

    @Test
    fun phoneGridIsFourColumnsWithFiftySixDpIcons() {
        assertThat(AppDrawerGrid.PHONE_COLUMNS).isEqualTo(4)
        assertThat(AppDrawerGrid.ICON_DP).isEqualTo(56)
        assertThat(LaunchRanker.LIMIT).isEqualTo(AppDrawerGrid.PHONE_COLUMNS)
    }

    @Test
    fun imeSearchLaunchesOnlyWhenExactlyOneMatch() {
        assertThat(AppDrawerLogic.imeLaunchTarget(emptyList())).isNull()
        assertThat(AppDrawerLogic.imeLaunchTarget(apps)).isNull()
        assertThat(AppDrawerLogic.imeLaunchTarget(listOf(apps.first())))
            .isEqualTo(apps.first())
    }

    @Test
    fun searchDebounceIsShortAndCatalogStaleIsSeconds() {
        assertThat(AppDrawerLogic.SEARCH_DEBOUNCE_MS).isAtLeast(50L)
        assertThat(AppDrawerLogic.SEARCH_DEBOUNCE_MS).isAtMost(150L)
        assertThat(AppDrawerLogic.CATALOG_STALE_MS).isAtLeast(5_000L)
    }

    @Test
    fun keyboardOpensOnlyWhenSettingOnAndDrawerVisible() {
        assertThat(AppDrawerLogic.shouldShowKeyboard(false, true)).isFalse()
        assertThat(AppDrawerLogic.shouldShowKeyboard(true, false)).isFalse()
        assertThat(AppDrawerLogic.shouldShowKeyboard(true, true)).isTrue()
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
