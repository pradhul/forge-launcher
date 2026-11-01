package com.sduduzog.slimlauncher.icons

/**
 * Cache keys for styled launcher icons. Theme + pack identity must be
 * part of the key so a wallpaper recolour never serves a stale bitmap.
 */
object IconCacheLogic {
    const val MAX_ENTRIES = 256

    /** Bump to drop bitmaps rendered by an older icon pipeline. */
    const val KEY_VERSION = 4

    fun key(
        packageName: String,
        activityName: String,
        userSerial: Long,
        packId: String?,
        themeHash: String,
        size: Int
    ): String {
        return "v$KEY_VERSION\u0000$packageName\u0000$activityName\u0000$userSerial\u0000${packId.orEmpty()}\u0000$themeHash\u0000$size"
    }

    fun themeHash(packId: String?, theme: PackTheme?, style: IconStyle = IconStyle.DEFAULT): String {
        if (theme == null) return "${packId.orEmpty()}:${style.storageValue}"
        val palette = if (style == IconStyle.THEMED) {
            val c = ThemedIconPalette.forTheme(theme)
            ":p${Integer.toHexString(c.background)}-${Integer.toHexString(c.foreground)}"
        } else ""
        return "${packId.orEmpty()}:${style.storageValue}:${theme.seed}:${theme.accent}:${theme.background}:${theme.onBackground}:${theme.darkSurface}$palette"
    }

    /**
     * Single source of truth for what an icon surface asks for. Home, dock and
     * drawer all resolve through this, so they share style, palette and cache.
     */
    fun request(
        surface: IconSurface,
        packageName: String,
        activityName: String,
        userSerial: Long,
        packId: String?,
        theme: PackTheme?,
        style: IconStyle,
        sizePx: Int
    ): IconRequest {
        @Suppress("UNUSED_VARIABLE") val ignored = surface
        val colors = if (style == IconStyle.THEMED && theme != null) ThemedIconPalette.forTheme(theme) else null
        return IconRequest(
            key = key(packageName, activityName, userSerial, packId, themeHash(packId, theme, style), sizePx),
            style = style,
            colors = colors,
            sizePx = sizePx,
            allowStyle = true
        )
    }

    /**
     * Only cache a bitmap once it is final: a stock fallback shown while the
     * themed pack icon is still being generated must not poison the cache.
     */
    fun shouldCache(packApplied: Boolean, packIconReady: Boolean, styled: Boolean): Boolean {
        if (!packApplied) return true
        return packIconReady || styled
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

enum class IconSurface { HOME, DOCK, DRAWER }

data class IconRequest(
    val key: String,
    val style: IconStyle,
    val colors: ThemedColors?,
    val sizePx: Int,
    val allowStyle: Boolean
)
