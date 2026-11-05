package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WallpaperCaptureLogicTest {

    @Test
    fun cacheWinsOverSystemWallpaperApis() {
        assertThat(
            WallpaperCaptureLogic.pickSource(
                cacheAvailable = true,
                drawableAvailable = true,
                peekAvailable = true,
                fileAvailable = true
            )
        ).isEqualTo(WallpaperCaptureLogic.Source.CACHE)
        assertThat(
            WallpaperCaptureLogic.pickSource(
                cacheAvailable = false,
                drawableAvailable = true,
                peekAvailable = true,
                fileAvailable = true
            )
        ).isEqualTo(WallpaperCaptureLogic.Source.DRAWABLE)
        assertThat(
            WallpaperCaptureLogic.pickSource(
                cacheAvailable = false,
                drawableAvailable = false,
                peekAvailable = true,
                fileAvailable = true
            )
        ).isEqualTo(WallpaperCaptureLogic.Source.PEEK)
        assertThat(
            WallpaperCaptureLogic.pickSource(
                cacheAvailable = false,
                drawableAvailable = false,
                peekAvailable = false,
                fileAvailable = true
            )
        ).isEqualTo(WallpaperCaptureLogic.Source.FILE)
        assertThat(
            WallpaperCaptureLogic.pickSource(
                cacheAvailable = false,
                drawableAvailable = false,
                peekAvailable = false,
                fileAvailable = false
            )
        ).isEqualTo(WallpaperCaptureLogic.Source.NONE)
    }

    @Test
    fun securityExceptionIsPermissionDenied() {
        assertThat(
            WallpaperCaptureLogic.isPermissionDenied(SecurityException("READ_WALLPAPER_INTERNAL"))
        ).isTrue()
        assertThat(
            WallpaperCaptureLogic.isPermissionDenied(IllegalStateException("missing wallpaper permission"))
        ).isTrue()
        assertThat(
            WallpaperCaptureLogic.isPermissionDenied(IllegalStateException("unrelated"))
        ).isFalse()
        assertThat(WallpaperCaptureLogic.describe(WallpaperCaptureLogic.Source.NONE))
            .contains("READ_WALLPAPER_INTERNAL")
    }
}
