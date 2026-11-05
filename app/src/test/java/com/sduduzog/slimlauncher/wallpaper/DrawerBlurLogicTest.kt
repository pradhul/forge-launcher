package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.IconPreferences
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DrawerBlurLogicTest {

    @Test
    fun tintAlphaIsTiedToBlurAndNeverFullyTransparent() {
        assertThat(DrawerBlurLogic.tintAlpha(0)).isEqualTo(DrawerBlurLogic.MIN_TINT)
        assertThat(DrawerBlurLogic.tintAlpha(100)).isEqualTo(DrawerBlurLogic.MAX_TINT)
        assertThat(DrawerBlurLogic.tintAlpha(50)).isGreaterThan(DrawerBlurLogic.MIN_TINT)
        assertThat(DrawerBlurLogic.tintAlpha(0)).isLessThan(0.2f)
        assertThat(DrawerBlurLogic.tintAlpha(0)).isGreaterThan(0f)
        val argb = DrawerBlurLogic.tintArgb(0xFF102030.toInt(), 50)
        assertThat(argb ushr 24).isGreaterThan(0)
    }

    @Test
    fun windowBlurIsOffWhenCrossWindowUnavailable() {
        assertThat(DrawerBlurLogic.shouldUseWindowBlur(31, true)).isTrue()
        assertThat(DrawerBlurLogic.shouldUseWindowBlur(31, false)).isFalse()
        assertThat(DrawerBlurLogic.shouldUseWindowBlur(30, true)).isFalse()
        assertThat(DrawerBlurLogic.needsBitmapFallback(31, false)).isTrue()
        assertThat(DrawerBlurLogic.needsBitmapFallback(31, true)).isFalse()
        assertThat(
            DrawerBlurLogic.windowBlurRadiusPx(50, 1f, crossWindowEnabled = false, sdkInt = 31)
        ).isEqualTo(0)
        assertThat(
            DrawerBlurLogic.windowBlurRadiusPx(50, 1f, crossWindowEnabled = true, sdkInt = 31)
        ).isEqualTo(12)
    }

    @Test
    fun sampleBlurRadiusScalesAndBoxBlurSpreadsColor() {
        assertThat(DrawerBlurLogic.sampleBlurRadius(0)).isEqualTo(0)
        assertThat(DrawerBlurLogic.sampleBlurRadius(100)).isGreaterThan(0)
        val pixels = IntArray(5) { if (it == 2) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        val blurred = BoxBlur.blur(pixels, 5, 1, 1)
        assertThat(blurred[1] and 0x00FFFFFF).isNotEqualTo(0)
        assertThat(blurred[2] and 0x00FFFFFF).isNotEqualTo(0)
    }

    @Test
    fun downsampleKeepsAspect() {
        assertThat(DrawerBlurLogic.downsampleSize(1920, 1080)).isEqualTo(96 to 54)
        assertThat(DrawerBlurLogic.downsampleSize(40, 40)).isEqualTo(40 to 40)
    }

    @Test
    fun motionOpenCloseAndBack() {
        assertThat(DrawerMotionLogic.shouldOpenFromHomeSwipe(100f, 0f, -400f)).isTrue()
        assertThat(DrawerMotionLogic.shouldOpenFromHomeSwipe(10f, 0f, -400f)).isFalse()
        assertThat(DrawerMotionLogic.shouldCloseFromDrawerSwipe(100f, gridAtTop = true)).isTrue()
        assertThat(DrawerMotionLogic.shouldCloseFromDrawerSwipe(100f, gridAtTop = false)).isFalse()
        assertThat(DrawerMotionLogic.backClosesDrawerOnly(true)).isTrue()
        assertThat(DrawerMotionLogic.backClosesDrawerOnly(false)).isFalse()
        assertThat(DrawerMotionLogic.ANIM_MS).isAtMost(200)
        assertThat(DrawerMotionLogic.ANIM_MS).isAtLeast(100)
    }

    @Test
    fun blurPercentPersistsWithoutDrawerFade() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.getSharedPreferences("blur_pref_test", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val prefs = IconPreferences(store)
        assertThat(prefs.drawerBlurPercent).isEqualTo(DrawerBlurLogic.DEFAULT_BLUR_PERCENT)
        assertThat(prefs.homeFadePercent).isEqualTo(100)
        prefs.drawerBlurPercent = 80
        prefs.homeFadePercent = 40
        val reloaded = IconPreferences(store)
        assertThat(reloaded.drawerBlurPercent).isEqualTo(80)
        assertThat(reloaded.homeFadePercent).isEqualTo(40)
        assertThat(reloaded.chrome.value.drawerBlurPercent).isEqualTo(80)
        prefs.drawerBlurPercent = 250
        assertThat(prefs.drawerBlurPercent).isEqualTo(100)
    }

    @Test
    fun blurBehindRequiresTranslucentCrossWindowPath() {
        assertThat(
            DrawerBlurLogic.blurBehindRadiusPx(50, 1f, crossWindowEnabled = true, sdkInt = 31)
        ).isEqualTo(20)
        assertThat(
            DrawerBlurLogic.blurBehindRadiusPx(0, 3f, crossWindowEnabled = true, sdkInt = 31)
        ).isEqualTo(0)
        assertThat(
            DrawerBlurLogic.blurBehindRadiusPx(100, 1f, crossWindowEnabled = false, sdkInt = 31)
        ).isEqualTo(0)
        assertThat(DrawerBlurLogic.backdropMode(31, true, hasBitmap = false))
            .isEqualTo(DrawerBlurLogic.BackdropMode.LIVE_WINDOW)
        assertThat(DrawerBlurLogic.backdropMode(31, false, hasBitmap = true))
            .isEqualTo(DrawerBlurLogic.BackdropMode.BITMAP)
        assertThat(DrawerBlurLogic.backdropMode(31, false, hasBitmap = false))
            .isEqualTo(DrawerBlurLogic.BackdropMode.COLOR_SCRIM)
        assertThat(
            DrawerBlurLogic.shouldDrawBitmapBackdrop(
                DrawerBlurLogic.BackdropMode.BITMAP,
                hasBitmap = true,
                percent = 50
            )
        ).isTrue()
        assertThat(
            DrawerBlurLogic.shouldDrawBitmapBackdrop(
                DrawerBlurLogic.BackdropMode.LIVE_WINDOW,
                hasBitmap = true,
                percent = 0
            )
        ).isFalse()
        assertThat(
            DrawerBlurLogic.shouldDrawBitmapBackdrop(
                DrawerBlurLogic.BackdropMode.COLOR_SCRIM,
                hasBitmap = false,
                percent = 100
            )
        ).isFalse()
    }

    @Test
    fun composeDoesNotReblurAPreparedSnapshot() {
        assertThat(DrawerBlurLogic.shouldApplyComposeBlur(hasPreblurredBitmap = true)).isFalse()
        assertThat(DrawerBlurLogic.shouldApplyComposeBlur(hasPreblurredBitmap = false)).isTrue()
    }

    @Test
    fun wallpaperColorsScrimIsStrongerThanTint() {
        assertThat(DrawerBlurLogic.fallbackScrimAlpha(0))
            .isGreaterThan(DrawerBlurLogic.tintAlpha(0))
        assertThat(DrawerBlurLogic.fallbackScrimAlpha(100))
            .isGreaterThan(DrawerBlurLogic.tintAlpha(100))
        val scrim = DrawerBlurLogic.fallbackScrimArgb(0xFF336699.toInt(), 80)
        assertThat(scrim ushr 24).isGreaterThan(0)
        assertThat(scrim and 0x00FFFFFF).isEqualTo(0x00336699)
    }
}
