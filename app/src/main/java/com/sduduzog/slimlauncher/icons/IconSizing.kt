package com.sduduzog.slimlauncher.icons

import android.app.ActivityManager
import android.content.Context

/** Icon pixel size derived from the device density / launcher icon size. */
object IconSizing {
    const val MIN_PX = 96
    const val MAX_PX = 432

    fun clamp(launcherLargeIconPx: Int, densityScaled48dp: Int): Int {
        return maxOf(launcherLargeIconPx, densityScaled48dp).coerceIn(MIN_PX, MAX_PX)
    }

    @Volatile
    private var cached = 0

    fun devicePx(context: Context): Int {
        if (cached > 0) return cached
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val large = am?.launcherLargeIconSize ?: 0
        val dp48 = (48f * context.resources.displayMetrics.density * 1.5f).toInt()
        return clamp(large, dp48).also { cached = it }
    }
}
