package com.sduduzog.slimlauncher.ui.home

import android.view.View
import com.sduduzog.slimlauncher.wallpaper.DrawerMotionLogic

/**
 * In-activity drawer overlay + IME rules.
 *
 * The drawer lives in the home activity window (softInputMode adjustResize).
 * There is no second Dialog/window. Overlay [View.GONE] when closed so a
 * full-screen ComposeView cannot eat home touches. IME is requested only
 * after the open animation, never awaited.
 */
object DrawerWindowLogic {
    fun shouldRequestIme(
        settingOn: Boolean,
        drawerVisible: Boolean,
        animationSettled: Boolean
    ): Boolean = settingOn && drawerVisible && animationSettled

    fun shouldHideIme(drawerVisible: Boolean): Boolean = !drawerVisible

    fun shouldRefreshInstalledOnOpen(): Boolean = false

    fun shouldRefreshBackdropOnOpen(): Boolean = false

    fun shouldHideHomeChrome(drawerOpen: Boolean): Boolean = drawerOpen

    fun shouldApplyActivityBlur(drawerOpen: Boolean, percent: Int): Boolean {
        return drawerOpen && percent > 0
    }

    fun overlayViewVisible(drawerOpen: Boolean, closeAnimationPending: Boolean): Boolean {
        return drawerOpen || closeAnimationPending
    }

    fun overlayVisibility(visible: Boolean): Int {
        return if (visible) View.VISIBLE else View.GONE
    }

    fun homeChromeVisibility(visible: Boolean): Int {
        return if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun shouldConsumeCloseScroll(
        availableY: Float,
        gridAtTop: Boolean,
        accumulatedDown: Float
    ): Boolean {
        if (!gridAtTop || availableY <= 0f) return false
        return accumulatedDown + availableY > DrawerMotionLogic.CLOSE_DISTANCE
    }
}
