package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class IconRasterizerTest {

    @Test
    fun colorDrawableProducesVisibleNonZeroBitmap() {
        val drawable = ColorDrawable(Color.RED)
        val bitmap = IconRasterizer.toBitmap(drawable, 96)
        assertThat(bitmap.width).isEqualTo(96)
        assertThat(bitmap.height).isEqualTo(96)
        assertThat(IconRasterizer.hasVisiblePixels(bitmap)).isTrue()
        assertThat(Color.red(bitmap.getPixel(48, 48))).isEqualTo(255)
        assertThat(Color.alpha(bitmap.getPixel(48, 48))).isEqualTo(255)
    }

    @Test
    fun applicationIconRasterizesWithVisiblePixels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val icon = context.getDrawable(android.R.drawable.sym_def_app_icon)
        assertThat(icon).isNotNull()
        val bitmap = IconRasterizer.toBitmap(icon!!, 128)
        assertThat(bitmap.width).isEqualTo(128)
        assertThat(bitmap.height).isEqualTo(128)
        assertThat(IconRasterizer.hasVisiblePixels(bitmap)).isTrue()
    }

    @Test
    fun adaptiveIconWithZeroIntrinsicSizeRasterizes() {
        val adaptive = AdaptiveIconDrawable(
            ColorDrawable(Color.BLUE),
            ColorDrawable(Color.YELLOW)
        )
        assertThat(adaptive.intrinsicWidth).isLessThan(1)
        val bitmap = IconRasterizer.toBitmap(adaptive, 96)
        assertThat(bitmap.width).isEqualTo(96)
        assertThat(bitmap.height).isEqualTo(96)
        assertThat(IconRasterizer.hasVisiblePixels(bitmap)).isTrue()
    }

    @Test
    fun emptyDrawableFallsBackToLetterTile() {
        val tile = IconRasterizer.letterTile("Messages", 64)
        assertThat(tile.width).isEqualTo(64)
        assertThat(tile.height).isEqualTo(64)
        assertThat(IconRasterizer.hasVisiblePixels(tile)).isTrue()
        assertThat(Color.alpha(tile.getPixel(0, 0))).isEqualTo(255)
        val bitmap = IconRasterizer.toBitmap(ColorDrawable(Color.TRANSPARENT), 64)
        assertThat(bitmap.width).isEqualTo(64)
        assertThat(IconRasterizer.hasVisiblePixels(bitmap)).isTrue()
    }

    @Test
    fun resolverBindSetsVisibleBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = FileIconPackStore(context.filesDir)
        val prefs = IconPreferences(
            context.getSharedPreferences("icon_rasterizer_test", Context.MODE_PRIVATE)
        )
        prefs.showIcons = false
        val resolver = AppIconResolver(context, store, prefs, OnDeviceIconStylizer(context))
        val view = android.widget.ImageView(context)
        view.layout(0, 0, 48, 48)
        resolver.bind(view, context.packageName, context.applicationInfo.className ?: "", 0L)
        assertThat(view.visibility).isEqualTo(android.view.View.VISIBLE)
        assertThat(view.drawable).isNotNull()
        val bitmap = resolver.resolveBitmap(
            context.packageName,
            context.applicationInfo.className ?: "",
            0L,
            64
        )
        assertThat(bitmap.width).isEqualTo(64)
        assertThat(bitmap.height).isEqualTo(64)
        assertThat(IconRasterizer.hasVisiblePixels(bitmap)).isTrue()
    }
}
