package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeIconGestureLogicTest {
    @Test
    fun longPressDoesNotDragUntilFingerMovesPastSlop() {
        assertThat(HomeIconGestureLogic.shouldStartDrag(true, false, 4f, 4f, 16f)).isFalse()
        assertThat(HomeIconGestureLogic.shouldStartDrag(true, false, 20f, 0f, 16f)).isTrue()
        assertThat(HomeIconGestureLogic.shouldStartDrag(false, false, 40f, 0f, 16f)).isFalse()
        assertThat(HomeIconGestureLogic.shouldStartDrag(true, true, 40f, 0f, 16f)).isFalse()
    }
}
