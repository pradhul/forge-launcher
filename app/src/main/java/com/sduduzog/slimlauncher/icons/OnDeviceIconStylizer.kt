package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceIconStylizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var engine: StyleTransferEngine? = null
    @Volatile
    private var engineFailed = false

    private fun engine(): StyleTransferEngine? {
        if (engineFailed) return null
        synchronized(this) {
            engine?.let { return it }
            if (engineFailed) return null
            return try {
                StyleTransferEngine(context).also { engine = it }
            } catch (error: Exception) {
                engineFailed = true
                Log.w(TAG, "Could not load style-transfer models", error)
                null
            }
        }
    }

    fun loadPresetStyle(preset: StylePreset): Bitmap {
        if (preset.assetPath.isBlank()) {
            return Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        }
        context.assets.open(preset.assetPath).use { stream ->
            return BitmapFactory.decodeStream(stream)
                ?: Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        }
    }

    fun loadStyleBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    }

    fun stylizeIcon(
        source: Drawable,
        style: Bitmap,
        preset: StylePreset,
        strength: Float
    ): ByteArray {
        val plate = compositeOnBackplate(source, ICON_SIZE, preset.backplateColor)
        val stylized = runStyleTransfer(plate, style) ?: plate
        val blended = blendBitmaps(plate, stylized, strength.coerceIn(0f, 1f))
        val finished = postProcessBitmap(blended, preset, 1f)
        return ImageUtils.encodePng(finished)
    }

    fun predictStyleBottleneck(style: Bitmap): FloatArray? {
        val loaded = engine() ?: return null
        return try {
            loaded.predictStyle(style)
        } catch (error: Exception) {
            Log.w(TAG, "Style predict failed", error)
            null
        }
    }

    fun styleWithoutTransfer(source: Drawable, preset: StylePreset): ByteArray {
        val plate = compositeOnBackplate(source, ICON_SIZE, preset.backplateColor)
        val finished = postProcessBitmap(plate, preset, 1f)
        return ImageUtils.encodePng(finished)
    }

    fun stylizeWithBottleneck(
        source: Drawable,
        bottleneck: FloatArray,
        preset: StylePreset,
        strength: Float
    ): ByteArray {
        val plate = compositeOnBackplate(source, ICON_SIZE, preset.backplateColor)
        val stylized = try {
            engine()?.transform(plate, bottleneck) ?: plate
        } catch (error: Exception) {
            Log.w(TAG, "Style transform failed, using post-process only", error)
            plate
        }
        val blended = blendBitmaps(plate, stylized, strength.coerceIn(0f, 1f))
        val finished = postProcessBitmap(blended, preset, 1f)
        return ImageUtils.encodePng(finished)
    }

    private fun runStyleTransfer(content: Bitmap, style: Bitmap): Bitmap? {
        val loaded = engine() ?: return null
        return try {
            val bottleneck = loaded.predictStyle(style)
            loaded.transform(content, bottleneck)
        } catch (error: Exception) {
            Log.w(TAG, "Style transfer failed, using post-process only", error)
            null
        }
    }

    fun compositeOnBackplate(drawable: Drawable, size: Int, backplateColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backplateColor)
        val inset = (size * 0.12f).toInt()
        val bounds = Rect(inset, inset, size - inset, size - inset)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val fg = drawable.foreground
            val bg = drawable.background
            bg?.setBounds(0, 0, size, size)
            bg?.draw(canvas)
            val safe = (size * 0.18f).toInt()
            fg?.setBounds(safe, safe, size - safe, size - safe)
            fg?.draw(canvas)
        } else if (drawable is BitmapDrawable && drawable.bitmap != null) {
            canvas.drawBitmap(drawable.bitmap, null, bounds, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            drawable.setBounds(bounds)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun blendBitmaps(original: Bitmap, stylized: Bitmap, amount: Float): Bitmap {
        val w = original.width
        val h = original.height
        val scaled = if (stylized.width == w && stylized.height == h) {
            stylized
        } else {
            Bitmap.createScaledBitmap(stylized, w, h, true)
        }
        val a = IntArray(w * h)
        val b = IntArray(w * h)
        original.getPixels(a, 0, w, 0, 0, w, h)
        scaled.getPixels(b, 0, w, 0, 0, w, h)
        val mixed = IconPostProcess.mix(a, b, amount)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.setPixels(mixed, 0, w, 0, 0, w, h)
        }
    }

    private fun postProcessBitmap(source: Bitmap, preset: StylePreset, strength: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val processed = IconPostProcess.apply(
            pixels = pixels,
            width = w,
            height = h,
            spec = preset.postProcess,
            shape = preset.backplate,
            strength = strength
        )
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(processed, 0, w, 0, 0, w, h)
        return out
    }

    companion object {
        private const val TAG = "OnDeviceStylizer"
        const val ICON_SIZE = 192
    }
}
