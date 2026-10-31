package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class BingWallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            WallpaperEntryPoint::class.java
        )
        val prefs = entry.iconPreferences()
        val forced = inputData.getBoolean(KEY_FORCE, false)
        if (!prefs.bingWallpaperEnabled && !forced) {
            return@withContext Result.success()
        }
        try {
            val client = entry.okHttp()
            val json = fetch(client, BingArchiveParser.archiveUrl())
            val image = BingArchiveParser.parse(json)
            val bitmap = downloadFirst(client, BingArchiveParser.candidateUrls(image))
                ?: error("Could not download Bing image")
            BingWallpaperApplier.setWallpaper(
                applicationContext,
                bitmap,
                prefs.bingSetLockScreen
            )
            prefs.bingTitle = image.title
            prefs.bingCopyright = image.copyright
            prefs.bingStartdate = image.startdate
            prefs.bingLastError = ""
            prefs.wallpaperTheme = entry.extractor().extract(bitmap)
            prefs.wallpaperPresent = true
            Result.success()
        } catch (error: Exception) {
            prefs.bingLastError = error.message ?: error.javaClass.simpleName
            prefs.notifyIconsChanged()
            Result.retry()
        }
    }

    private fun fetch(client: OkHttpClient, url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string() ?: error("empty archive")
        }
    }

    private fun downloadFirst(client: OkHttpClient, urls: List<String>): Bitmap? {
        for (url in urls) {
            val bytes = runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.bytes()
                }
            }.getOrNull() ?: continue
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) return bitmap
        }
        return null
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
