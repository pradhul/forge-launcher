package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.models.HomeApp
import org.junit.Test

class HomeGridLogicTest {
    private fun app(name: String, index: Int) = HomeApp(name, "pkg.$name", "Act", index, null, 0L)

    @Test
    fun bottomRowIsJustMoreSlots() {
        val six = (0 until 6).map { app("a$it", it) }
        assertThat(HomeGridLogic.apps(six)).hasSize(6)
        assertThat(HomeGridLogic.COLUMNS).isEqualTo(4)
        assertThat(HomeGridLogic.canAdd(4)).isTrue()
        assertThat(HomeGridLogic.canAdd(HomeGridLogic.MAX - 1)).isTrue()
        assertThat(HomeGridLogic.canAdd(HomeGridLogic.MAX)).isFalse()
    }

    @Test
    fun reorderMovesItemAndRewritesIndices() {
        val apps = listOf(app("a", 0), app("b", 1), app("c", 2), app("d", 3))
        val moved = HomeGridLogic.reorder(apps, 0, 2)
        assertThat(moved.map { it.appName }).containsExactly("b", "c", "a", "d").inOrder()
        assertThat(moved.map { it.sortingIndex }).containsExactly(0, 1, 2, 3).inOrder()
        assertThat(HomeGridLogic.reorder(apps, 1, 1)).isEqualTo(apps)
    }
}
