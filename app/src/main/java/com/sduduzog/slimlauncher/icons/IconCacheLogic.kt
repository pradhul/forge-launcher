package com.sduduzog.slimlauncher.icons

/**
 * Cache keys for styled launcher icons. Theme + pack identity must be
 * part of the key so a wallpaper recolour never serves a stale bitmap.
 */
object IconCacheLogic {
    const val MAX_ENTRIES = 256

    fun key(
        packageName: String,
        activityName: String,
        userSerial: Long,
        packId: String?,
        themeHash: String,
        size: Int
    ): String {
        return "$packageName\u0000$activityName\u0000$userSerial\u0000${packId.orEmpty()}\u0000$themeHash\u0000$size"
    }

    fun themeHash(packId: String?, theme: PackTheme?): String {
        if (theme == null) return packId.orEmpty()
        return "${packId.orEmpty()}:${theme.seed}:${theme.accent}:${theme.background}:${theme.onBackground}"
    }

    fun missingKeys(have: Set<String>, wanted: Collection<String>): List<String> {
        if (have.isEmpty()) return wanted.toList()
        return wanted.filterNot { it in have }
    }

    fun shouldRefreshCatalog(
        lastLoadMs: Long,
        nowMs: Long,
        staleAfterMs: Long,
        hasCache: Boolean
    ): Boolean {
        if (!hasCache) return true
        if (lastLoadMs <= 0L) return true
        return nowMs - lastLoadMs >= staleAfterMs
    }
}
