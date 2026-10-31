package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.sduduzog.slimlauncher.icons.IconPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperSnapshotter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: WallpaperBitmapStore,
    private val prefs: IconPreferences
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
        if (prefs.bingWallpaperEnabled || store.exists()) {
            val cached = store.load()
            if (cached != null) {
                Log.i(TAG, WallpaperCaptureLogic.describe(
                    WallpaperCaptureLogic.Source.CACHE,
                    cached.width,
                    cached.height
                ))
                return downsample(cached)
            }
        }

        val fromDrawable = readDrawable("getDrawable") { wallpaperManager.drawable }
        if (fromDrawable != null) return fromDrawable

        val fromPeek = readDrawable("peekDrawable") { wallpaperManager.peekDrawable() }
        if (fromPeek != null) return fromPeek

        val fromFile = readWallpaperFile()
        if (fromFile != null) return fromFile

        Log.w(TAG, WallpaperCaptureLogic.describe(WallpaperCaptureLogic.Source.NONE))
        return null
    }

    private fun readDrawable(label: String, loader: () -> Drawable?): Bitmap? {
        return try {
            val drawable = loader()
            if (drawable == null) {
                Log.i(TAG, "$label returned null")
                null
            } else {
                val bitmap = drawableToBitmap(drawable)
                Log.i(
                    TAG,
                    "$label ok size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight} " +
                        "bitmap=${bitmap.width}x${bitmap.height}"
                )
                bitmap
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "$label SecurityException: ${error.message}")
            null
        } catch (error: Throwable) {
            val kind = if (WallpaperCaptureLogic.isPermissionDenied(error)) {
                "permission"
            } else {
                error.javaClass.simpleName
            }
            Log.w(TAG, "$label failed ($kind): ${error.message}")
            null
        }
    }

    private fun readWallpaperFile(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return try {
            wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                val bitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                if (bitmap == null) {
                    Log.i(TAG, "getWallpaperFile decoded null")
                    null
                } else {
                    Log.i(
                        TAG,
                        WallpaperCaptureLogic.describe(
                            WallpaperCaptureLogic.Source.FILE,
                            bitmap.width,
                            bitmap.height
                        )
                    )
                    downsample(bitmap)
                }
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "getWallpaperFile SecurityException: ${error.message}")
            null
        } catch (error: Throwable) {
            Log.w(TAG, "getWallpaperFile failed: ${error.javaClass.simpleName}: ${error.message}")
            null
        }
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

        @Suppress("UNUSED_PARAMETER")
        fun prepare(source: Bitmap, percent: Int, sdkInt: Int = Build.VERSION.SDK_INT): Bitmap {
            val small = downsample(source)
            val radius = DrawerBlurLogic.sampleBlurRadius(percent)
            if (radius <= 0) return small
            // Software Canvas ignores RenderEffect; BoxBlur is the bitmap path.
            // Live wallpaper blur uses FLAG_BLUR_BEHIND on the home activity.
            return boxBlur(small, radius)
        }

        fun boxBlur(source: Bitmap, radius: Int): Bitmap {
            val pixels = IntArray(source.width * source.height)
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
            val blurred = BoxBlur.blur(pixels, source.width, source.height, radius)
            val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            out.setPixels(blurred, 0, source.width, 0, 0, source.width, source.height)
            return out
        }

        fun downsample(source: Bitmap): Bitmap {
            val (w, h) = DrawerBlurLogic.downsampleSize(source.width, source.height)
            if (source.width == w && source.height == h) return source
            return Bitmap.createScaledBitmap(source, w, h, true)
        }

        private const val TAG = "ForgeWallpaper"
    }
}
