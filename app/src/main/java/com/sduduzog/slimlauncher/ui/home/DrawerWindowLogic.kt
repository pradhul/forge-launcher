package com.sduduzog.slimlauncher.ui.home

import android.os.Build
import android.view.WindowManager
import com.sduduzog.slimlauncher.wallpaper.DrawerMotionLogic

/**
 * Drawer dialog window + IME rules.
 *
 * A full-screen Dialog defaults to FLAG_ALT_FOCUSABLE_IM, which sends the
 * IME to the activity behind the drawer. Combined with requesting
 * showSoftInput before the dialog has window focus (and hide() leaving
 * isShowing=true with a GONE decor), the search field never gets a
 * keyboard and the drawer window can sit on top unresponsive.
 */
object DrawerWindowLogic {
    const val FLAG_NOT_FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    const val FLAG_NOT_TOUCHABLE = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    const val FLAG_ALT_FOCUSABLE_IM = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
    const val FLAG_LAYOUT_IN_SCREEN = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    const val FLAG_LAYOUT_NO_LIMITS = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    const val FLAG_DIM_BEHIND = WindowManager.LayoutParams.FLAG_DIM_BEHIND

    const val SOFT_INPUT_STATE_ALWAYS_VISIBLE =
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
    const val SOFT_INPUT_STATE_HIDDEN = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
    @Suppress("DEPRECATION")
    const val SOFT_INPUT_ADJUST_RESIZE = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    const val SOFT_INPUT_MASK_STATE = WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
    const val SOFT_INPUT_MASK_ADJUST = WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST

    const val BLOCKING_FLAGS =
        FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE or FLAG_ALT_FOCUSABLE_IM or FLAG_LAYOUT_NO_LIMITS

    fun shouldRequestIme(
        settingOn: Boolean,
        drawerVisible: Boolean,
        windowHasFocus: Boolean
    ): Boolean = settingOn && drawerVisible && windowHasFocus

    fun shouldHideIme(drawerVisible: Boolean): Boolean = !drawerVisible

    fun softInputMode(keyboardOnOpen: Boolean): Int {
        val state = if (keyboardOnOpen) {
            SOFT_INPUT_STATE_ALWAYS_VISIBLE
        } else {
            SOFT_INPUT_STATE_HIDDEN
        }
        return state or SOFT_INPUT_ADJUST_RESIZE
    }

    fun flagsAfterPrepare(currentFlags: Int): Int {
        var flags = currentFlags
        flags = flags and BLOCKING_FLAGS.inv()
        flags = flags and FLAG_DIM_BEHIND.inv()
        flags = flags or FLAG_LAYOUT_IN_SCREEN
        return flags
    }

    fun shouldCallShow(isShowing: Boolean, decorVisible: Boolean): Boolean {
        return !isShowing || !decorVisible
    }

    fun shouldDismissAfterClose(drawerOpen: Boolean, animationFinished: Boolean): Boolean {
        return !drawerOpen && animationFinished
    }

    fun shouldConsumeCloseScroll(
        availableY: Float,
        gridAtTop: Boolean,
        accumulatedDown: Float
    ): Boolean {
        if (!gridAtTop || availableY <= 0f) return false
        return accumulatedDown + availableY > DrawerMotionLogic.CLOSE_DISTANCE
    }

    fun imeInsetsType(): Int {
        return if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsets.Type.ime()
        } else {
            0
        }
    }
}
