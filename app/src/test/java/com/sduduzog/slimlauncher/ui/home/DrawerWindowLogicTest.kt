package com.sduduzog.slimlauncher.ui.home

import android.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrawerWindowLogicTest {

    @Test
    fun imeWaitsForOpenAnimationNotWindowFocus() {
        assertThat(
            DrawerWindowLogic.shouldRequestIme(true, true, animationSettled = false)
        ).isFalse()
        assertThat(
            DrawerWindowLogic.shouldRequestIme(true, true, animationSettled = true)
        ).isTrue()
        assertThat(
            DrawerWindowLogic.shouldRequestIme(true, false, animationSettled = true)
        ).isFalse()
        assertThat(
            DrawerWindowLogic.shouldRequestIme(false, true, animationSettled = true)
        ).isFalse()
    }

    @Test
    fun imeHidesWhenDrawerIsNotVisible() {
        assertThat(DrawerWindowLogic.shouldHideIme(drawerVisible = true)).isFalse()
        assertThat(DrawerWindowLogic.shouldHideIme(drawerVisible = false)).isTrue()
    }

    @Test
    fun openPathRendersFromCache() {
        assertThat(DrawerWindowLogic.shouldRefreshInstalledOnOpen()).isFalse()
        assertThat(DrawerWindowLogic.shouldRefreshBackdropOnOpen()).isFalse()
    }

    @Test
    fun overlayIsGoneWhenClosedAndVisibleWhileOpenOrClosing() {
        assertThat(DrawerWindowLogic.overlayViewVisible(true, false)).isTrue()
        assertThat(DrawerWindowLogic.overlayViewVisible(false, true)).isTrue()
        assertThat(DrawerWindowLogic.overlayViewVisible(false, false)).isFalse()
        assertThat(DrawerWindowLogic.overlayVisibility(true)).isEqualTo(View.VISIBLE)
        assertThat(DrawerWindowLogic.overlayVisibility(false)).isEqualTo(View.GONE)
    }

    @Test
    fun homeChromeHidesWhileDrawerOpen() {
        assertThat(DrawerWindowLogic.shouldHideHomeChrome(true)).isTrue()
        assertThat(DrawerWindowLogic.shouldHideHomeChrome(false)).isFalse()
        assertThat(DrawerWindowLogic.homeChromeVisibility(true)).isEqualTo(View.VISIBLE)
        assertThat(DrawerWindowLogic.homeChromeVisibility(false)).isEqualTo(View.INVISIBLE)
    }

    @Test
    fun activityBlurOnlyWhileOpenAndSliderAboveZero() {
        assertThat(DrawerWindowLogic.shouldApplyActivityBlur(true, 50)).isTrue()
        assertThat(DrawerWindowLogic.shouldApplyActivityBlur(true, 0)).isFalse()
        assertThat(DrawerWindowLogic.shouldApplyActivityBlur(false, 80)).isFalse()
    }

    @Test
    fun nestedScrollDoesNotConsumeSmallDownwardMoves() {
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(30f, gridAtTop = true, accumulatedDown = 0f)
        ).isFalse()
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(40f, gridAtTop = true, accumulatedDown = 50f)
        ).isTrue()
        assertThat(
            DrawerWindowLogic.shouldConsumeCloseScroll(200f, gridAtTop = false, accumulatedDown = 0f)
        ).isFalse()
    }
}
