package com.sduduzog.slimlauncher.wallpaper

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import android.view.Window
import android.view.WindowManager

/**
 * Applies FLAG_BLUR_BEHIND on the *home activity* window while the drawer
 * is open. No second window. Radius is 0 when the drawer is closed.
 *
 * Guarded by [android.view.WindowManager.isCrossWindowBlurEnabled]. When
 * that is false, the overlay uses a cached Bing bitmap or a tinted scrim.
 */
object DrawerBlurWindow {
    const val TAG = "ForgeWallpaper"

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
        Log.i(
            TAG,
            "activity blur percent=$percent blurBehind=$behind " +
                "crossWindow=$crossWindowEnabled sdk=$sdkInt"
        )
        if (sdkInt < DrawerBlurLogic.WINDOW_BLUR_SDK) return
        applyApi31(window, behind)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApi31(window: Window, behind: Int) {
        if (behind > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
        val attrs = window.attributes
        attrs.blurBehindRadius = behind
        window.attributes = attrs
    }
}
