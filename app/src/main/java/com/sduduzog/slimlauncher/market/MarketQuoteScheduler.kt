package com.sduduzog.slimlauncher.market

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object MarketQuoteScheduler {
    const val PERIODIC_NAME = "forge_market_quotes"
    const val ONCE_NAME = "forge_market_quotes_now"

    fun sync(context: Context, placed: Boolean) {
        val manager = manager(context) ?: return
        if (!placed) {
            manager.cancelUniqueWork(PERIODIC_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<MarketQuoteWorker>(
            MarketQuoteLogic.BACKGROUND_INTERVAL_MIN,
            TimeUnit.MINUTES
        )
            .setConstraints(network())
            .setInputData(workDataOf(MarketQuoteWorker.KEY_FORCE to false))
            .build()
        manager.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun refreshNow(context: Context, kinds: Collection<String>, force: Boolean) {
        if (kinds.isEmpty()) return
        val manager = manager(context) ?: return
        val request = OneTimeWorkRequestBuilder<MarketQuoteWorker>()
            .setConstraints(network())
            .setInputData(
                workDataOf(
                    MarketQuoteWorker.KEY_FORCE to force,
                    MarketQuoteWorker.KEY_KINDS to kinds.joinToString(",")
                )
            )
            .build()
        manager.enqueueUniqueWork(
            ONCE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun manager(context: Context): WorkManager? =
        runCatching { WorkManager.getInstance(context.applicationContext) }.getOrNull()

    private fun network(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
