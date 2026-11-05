package com.sduduzog.slimlauncher.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Turns any launcher drawable (including AdaptiveIconDrawable) into a
 * fixed-size ARGB bitmap. ImageView/Compose will not show adaptive icons
 * that have 0 intrinsic size or that were never given bounds.
 */
object IconRasterizer {

    fun toBitmap(drawable: Drawable, size: Int): Bitmap {
        val px = size.coerceAtLeast(1)
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val src = drawable.mutate()
        if (src is BitmapDrawable && src.bitmap != null && !src.bitmap.isRecycled) {
            val bmp = src.bitmap
            if (bmp.width > 0 && bmp.height > 0) {
                val scaled = if (bmp.width == px && bmp.height == px) {
                    bmp
                } else {
                    Bitmap.createScaledBitmap(bmp, px, px, true)
                }
                canvas.drawBitmap(scaled, 0f, 0f, null)
                if (hasVisiblePixels(out)) return out
                out.eraseColor(Color.TRANSPARENT)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src is AdaptiveIconDrawable) {
            src.setBounds(0, 0, px, px)
            src.draw(canvas)
            if (hasVisiblePixels(out)) return out
            out.eraseColor(Color.TRANSPARENT)
            val inset = (px * 0.18f).toInt()
            src.background?.mutate()?.let {
                it.setBounds(0, 0, px, px)
                it.draw(canvas)
            }
            src.foreground?.mutate()?.let {
                it.setBounds(inset, inset, px - inset, px - inset)
                it.draw(canvas)
            }
            if (hasVisiblePixels(out)) return out
            out.eraseColor(Color.TRANSPARENT)
        }
        src.setBounds(0, 0, px, px)
        src.draw(canvas)
        if (hasVisiblePixels(out)) return out
        return letterTile("?", px)
    }

    fun letterTile(label: String, size: Int): Bitmap {
        val px = size.coerceAtLeast(1)
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        out.eraseColor(Color.rgb(46, 92, 166))
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = px * 0.48f
        paint.isFakeBoldText = true
        val letter = label.trim().takeLastWhile { it != '.' }
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"
        val y = px / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(letter, px / 2f, y, paint)
        return out
    }

    fun hasVisiblePixels(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false
        val stepX = (w / 8).coerceAtLeast(1)
        val stepY = (h / 8).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 8) return true
                x += stepX
            }
            y += stepY
        }
        return Color.alpha(bitmap.getPixel(w / 2, h / 2)) > 8
    }
}
