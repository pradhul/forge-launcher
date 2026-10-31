package com.sduduzog.slimlauncher.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun drawableToPng(drawable: Drawable, size: Int = 256): ByteArray {
        val bitmap = when {
            drawable is BitmapDrawable && drawable.bitmap != null -> {
                Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
            }
            else -> {
                val created = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(created)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                created
            }
        }
        return encodePng(bitmap)
    }

    fun downscaleToSquarePng(bytes: ByteArray, size: Int = 256): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val scaled = Bitmap.createScaledBitmap(decoded, size, size, true)
        return encodePng(scaled)
    }

    private fun encodePng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
