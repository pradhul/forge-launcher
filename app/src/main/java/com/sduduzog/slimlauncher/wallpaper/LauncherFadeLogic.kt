package com.sduduzog.slimlauncher.wallpaper

import kotlin.math.roundToInt

object LauncherFadeLogic {
    const val DEFAULT_DRAWER_FADE = 100
    const val DEFAULT_HOME_FADE = 100
    const val DEFAULT_DRAWER_BLUR = 50
    const val MAX_BLUR_DP = 24f
    const val BLUR_MIN_SDK = 31

    fun clampPercent(value: Int): Int = value.coerceIn(0, 100)

    fun scaleAlpha(baseAlpha: Int, percent: Int): Int {
        val scaled = baseAlpha * clampPercent(percent) / 100f
        return scaled.roundToInt().coerceIn(0, 255)
    }

    fun blurRadiusDp(percent: Int): Float {
        return MAX_BLUR_DP * clampPercent(percent) / 100f
    }

    fun blurSupported(sdkInt: Int): Boolean = sdkInt >= BLUR_MIN_SDK
}

data class ChromeFadePrefs(
    val drawerFadePercent: Int = LauncherFadeLogic.DEFAULT_DRAWER_FADE,
    val homeFadePercent: Int = LauncherFadeLogic.DEFAULT_HOME_FADE,
    val drawerBlurPercent: Int = LauncherFadeLogic.DEFAULT_DRAWER_BLUR
)
