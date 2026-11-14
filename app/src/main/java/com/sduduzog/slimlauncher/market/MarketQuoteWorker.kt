package com.sduduzog.slimlauncher.market

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketQuoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val kinds = inputData.getString(KEY_KINDS)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: setOf(MarketKinds.GOLD, MarketKinds.NIFTY)
        val force = inputData.getBoolean(KEY_FORCE, false)
        runCatching {
            MarketQuoteRepository.get(applicationContext).refreshDueSync(kinds, force)
        }
        Result.success()
    }

    companion object {
        const val KEY_KINDS = "kinds"
        const val KEY_FORCE = "force"
    }
}
