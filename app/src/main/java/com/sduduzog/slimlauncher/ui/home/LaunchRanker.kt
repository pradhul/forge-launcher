package com.sduduzog.slimlauncher.ui.home

import kotlin.math.pow

data class LaunchRecord(
    val key: String,
    val count: Int,
    val lastLaunchMillis: Long
)

object LaunchRanker {
    const val LIMIT = 4
    const val HALF_LIFE_DAYS = 7.0
    private const val DAY_MS = 24.0 * 60.0 * 60.0 * 1000.0

    fun record(key: String, existing: LaunchRecord?, nowMillis: Long): LaunchRecord {
        return LaunchRecord(
            key = key,
            count = (existing?.count ?: 0) + 1,
            lastLaunchMillis = nowMillis
        )
    }

    fun score(record: LaunchRecord, nowMillis: Long): Double {
        val ageDays = ((nowMillis - record.lastLaunchMillis).coerceAtLeast(0L)) / DAY_MS
        val decay = 0.5.pow(ageDays / HALF_LIFE_DAYS)
        return record.count * decay
    }

    fun rank(
        records: Collection<LaunchRecord>,
        nowMillis: Long,
        limit: Int = LIMIT
    ): List<String> {
        return records
            .sortedWith(
                compareByDescending<LaunchRecord> { score(it, nowMillis) }
                    .thenByDescending { it.lastLaunchMillis }
            )
            .map { it.key }
            .take(limit)
    }

    fun shouldShowFrequent(query: String, rankedKeys: List<String>): Boolean {
        return query.trim().isEmpty() && rankedKeys.isNotEmpty()
    }
}
