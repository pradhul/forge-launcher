package com.sduduzog.slimlauncher.wallpaper

import kotlin.math.roundToInt

object LauncherFadeLogic {
    const val DEFAULT_HOME_FADE = 100
    const val DEFAULT_DRAWER_BLUR = DrawerBlurLogic.DEFAULT_BLUR_PERCENT
    const val MAX_BLUR_DP = DrawerBlurLogic.MAX_BLUR_DP

    fun clampPercent(value: Int): Int = value.coerceIn(0, 100)

    fun scaleAlpha(baseAlpha: Int, percent: Int): Int {
        val scaled = baseAlpha * clampPercent(percent) / 100f
        return scaled.roundToInt().coerceIn(0, 255)
    }

    fun blurRadiusDp(percent: Int): Float {
        return DrawerBlurLogic.blurRadiusPx(percent, density = 1f)
    }
}

data class ChromeFadePrefs(
    val homeFadePercent: Int = LauncherFadeLogic.DEFAULT_HOME_FADE,
    val drawerBlurPercent: Int = LauncherFadeLogic.DEFAULT_DRAWER_BLUR
)
