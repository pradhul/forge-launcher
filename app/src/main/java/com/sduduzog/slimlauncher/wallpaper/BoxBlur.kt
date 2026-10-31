package com.sduduzog.slimlauncher.wallpaper

/**
 * Separable box blur on packed ARGB pixels. Used to pre-blur a downscaled
 * wallpaper snapshot so the drawer backdrop is visible without GPU
 * RenderEffect / window blur.
 */
object BoxBlur {
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        require(pixels.size == width * height)
        if (radius <= 0 || width <= 0 || height <= 0) return pixels.copyOf()
        val r = radius.coerceAtLeast(1)
        val tmp = IntArray(pixels.size)
        val out = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                tmp[y * width + x] = average(pixels, width, height, x, y, r, horizontal = true)
            }
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[y * width + x] = average(tmp, width, height, x, y, r, horizontal = false)
            }
        }
        return out
    }

    private fun average(
        src: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        radius: Int,
        horizontal: Boolean
    ): Int {
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        var count = 0
        if (horizontal) {
            val yRow = y * width
            val x0 = (x - radius).coerceAtLeast(0)
            val x1 = (x + radius).coerceAtMost(width - 1)
            for (xx in x0..x1) {
                val p = src[yRow + xx]
                a += p ushr 24 and 0xFF
                r += p ushr 16 and 0xFF
                g += p ushr 8 and 0xFF
                b += p and 0xFF
                count++
            }
        } else {
            val y0 = (y - radius).coerceAtLeast(0)
            val y1 = (y + radius).coerceAtMost(height - 1)
            for (yy in y0..y1) {
                val p = src[yy * width + x]
                a += p ushr 24 and 0xFF
                r += p ushr 16 and 0xFF
                g += p ushr 8 and 0xFF
                b += p and 0xFF
                count++
            }
        }
        if (count == 0) return src[y * width + x]
        return (a / count shl 24) or (r / count shl 16) or (g / count shl 8) or (b / count)
    }
}
