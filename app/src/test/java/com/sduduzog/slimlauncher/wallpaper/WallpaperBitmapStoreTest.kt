package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class WallpaperBitmapStoreTest {

    @Test
    fun roundTripsAColorfulBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WallpaperBitmapStore.file(context).delete()
        val source = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.MAGENTA)
        source.setPixel(0, 0, Color.YELLOW)
        assertThat(WallpaperBitmapStore.saveTo(context, source)).isTrue()
        val loaded = WallpaperBitmapStore.loadFrom(context)
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.width).isEqualTo(32)
        assertThat(loaded.height).isEqualTo(48)
        // JPEG is lossy; magenta should still be strongly red+blue.
        val pixel = loaded.getPixel(2, 2)
        assertThat(android.graphics.Color.red(pixel)).isGreaterThan(200)
        assertThat(android.graphics.Color.blue(pixel)).isGreaterThan(200)
    }

    @Test
    fun scalesHugeBitmapsDown() {
        val huge = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)
        val scaled = WallpaperBitmapStore.scaleForCache(huge)
        assertThat(scaled.width).isAtMost(WallpaperBitmapStore.MAX_EDGE)
        assertThat(scaled.height).isAtMost(WallpaperBitmapStore.MAX_EDGE)
        assertThat(scaled.width.toFloat() / scaled.height).isWithin(0.02f).of(2f)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class WallpaperSnapshotPrepareTest {

    @Test
    fun zeroPercentKeepsUnblurredDownsample() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        source.setPixel(10, 10, Color.WHITE)
        val prepared = WallpaperSnapshotter.prepare(source, 0, sdkInt = 29)
        assertThat(prepared.getPixel(10, 10)).isEqualTo(Color.WHITE)
    }

    @Test
    fun fullBlurChangesThePreparedBitmap() {
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLACK)
        source.setPixel(0, 0, Color.WHITE)
        source.setPixel(31, 31, Color.RED)
        val sharp = WallpaperSnapshotter.prepare(source, 0, sdkInt = 29)
        val blurred = WallpaperSnapshotter.prepare(source, 100, sdkInt = 29)
        assertThat(blurred.width).isEqualTo(sharp.width)
        assertThat(DrawerBlurLogic.sampleBlurRadius(100)).isGreaterThan(0)
        val pixels = IntArray(5) { if (it == 2) Color.WHITE else Color.BLACK }
        val spread = BoxBlur.blur(pixels, 5, 1, 1)
        assertThat(spread[1] and 0x00FFFFFF).isNotEqualTo(0)
    }
}
