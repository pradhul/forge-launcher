package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.models.HomeApp
import org.junit.Test

class HomeGridLogicTest {
    private fun app(name: String, index: Int) = HomeApp(name, "pkg.$name", "Act", index, null, 0L)

    private fun names(slots: List<HomeSlot>): List<String> = slots.map {
        when (it) {
            is HomeSlot.App -> it.app.appName
            is HomeSlot.Empty -> "_"
        }
    }

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
    fun firstFourAppsStayOnTheVisualBottomRow() {
        val six = listOf("a", "b", "c", "d", "e", "f").mapIndexed { i, n -> app(n, i) }
        assertThat(names(HomeGridLogic.visualSlots(six)))
            .containsExactly("e", "f", "_", "_", "a", "b", "c", "d").inOrder()
        assertThat(HomeGridLogic.logicalFromVisual(HomeGridLogic.visualSlots(six)).map { it.appName })
            .containsExactly("a", "b", "c", "d", "e", "f").inOrder()
        assertThat(names(HomeGridLogic.visualSlots(listOf(app("a", 0), app("b", 1)))))
            .containsExactly("a", "b").inOrder()
        assertThat(names(HomeGridLogic.visualSlots(six.take(4))))
            .containsExactly("a", "b", "c", "d").inOrder()
    }

    @Test
    fun overflowRowsSitAboveAFullBottomRow() {
        val eight = listOf("a", "b", "c", "d", "e", "f", "g", "h").mapIndexed { i, n -> app(n, i) }
        assertThat(names(HomeGridLogic.visualSlots(eight)))
            .containsExactly("e", "f", "g", "h", "a", "b", "c", "d").inOrder()
        val nine = eight + app("i", 8)
        assertThat(names(HomeGridLogic.visualSlots(nine)))
            .containsExactly("e", "f", "g", "h", "i", "_", "_", "_", "a", "b", "c", "d").inOrder()
        assertThat(HomeGridLogic.logicalFromVisual(HomeGridLogic.visualSlots(nine)).map { it.appName })
            .containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i").inOrder()
    }

    @Test
    fun messyDragCompactsBackToBottomRowFirst() {
        val six = listOf("a", "b", "c", "d", "e", "f").mapIndexed { i, n -> app(n, i) }
        val visual = HomeGridLogic.visualSlots(six)
        val dragged = HomeGridLogic.reorder(visual, 0, 4)
        assertThat(HomeGridLogic.logicalFromVisual(dragged).map { it.appName })
            .containsExactly("e", "b", "c", "d", "f", "a").inOrder()
    }

    @Test
    fun reorderMovesSlotsAndRewritesLogicalIndices() {
        val apps = listOf(app("a", 0), app("b", 1), app("c", 2), app("d", 3))
        val slots = HomeGridLogic.visualSlots(apps)
        val moved = HomeGridLogic.reorder(slots, 0, 2)
        assertThat(names(moved)).containsExactly("b", "c", "a", "d").inOrder()
        assertThat(HomeGridLogic.withIndices(HomeGridLogic.logicalFromVisual(moved)).map { it.sortingIndex })
            .containsExactly(0, 1, 2, 3).inOrder()
        assertThat(HomeGridLogic.reorder(slots, 1, 1)).isEqualTo(slots)
    }
}
