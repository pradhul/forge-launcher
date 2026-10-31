package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperSnapshotter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)
    @Volatile
    private var source: Bitmap? = null
    @Volatile
    private var cachedPercent: Int? = null
    @Volatile
    private var cachedBlurred: Bitmap? = null

    @Synchronized
    fun invalidate() {
        source = null
        cachedPercent = null
        cachedBlurred = null
    }

    @Synchronized
    fun blurred(percent: Int): Bitmap? {
        val src = source ?: capture()?.also { source = it } ?: return null
        val clamped = DrawerBlurLogic.clampPercent(percent)
        cachedBlurred?.let { ready ->
            if (cachedPercent == clamped) return ready
        }
        val prepared = prepare(src, clamped)
        cachedPercent = clamped
        cachedBlurred = prepared
        return prepared
    }

    fun capture(): Bitmap? {
        val drawable = runCatching { wallpaperManager.drawable }.getOrNull()
            ?: runCatching { wallpaperManager.peekDrawable() }.getOrNull()
            ?: return null
        return drawableToBitmap(drawable)
    }

    companion object {
        fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                return downsample(drawable.bitmap)
            }
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(full)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            return downsample(full)
        }

        fun prepare(source: Bitmap, percent: Int): Bitmap {
            val small = downsample(source)
            val radius = DrawerBlurLogic.sampleBlurRadius(percent)
            if (radius <= 0) return small
            val pixels = IntArray(small.width * small.height)
            small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
            val blurred = BoxBlur.blur(pixels, small.width, small.height, radius)
            val out = Bitmap.createBitmap(small.width, small.height, Bitmap.Config.ARGB_8888)
            out.setPixels(blurred, 0, small.width, 0, 0, small.width, small.height)
            return out
        }

        fun downsample(source: Bitmap): Bitmap {
            val (w, h) = DrawerBlurLogic.downsampleSize(source.width, source.height)
            if (source.width == w && source.height == h) return source
            return Bitmap.createScaledBitmap(source, w, h, true)
        }
    }
}
