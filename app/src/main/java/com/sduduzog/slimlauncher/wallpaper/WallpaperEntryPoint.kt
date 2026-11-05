package com.sduduzog.slimlauncher.wallpaper

import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WallpaperEntryPoint {
    fun iconPreferences(): IconPreferences
    fun okHttp(): OkHttpClient
    fun extractor(): PackThemeExtractor
    fun wallpaperPackCoordinator(): WallpaperPackCoordinator
}
