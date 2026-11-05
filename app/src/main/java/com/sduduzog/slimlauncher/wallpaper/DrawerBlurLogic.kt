package com.sduduzog.slimlauncher.wallpaper

import kotlin.math.roundToInt

/**
 * Drawer blur without a second window.
 *
 * Paths, in order:
 * 1. FLAG_BLUR_BEHIND on the home activity while the drawer is open
 *    (API 31+, isCrossWindowBlurEnabled). Home chrome is hidden.
 * 2. Cached Bing / Forge-set wallpaper bitmap, BoxBlur / Modifier.blur.
 * 3. WallpaperColors / dynamic-palette tinted frosted scrim.
 */
object DrawerBlurLogic {
    const val DEFAULT_BLUR_PERCENT = 50
    const val MAX_BLUR_DP = 24f
    const val MAX_BLUR_BEHIND_DP = 40f
    const val DOWNSAMPLE_MAX_EDGE = 96
    const val MIN_TINT = 0.08f
    const val MAX_TINT = 0.36f
    const val MIN_FALLBACK_SCRIM = 0.18f
    const val MAX_FALLBACK_SCRIM = 0.55f
    const val WINDOW_BLUR_SDK = 31
    const val ANIM_MS = 90

    enum class BackdropMode { LIVE_WINDOW, BITMAP, COLOR_SCRIM }

    fun clampPercent(value: Int): Int = value.coerceIn(0, 100)

    fun tintAlpha(blurPercent: Int): Float {
        val t = clampPercent(blurPercent) / 100f
        return MIN_TINT + (MAX_TINT - MIN_TINT) * t
    }

    fun tintArgb(seed: Int, blurPercent: Int): Int {
        val alpha = (tintAlpha(blurPercent) * 255f).roundToInt().coerceIn(0, 255)
        return (seed and 0x00FFFFFF) or (alpha shl 24)
    }

    fun blurRadiusPx(percent: Int, density: Float): Float {
        return MAX_BLUR_DP * density.coerceAtLeast(0.5f) * clampPercent(percent) / 100f
    }

    fun sampleBlurRadius(percent: Int): Int {
        return ((DOWNSAMPLE_MAX_EDGE / 8f) * clampPercent(percent) / 100f).roundToInt()
            .coerceAtLeast(if (percent > 0) 1 else 0)
    }

    fun downsampleSize(width: Int, height: Int, maxEdge: Int = DOWNSAMPLE_MAX_EDGE): Pair<Int, Int> {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val longest = maxOf(w, h).toFloat()
        if (longest <= maxEdge) return w to h
        val scale = maxEdge / longest
        return (w * scale).toInt().coerceAtLeast(1) to (h * scale).toInt().coerceAtLeast(1)
    }

    fun shouldUseWindowBlur(sdkInt: Int, crossWindowEnabled: Boolean): Boolean {
        return sdkInt >= WINDOW_BLUR_SDK && crossWindowEnabled
    }

    fun windowBlurRadiusPx(percent: Int, density: Float, crossWindowEnabled: Boolean, sdkInt: Int): Int {
        if (!shouldUseWindowBlur(sdkInt, crossWindowEnabled)) return 0
        return blurRadiusPx(percent, density).roundToInt().coerceAtLeast(if (percent > 0) 1 else 0)
    }

    fun needsBitmapFallback(sdkInt: Int, crossWindowEnabled: Boolean): Boolean {
        return !shouldUseWindowBlur(sdkInt, crossWindowEnabled)
    }

    fun blurBehindRadiusPx(percent: Int, density: Float, crossWindowEnabled: Boolean, sdkInt: Int): Int {
        if (!shouldUseWindowBlur(sdkInt, crossWindowEnabled)) return 0
        val clamped = clampPercent(percent)
        if (clamped <= 0) return 0
        return (MAX_BLUR_BEHIND_DP * density.coerceAtLeast(0.5f) * clamped / 100f)
            .roundToInt()
            .coerceAtLeast(1)
    }

    fun fallbackScrimAlpha(blurPercent: Int): Float {
        val t = clampPercent(blurPercent) / 100f
        return MIN_FALLBACK_SCRIM + (MAX_FALLBACK_SCRIM - MIN_FALLBACK_SCRIM) * t
    }

    fun fallbackScrimArgb(seed: Int, blurPercent: Int): Int {
        val alpha = (fallbackScrimAlpha(blurPercent) * 255f).roundToInt().coerceIn(0, 255)
        return (seed and 0x00FFFFFF) or (alpha shl 24)
    }

    fun backdropMode(
        sdkInt: Int,
        crossWindowEnabled: Boolean,
        hasBitmap: Boolean
    ): BackdropMode {
        if (shouldUseWindowBlur(sdkInt, crossWindowEnabled)) return BackdropMode.LIVE_WINDOW
        if (hasBitmap) return BackdropMode.BITMAP
        return BackdropMode.COLOR_SCRIM
    }

    fun shouldApplyComposeBlur(hasPreblurredBitmap: Boolean): Boolean {
        return !hasPreblurredBitmap
    }

    fun shouldDrawBitmapBackdrop(mode: BackdropMode, hasBitmap: Boolean, percent: Int): Boolean {
        if (!hasBitmap || clampPercent(percent) <= 0) return false
        // Draw the cached bitmap whenever we have one, even if live window
        // blur is reported on — isCrossWindowBlurEnabled can be true while
        // FLAG_BLUR_BEHIND is still a no-op (emulator / battery saver).
        return mode != BackdropMode.COLOR_SCRIM || hasBitmap
    }
}

object DrawerMotionLogic {
    const val ANIM_MS = DrawerBlurLogic.ANIM_MS
    const val OPEN_DISTANCE = 36f
    const val CLOSE_DISTANCE = 36f

    fun shouldOpenFromHomeSwipe(upDistance: Float, velocityX: Float, velocityY: Float): Boolean {
        return upDistance > OPEN_DISTANCE && kotlin.math.abs(velocityY) > kotlin.math.abs(velocityX)
    }

    fun shouldCloseFromDrawerSwipe(downDistance: Float, gridAtTop: Boolean): Boolean {
        return gridAtTop && downDistance > CLOSE_DISTANCE
    }

    fun backClosesDrawerOnly(drawerOpen: Boolean): Boolean = drawerOpen
}
