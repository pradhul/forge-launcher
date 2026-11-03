package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrawerGestureLogicTest {
    private val d = 2.75f

    @Test
    fun singleShortSwipeDownAtTopClosesDuringMove() {
        assertThat(DrawerGestureLogic.shouldCloseOnMove(40f * d, 5f, startedAtTop = true, density = d)).isTrue()
        assertThat(DrawerGestureLogic.shouldCloseOnMove(20f * d, 0f, startedAtTop = true, density = d)).isFalse()
    }

    @Test
    fun swipeDownWhenScrolledDoesNotClose() {
        assertThat(DrawerGestureLogic.shouldCloseOnMove(200f * d, 0f, startedAtTop = false, density = d)).isFalse()
        assertThat(DrawerGestureLogic.shouldCloseOnRelease(200f * d, 0f, 5000f, startedAtTop = false, density = d)).isFalse()
    }

    @Test
    fun quickFlickDownClosesOnRelease() {
        assertThat(DrawerGestureLogic.shouldCloseOnRelease(15f * d, 2f, 1500f * d, startedAtTop = true, density = d)).isTrue()
        assertThat(DrawerGestureLogic.shouldCloseOnRelease(15f * d, 2f, 100f * d, startedAtTop = true, density = d)).isFalse()
    }

    @Test
    fun horizontalSwipeIgnored() {
        assertThat(DrawerGestureLogic.shouldCloseOnMove(60f * d, 80f * d, true, d)).isFalse()
        assertThat(DrawerGestureLogic.shouldOpenOnMove(-60f * d, 80f * d, d)).isFalse()
    }

    @Test
    fun swipeUpOnHomeOpensDuringMoveOrFlick() {
        assertThat(DrawerGestureLogic.shouldOpenOnMove(-40f * d, 0f, d)).isTrue()
        assertThat(DrawerGestureLogic.shouldOpenOnMove(40f * d, 0f, d)).isFalse()
        assertThat(DrawerGestureLogic.shouldOpenOnRelease(-12f * d, 0f, -1200f * d, d)).isTrue()
    }

    @Test
    fun dockHasFourSlotsFromFavourites() {
        val favs = listOf("a", "b", "c", "d", "e")
        assertThat(DockLogic.slots(favs)).containsExactly("a", "b", "c", "d").inOrder()
        assertThat(DockLogic.slots(listOf("a"))).containsExactly("a")
        assertThat(DockLogic.canAdd(3)).isTrue()
        assertThat(DockLogic.canAdd(4)).isFalse()
    }
}
