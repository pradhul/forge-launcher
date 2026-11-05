package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppShortcutLogicTest {
    private fun s(id: String, label: String = id) =
        AppShortcut(id, "com.android.chrome", 0L, label)

    @Test
    fun capsBlankAndDuplicateShortcuts() {
        val tooMany = (1..8).map { s("id$it", "S$it") }
        assertThat(AppShortcutLogic.visible(tooMany)).hasSize(AppShortcutLogic.MAX)
        assertThat(AppShortcutLogic.visible(listOf(s("a", ""), s("a", "A"), s("a", "A2"))))
            .containsExactly(s("a", "A"))
    }
}
