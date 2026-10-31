package com.sduduzog.slimlauncher.wallpaper

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager

/**
 * Configures a *separate* translucent window (dialog) so FLAG_BLUR_BEHIND
 * and setBackgroundBlurRadius actually composite.
 *
 * setBackgroundBlurRadius on the home activity is a no-op when the window
 * only has FLAG_SHOW_WALLPAPER (not windowIsTranslucent). Applying it on
 * the home window would also blur the launcher chrome. The drawer must
 * live in its own translucent window.
 */
object DrawerBlurWindow {
    const val TAG = "ForgeWallpaper"

    fun prepareTranslucent(window: Window) {
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.FILL)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    fun apply(
        window: Window,
        percent: Int,
        density: Float,
        crossWindowEnabled: Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT
    ) {
        val behind = DrawerBlurLogic.blurBehindRadiusPx(
            percent = percent,
            density = density,
            crossWindowEnabled = crossWindowEnabled,
            sdkInt = sdkInt
        )
        val background = DrawerBlurLogic.windowBlurRadiusPx(
            percent = percent,
            density = density,
            crossWindowEnabled = crossWindowEnabled,
            sdkInt = sdkInt
        )
        Log.i(
            TAG,
            "drawer window percent=$percent blurBehind=$behind bgBlur=$background " +
                "crossWindow=$crossWindowEnabled sdk=$sdkInt"
        )
        if (sdkInt < DrawerBlurLogic.WINDOW_BLUR_SDK) return
        applyApi31(window, behind, background)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApi31(window: Window, behind: Int, background: Int) {
        if (behind > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
        val attrs = window.attributes
        attrs.blurBehindRadius = behind
        window.attributes = attrs
        window.setBackgroundBlurRadius(background)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
