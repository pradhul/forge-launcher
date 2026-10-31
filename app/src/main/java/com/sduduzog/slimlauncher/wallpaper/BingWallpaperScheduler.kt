package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.sduduzog.slimlauncher.icons.IconPreferences
import java.util.concurrent.TimeUnit

object BingWallpaperScheduler {
    const val PERIODIC_NAME = "forge_bing_wallpaper_daily"
    const val ONCE_NAME = "forge_bing_wallpaper_now"

    fun sync(context: Context, prefs: IconPreferences) {
        val manager = WorkManager.getInstance(context)
        if (!prefs.bingWallpaperEnabled) {
            manager.cancelUniqueWork(PERIODIC_NAME)
            return
        }
        manager.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<BingWallpaperWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints(prefs.bingUnmeteredOnly))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        )
    }

    fun refreshNow(context: Context, unmeteredOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<BingWallpaperWorker>()
            .setConstraints(networkConstraints(unmeteredOnly))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(workDataOf(BingWallpaperWorker.KEY_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun networkConstraints(unmeteredOnly: Boolean): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(
                if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
    }
}
