package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BingWallpaperFlagsTest {

    @Test
    fun alwaysSetsHomeAndLockScreens() {
        assertThat(BingWallpaperApplier.whichFlags()).isEqualTo(
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        )
    }

    @Test
    fun decodeOptionsRequestFullColorArgb() {
        val options = BingWallpaperApplier.colorDecodeOptions()
        assertThat(options.inPreferredConfig).isEqualTo(android.graphics.Bitmap.Config.ARGB_8888)
        assertThat(BingWallpaperApplier.isGreyscaleConfig(android.graphics.Bitmap.Config.ARGB_8888))
            .isFalse()
        assertThat(BingWallpaperApplier.isGreyscaleConfig(android.graphics.Bitmap.Config.ALPHA_8))
            .isTrue()
    }
}
