package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppPopupLogicTest {
    private fun s(id: String, label: String) = AppShortcut(id, "com.android.chrome", 0L, label)

    @Test
    fun shortcutsComeFirstThenInfoHomeUninstall() {
        val items = AppPopupLogic.items(
            listOf(s("private", "Open private window"), s("tab", "New tab")),
            onHome = false,
            canUninstall = true
        )
        assertThat(items.map { it.label }).containsExactly(
            "Open private window", "New tab", "App info", "Add to home", "Uninstall"
        ).inOrder()
        assertThat(items[0].icon).isEqualTo(AppPopupIcon.PRIVATE)
        assertThat(items[1].icon).isEqualTo(AppPopupIcon.NEW_TAB)
        assertThat(items[3].action).isEqualTo(AppPopupAction.Home)
        assertThat(items.none { it.label.equals("Cancel", ignoreCase = true) }).isTrue()
    }

    @Test
    fun homeLabelFollowsRealMembershipAndHidesUninstallWhenBlocked() {
        val add = AppPopupLogic.items(emptyList(), onHome = false, canUninstall = false)
        assertThat(add.map { it.label }).containsExactly("App info", "Add to home").inOrder()
        val remove = AppPopupLogic.items(emptyList(), onHome = true, canUninstall = true)
        assertThat(remove.map { it.label }).containsExactly("App info", "Remove from home", "Uninstall").inOrder()
    }

    @Test
    fun systemAppsCannotBeUninstalled() {
        assertThat(AppPopupLogic.canUninstall(0)).isTrue()
        assertThat(AppPopupLogic.canUninstall(AppPopupLogic.FLAG_SYSTEM)).isFalse()
        assertThat(AppPopupLogic.canUninstall(AppPopupLogic.FLAG_SYSTEM or 128)).isFalse()
    }

    @Test
    fun popupSitsAboveIconThenClampsOnScreen() {
        val (x, y) = AppPopupLogic.place(
            anchorX = 200f, anchorY = 400f,
            popupW = 248f, popupH = 240f,
            screenW = 400f, screenH = 800f,
            margin = 16f, gap = 10f
        )
        assertThat(x).isEqualTo(76f)
        assertThat(y).isEqualTo(150f)
        val top = AppPopupLogic.place(200f, 40f, 248f, 240f, 400f, 800f, 16f, 10f)
        assertThat(top.second).isAtLeast(16f)
        val edge = AppPopupLogic.place(10f, 700f, 248f, 240f, 400f, 800f, 16f, 10f)
        assertThat(edge.first).isEqualTo(16f)
    }
}
