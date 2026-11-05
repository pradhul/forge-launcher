package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppShortcutLogicTest {
    private fun s(id: String, label: String = id) =
        AppShortcut(id, "com.android.chrome", 0L, label)

    @Test
    fun ringStartsWithAppInfoHomeUninstallThenShortcuts() {
        val options = AppShortcutLogic.ringOptions(
            listOf(s("private", "Open private window"), s("tab", "New tab"))
        )
        assertThat(options).hasSize(5)
        assertThat(options[0]).isEqualTo(RingOption.Builtin(RingAction.APP_INFO))
        assertThat(options[1]).isEqualTo(RingOption.Builtin(RingAction.HOME))
        assertThat(options[2]).isEqualTo(RingOption.Builtin(RingAction.UNINSTALL))
        assertThat((options[3] as RingOption.Shortcut).shortcut.label).isEqualTo("Open private window")
        assertThat(AppShortcutLogic.label(options[1], pinned = false)).isEqualTo("Add to home")
        assertThat(AppShortcutLogic.label(options[1], pinned = true)).isEqualTo("Remove from home")
        assertThat(AppShortcutLogic.glyph(options[3], false)).isEqualTo("O")
    }

    @Test
    fun capsBlankAndDuplicateShortcuts() {
        val tooMany = (1..8).map { s("id$it", "S$it") }
        assertThat(AppShortcutLogic.visible(tooMany)).hasSize(AppShortcutLogic.MAX)
        assertThat(AppShortcutLogic.visible(listOf(s("a", ""), s("a", "A"), s("a", "A2"))))
            .containsExactly(s("a", "A"))
        assertThat(AppShortcutLogic.ringOptions()).isEqualTo(AppShortcutLogic.BUILTINS)
        assertThat(RingMenuLogic.HINT).isEqualTo("Drag to select")
    }

    @Test
    fun layoutFitsBuiltinsPlusShortcuts() {
        val items = AppShortcutLogic.ringOptions(listOf(s("p", "Private"), s("n", "New tab")))
        val l = RingMenuLogic.layout(540f, 1200f, 360f, 1080f, 2400f, 130f, 410f, items = items)
        assertThat(l.options).hasSize(5)
        assertThat(l.hint.x).isGreaterThan(0f)
        val private = l.options.last().second
        assertThat(
            RingMenuLogic.hitTest(l, 540f, 1200f, private.x, private.y, 140f)
        ).isEqualTo(RingHit.Option(items.last()))
    }
}
