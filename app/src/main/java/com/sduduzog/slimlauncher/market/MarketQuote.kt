package com.sduduzog.slimlauncher.market

/** Cached quote for a built-in market card. [changePercent] is null when the feed has none. */
data class MarketQuote(
    val kind: String,
    val label: String,
    val price: Double,
    val changePercent: Double?,
    val updatedAtMs: Long,
    val source: String
)

object MarketKinds {
    const val GOLD = "forge.gold"
    const val NIFTY = "forge.nifty"
    const val LABEL_GOLD = "Gold · 24K"
    const val LABEL_NIFTY = "NIFTY 50"
    const val APP_LABEL = "Forge"
    const val COMPACT_HEIGHT_DP = 112
    const val SOURCE_GOLD = "goldprice.dev"
    const val SOURCE_NIFTY = "yahoo"

    fun isBuiltin(kind: String): Boolean = kind == GOLD || kind == NIFTY

    fun label(kind: String): String = when (kind) {
        GOLD -> LABEL_GOLD
        NIFTY -> LABEL_NIFTY
        else -> kind
    }
}

object MarketSources {
    const val GOLD_URL = "https://api.goldprice.dev/v1/carat?currency=INR"
    const val NIFTY_URL =
        "https://query1.finance.yahoo.com/v8/finance/chart/%5ENSEI?range=1d&interval=1d"
    const val USER_AGENT = "ForgeLauncher/1.0 (Android)"
}
