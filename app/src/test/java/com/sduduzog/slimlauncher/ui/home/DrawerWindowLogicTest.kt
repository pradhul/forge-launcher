package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DrawerWindowLogicTest {

    @Test
    fun imeWaitsForDialogWindowFocus() {
        assertThat(DrawerWindowLogic.shouldRequestIme(true, true, windowHasFocus = false))
            .isFalse()
        assertThat(DrawerWindowLogic.shouldRequestIme(true, true, windowHasFocus = true))
            .isTrue()
        assertThat(DrawerWindowLogic.shouldRequestIme(true, false, windowHasFocus = true))
            .isFalse()
        assertThat(DrawerWindowLogic.shouldRequestIme(false, true, windowHasFocus = true))
            .isFalse()
    }

    @Test
    fun imeHidesWhenDrawerIsNotVisible() {
        assertThat(DrawerWindowLogic.shouldHideIme(drawerVisible = true)).isFalse()
        assertThat(DrawerWindowLogic.shouldHideIme(drawerVisible = false)).isTrue()
    }

    @Test
    fun keyboardOnOpenUsesAlwaysVisibleAdjustResize() {
        val mode = DrawerWindowLogic.softInputMode(keyboardOnOpen = true)
        assertThat(mode and DrawerWindowLogic.SOFT_INPUT_MASK_STATE)
            .isEqualTo(DrawerWindowLogic.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        assertThat(mode and DrawerWindowLogic.SOFT_INPUT_MASK_ADJUST)
            .isEqualTo(DrawerWindowLogic.SOFT_INPUT_ADJUST_RESIZE)
        val hidden = DrawerWindowLogic.softInputMode(keyboardOnOpen = false)
        assertThat(hidden and DrawerWindowLogic.SOFT_INPUT_MASK_STATE)
            .isEqualTo(DrawerWindowLogic.SOFT_INPUT_STATE_HIDDEN)
    }

    @Test
    fun prepareClearsImeAndTouchBlockingFlags() {
        val incoming = DrawerWindowLogic.FLAG_ALT_FOCUSABLE_IM or
            DrawerWindowLogic.FLAG_NOT_FOCUSABLE or
            DrawerWindowLogic.FLAG_NOT_TOUCHABLE or
            DrawerWindowLogic.FLAG_LAYOUT_NO_LIMITS or
            DrawerWindowLogic.FLAG_DIM_BEHIND
        val prepared = DrawerWindowLogic.flagsAfterPrepare(incoming)
        assertThat(prepared and DrawerWindowLogic.BLOCKING_FLAGS).isEqualTo(0)
        assertThat(prepared and DrawerWindowLogic.FLAG_LAYOUT_IN_SCREEN)
            .isEqualTo(DrawerWindowLogic.FLAG_LAYOUT_IN_SCREEN)
        assertThat(DrawerWindowLogic.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            .isEqualTo(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        assertThat(DrawerWindowLogic.FLAG_ALT_FOCUSABLE_IM)
            .isEqualTo(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    @Test
    fun hideWithoutDismissRequiresShowWhenDecorGone() {
        assertThat(
            DrawerWindowLogic.shouldCallShow(isShowing = true, decorVisible = false)
        ).isTrue()
        assertThat(
            DrawerWindowLogic.shouldCallShow(isShowing = true, decorVisible = true)
        ).isFalse()
        assertThat(
            DrawerWindowLogic.shouldCallShow(isShowing = false, decorVisible = false)
        ).isTrue()
    }

    @Test
    fun windowIsDismissedOnlyAfterCloseAnimation() {
        assertThat(DrawerWindowLogic.shouldDismissAfterClose(true, true)).isFalse()
        assertThat(DrawerWindowLogic.shouldDismissAfterClose(false, false)).isFalse()
        assertThat(DrawerWindowLogic.shouldDismissAfterClose(false, true)).isTrue()
    }

    @Test
    fun nestedScrollDoesNotConsumeSmallDownwardMoves() {
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(40f, gridAtTop = true, accumulatedDown = 0f)
        ).isFalse()
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(40f, gridAtTop = true, accumulatedDown = 50f)
        ).isTrue()
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(200f, gridAtTop = false, accumulatedDown = 0f)
        ).isFalse()
    }
}
