package com.sduduzog.slimlauncher.market

/** Cached quote for a built-in market card. [changePercent] is null when the feed has none. */
data class MarketQuote(
    val kind: String,
    val label: String,
    val price: Double,
    val changePercent: Double?,
    val updatedAtMs: Long,
    val source: String,
    val spot: Boolean = false
)

data class MarketQuoteUi(
    val quote: MarketQuote?,
    val refreshing: Boolean = false,
    val failed: Boolean = false
)

object MarketKinds {
    const val GOLD = "forge.gold"
    const val NIFTY = "forge.nifty"
    const val LABEL_GOLD = "Gold · 22K · 1g"
    const val LABEL_NIFTY = "NIFTY 50"
    const val APP_LABEL = "Forge"
    const val COMPACT_HEIGHT_DP = 124
    const val LEGACY_COMPACT_HEIGHT_DP = 112
    const val SOURCE_GOLD_RETAIL = "goodreturns"
    const val SOURCE_GOLD_SPOT = "goldprice.dev"
    const val SOURCE_NIFTY = "yahoo"

    fun isBuiltin(kind: String): Boolean = kind == GOLD || kind == NIFTY

    fun label(kind: String): String = when (kind) {
        GOLD -> LABEL_GOLD
        NIFTY -> LABEL_NIFTY
        else -> kind
    }
}

object MarketSources {
    const val GOLD_RETAIL_URL = "https://www.goodreturns.in/gold-rates/"
    const val GOLD_RETAIL_WIDGET_URL =
        "https://www.goodreturns.in/dynamic_html_includes/web/widget/home_page_gold_widget.html"
    const val GOLD_SPOT_URL = "https://api.goldprice.dev/v1/carat?currency=INR"
    const val NIFTY_URL =
        "https://query1.finance.yahoo.com/v8/finance/chart/%5ENSEI?range=1d&interval=1d"
    const val USER_AGENT = "ForgeLauncher/1.0 (Android)"
    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
