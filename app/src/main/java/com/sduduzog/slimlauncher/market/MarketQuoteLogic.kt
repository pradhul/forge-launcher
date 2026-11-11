package com.sduduzog.slimlauncher.market

import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

/** Parsers, NSE session window, refresh gates, and display formatting. */
object MarketQuoteLogic {
    const val NIFTY_INTERVAL_MS = 90_000L
    const val GOLD_INTERVAL_MS = 30L * 60_000L
    const val BACKGROUND_INTERVAL_MIN = 15L
    const val OPEN_MINUTE = 9 * 60 + 15
    const val CLOSE_MINUTE = 15 * 60 + 30
    const val ZONE_ID = "Asia/Kolkata"

    private val goldJs22k = Regex(
        """['"]22['"]\s*:\s*parseMetalPrice\(\s*['"]([0-9,.]+)['"]\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val goldWidget22k = Regex(
        """22\s*[kK]\s*Gold[\s\S]{0,500}?(?:₹|&#8377;|Rs\.?)\s*([0-9,]+(?:\.[0-9]+)?)\s*/\s*gm""",
        RegexOption.IGNORE_CASE
    )
    private val goldWidgetChange = Regex(
        """22\s*[kK]\s*Gold[\s\S]{0,500}?\(([+-]?[0-9.]+)\s*%\)""",
        RegexOption.IGNORE_CASE
    )

    /** Indian retail 22K per gram from Goodreturns HTML (JS blob or Mumbai widget). */
    fun parseGoodreturns22k1g(html: String, nowMs: Long): MarketQuote {
        val decoded = html.replace("&#8377;", "₹").replace("&#x20b9;", "₹", ignoreCase = true)
        val raw = goldJs22k.find(decoded)?.groupValues?.get(1)
            ?: goldWidget22k.find(decoded)?.groupValues?.get(1)
            ?: error("goodreturns 22k missing")
        val perGram = raw.replace(",", "").toDoubleOrNull() ?: error("goodreturns 22k invalid")
        require(perGram > 0.0) { "goodreturns 22k missing" }
        val pct = goldWidgetChange.find(decoded)?.groupValues?.get(1)?.toDoubleOrNull()
        return MarketQuote(
            kind = MarketKinds.GOLD,
            label = MarketKinds.LABEL_GOLD,
            price = perGram,
            changePercent = pct?.takeIf { it.isFinite() },
            updatedAtMs = nowMs,
            source = MarketKinds.SOURCE_GOLD_RETAIL,
            spot = false
        )
    }

    /** Spot-derived 22K per gram (LBMA/FX), used only as fallback. */
    fun parseGoldSpot22k1g(json: String, nowMs: Long): MarketQuote {
        val obj = JSONObject(json)
        val perGram = number(obj, "price_gram_22k")
        require(perGram.isFinite() && perGram > 0.0) { "gold spot missing" }
        return MarketQuote(
            kind = MarketKinds.GOLD,
            label = MarketKinds.LABEL_GOLD,
            price = perGram,
            changePercent = null,
            updatedAtMs = nowMs,
            source = MarketKinds.SOURCE_GOLD_SPOT,
            spot = true
        )
    }

    fun parseNifty(json: String, nowMs: Long): MarketQuote {
        val meta = JSONObject(json)
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)
            .getJSONObject("meta")
        val price = number(meta, "regularMarketPrice")
        require(price.isFinite() && price > 0.0) { "nifty price missing" }
        val pct = number(meta, "regularMarketChangePercent").takeIf { it.isFinite() }
            ?: run {
                val prev = number(meta, "chartPreviousClose")
                if (prev.isFinite() && prev != 0.0) (price - prev) / prev * 100.0 else null
            }
        return MarketQuote(
            kind = MarketKinds.NIFTY,
            label = MarketKinds.LABEL_NIFTY,
            price = price,
            changePercent = pct,
            updatedAtMs = nowMs,
            source = MarketKinds.SOURCE_NIFTY
        )
    }

    fun parse(kind: String, body: String, nowMs: Long): MarketQuote = when (kind) {
        MarketKinds.GOLD -> runCatching { parseGoodreturns22k1g(body, nowMs) }.getOrElse {
            parseGoldSpot22k1g(body, nowMs)
        }
        MarketKinds.NIFTY -> parseNifty(body, nowMs)
        else -> error("unknown kind $kind")
    }

    fun url(kind: String): String = when (kind) {
        MarketKinds.GOLD -> MarketSources.GOLD_RETAIL_URL
        MarketKinds.NIFTY -> MarketSources.NIFTY_URL
        else -> error("unknown kind $kind")
    }

    fun formatSynced(
        updatedAtMs: Long,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        if (updatedAtMs <= 0L) return ""
        val fmt = java.text.SimpleDateFormat("d MMM, h:mm a", locale)
        fmt.timeZone = zone
        return "Synced ${fmt.format(java.util.Date(updatedAtMs))}"
    }

    fun formatFooter(synced: String, failed: Boolean, spot: Boolean): String {
        val parts = ArrayList<String>(3)
        if (failed) parts.add("Couldn't update")
        if (synced.isNotEmpty()) parts.add(synced)
        if (spot) parts.add("spot")
        return parts.joinToString(" · ")
    }

    fun isNseOpen(nowMs: Long, zone: TimeZone = TimeZone.getTimeZone(ZONE_ID)): Boolean {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return minute in OPEN_MINUTE..CLOSE_MINUTE
    }

    /**
     * Background/periodic gate. Nifty stays frozen outside NSE hours.
     * A tap bypasses this and always attempts a fetch.
     */
    fun shouldFetch(
        kind: String,
        lastSuccessMs: Long,
        nowMs: Long,
        marketOpen: Boolean,
        force: Boolean
    ): Boolean {
        if (force) return true
        if (kind == MarketKinds.NIFTY && !marketOpen) return false
        val interval = if (kind == MarketKinds.GOLD) GOLD_INTERVAL_MS else NIFTY_INTERVAL_MS
        if (lastSuccessMs <= 0L) return true
        return nowMs - lastSuccessMs >= interval
    }

    fun formatInr(amount: Double): String = "\u20B9${groupIndian(amount, 0)}"

    fun formatIndex(amount: Double): String = groupIndian(amount, 2)

    fun formatPrice(kind: String, amount: Double): String =
        if (kind == MarketKinds.GOLD) formatInr(amount) else formatIndex(amount)

    fun formatChange(percent: Double): String {
        val body = String.format(Locale.US, "%.2f%%", abs(percent))
        return when {
            percent > 0.005 -> "▲ $body"
            percent < -0.005 -> "▼ $body"
            else -> body
        }
    }

    fun changeDirection(percent: Double?): Int {
        if (percent == null || !percent.isFinite()) return 0
        return when {
            percent > 0.005 -> 1
            percent < -0.005 -> -1
            else -> 0
        }
    }

    /** Indian grouping: 24,312.60 and 1,21,338 — not US thousands. */
    fun groupIndian(amount: Double, decimals: Int): String {
        val factor = when (decimals) {
            0 -> 1.0
            1 -> 10.0
            else -> 100.0
        }
        val scaled = (amount * factor).roundToLong()
        val negative = scaled < 0
        val abs = abs(scaled)
        val intPart = if (decimals == 0) abs else abs / factor.toLong()
        val frac = if (decimals == 0) 0L else abs % factor.toLong()
        val grouped = groupInteger(intPart)
        val body = if (decimals == 0) grouped else grouped + "." + frac.toString().padStart(decimals, '0')
        return if (negative) "-$body" else body
    }

    private fun groupInteger(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val last3 = raw.takeLast(3)
        val head = raw.dropLast(3)
        val chunks = ArrayList<String>()
        var i = head.length
        while (i > 0) {
            val start = (i - 2).coerceAtLeast(0)
            chunks.add(head.substring(start, i))
            i = start
        }
        return chunks.asReversed().joinToString(",") + "," + last3
    }

    private fun number(obj: JSONObject, key: String): Double {
        if (!obj.has(key) || obj.isNull(key)) return Double.NaN
        return when (val value = obj.get(key)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: Double.NaN
            else -> Double.NaN
        }
    }
}
