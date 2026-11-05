package com.sduduzog.slimlauncher.icons

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Dev-only preview: when FORGE_PREVIEW_IN is set, reads raw big-endian ARGB
 * square icons (*.argb, size N = sqrt(ints)) and writes themed versions
 * (light + dark palette) next to them as *.light.out / *.dark.out.
 */
class ThemedIconPreviewRender {
    @Test
    fun renderPreview() {
        val inDir = System.getenv("FORGE_PREVIEW_IN")
        assumeTrue(inDir != null)
        val files = File(inDir!!).listFiles { f -> f.name.endsWith(".argb") }!!
        for (f in files) {
            val bytes = f.readBytes()
            val count = bytes.size / 4
            val px = kotlin.math.sqrt(count.toDouble()).toInt()
            val pixels = IntArray(count)
            DataInputStream(bytes.inputStream()).use { s -> for (i in 0 until count) pixels[i] = s.readInt() }
            val shape = IconPostProcess.applyShapeMask(IntArray(count) { -1 }, px, px, BackplateShape.CIRCLE)
                .map { IconPostProcess.alpha(it) }.toIntArray()
            for ((name, seed, dark) in listOf(Triple("light", 0xFF4A7FB0.toInt(), false), Triple("dark", 0xFF8E6BB5.toInt(), true))) {
                val colors = ThemedIconPalette.colors(seed, dark)
                val g = GlyphExtractor.fromBitmap(pixels, px, px)
                val (d, l) = ThemedIconPalette.duotoneEnds(colors)
                val out = if (g.ok) GlyphExtractor.compose(g.alpha, shape, colors.background, colors.foreground)
                else GlyphExtractor.duotone(pixels, shape, d, l)
                DataOutputStream(File(f.path + ".$name.out").outputStream()).use { s -> out.forEach { s.writeInt(it) } }
                File(f.path + ".$name.txt").writeText("ok=${g.ok} cov=${g.coverage} noise=${g.noise} inv=${g.inverted}")
            }
        }
    }
}
