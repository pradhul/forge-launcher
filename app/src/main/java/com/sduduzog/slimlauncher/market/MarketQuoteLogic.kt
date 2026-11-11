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
    private val goldHistoryRow = Regex(
        """<td>\s*([A-Za-z]{3}\s+\d{1,2},\s+\d{4})\s*</td>\s*<td>[\s\S]*?</td>\s*<td>\s*₹\s*([0-9,]+)""",
        RegexOption.IGNORE_CASE
    )
    private val goldGram1Delta = Regex(
        """<td>\s*1\s*</td>\s*<td>[\s\S]*?</td>\s*<td>\s*₹\s*[0-9,]+\s*<span[^>]*>\s*\(([+-]?\d+)\)""",
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
        val previous = parseGoodreturnsPrevious22k(decoded, perGram, nowMs)
        val pct = dayChange(perGram, previous)?.percent
            ?: goldWidgetChange.find(decoded)?.groupValues?.get(1)?.toDoubleOrNull()
        return MarketQuote(
            kind = MarketKinds.GOLD,
            label = MarketKinds.LABEL_GOLD,
            price = perGram,
            changePercent = pct?.takeIf { it.isFinite() },
            updatedAtMs = nowMs,
            source = MarketKinds.SOURCE_GOLD_RETAIL,
            spot = false,
            previousPrice = previous
        )
    }

    fun parseGoodreturnsPrevious22k(html: String, todayPrice: Double, nowMs: Long): Double? {
        val today = istDate(nowMs)
        val earlier = parseGoodreturnsHistory22k(html)
            .filter { it.first < today }
            .maxByOrNull { it.first }
            ?.second
        if (earlier != null && earlier > 0.0) return earlier
        val delta = goldGram1Delta.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        if (delta != null && delta.isFinite()) return todayPrice - delta
        return null
    }

    fun parseGoodreturnsHistory22k(html: String): List<Pair<String, Double>> {
        return goldHistoryRow.findAll(html).mapNotNull { match ->
            val date = parseHistoryDate(match.groupValues[1]) ?: return@mapNotNull null
            val price = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            if (price > 0.0) date to price else null
        }.toList()
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
        val previous = niftyPreviousClose(meta, price)
        val pct = number(meta, "regularMarketChangePercent").takeIf { it.isFinite() }
            ?: number(meta, "fulldayChangePercent").takeIf { it.isFinite() }
            ?: dayChange(price, previous)?.percent
        return MarketQuote(
            kind = MarketKinds.NIFTY,
            label = MarketKinds.LABEL_NIFTY,
            price = price,
            changePercent = pct,
            updatedAtMs = nowMs,
            source = MarketKinds.SOURCE_NIFTY,
            previousPrice = previous
        )
    }

    fun niftyPreviousClose(meta: JSONObject, price: Double): Double? {
        val fullday = number(meta, "fulldayChange")
        if (fullday.isFinite()) return price - fullday
        val regular = number(meta, "regularMarketChange")
        if (regular.isFinite()) return price - regular
        val chartPrev = number(meta, "chartPreviousClose")
        if (chartPrev.isFinite() && chartPrev > 0.0) return chartPrev
        return null
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

    fun formatDayChange(kind: String, change: DayChange): String {
        val dir = changeDirection(change)
        val arrow = when {
            dir > 0 -> "▲ "
            dir < 0 -> "▼ "
            else -> ""
        }
        val amount = if (kind == MarketKinds.GOLD) {
            formatInr(abs(change.amount))
        } else {
            groupIndian(abs(change.amount), 2)
        }
        return "$arrow$amount (${formatChangePercent(abs(change.percent))}) vs yesterday"
    }

    fun formatChangePercent(percent: Double): String {
        val abs = abs(percent)
        val one = String.format(Locale.US, "%.1f", abs)
        return if (one == "0.0" && abs >= 0.005) {
            String.format(Locale.US, "%.2f%%", abs)
        } else {
            "$one%"
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

    fun changeDirection(change: DayChange): Int {
        return when {
            change.amount > 0.005 || change.percent > 0.005 -> 1
            change.amount < -0.005 || change.percent < -0.005 -> -1
            else -> 0
        }
    }

    fun dayChange(price: Double, previous: Double?): DayChange? {
        if (previous == null || !previous.isFinite() || previous == 0.0) return null
        if (!price.isFinite()) return null
        val amount = price - previous
        val percent = amount / previous * 100.0
        if (!amount.isFinite() || !percent.isFinite()) return null
        return DayChange(amount, percent)
    }

    fun istDate(nowMs: Long, zone: TimeZone = TimeZone.getTimeZone(ZONE_ID)): String {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun previousFromHistory(history: Map<String, Double>, todayIst: String): Double? {
        val date = history.keys.filter { it < todayIst }.maxOrNull() ?: return null
        return history[date]?.takeIf { it.isFinite() && it > 0.0 }
    }

    fun previousFromPercent(price: Double, percent: Double?): Double? {
        if (percent == null || !percent.isFinite()) return null
        val denom = 1.0 + percent / 100.0
        if (!denom.isFinite() || denom == 0.0) return null
        val previous = price / denom
        return previous.takeIf { it.isFinite() && it > 0.0 }
    }

    fun recordDay(
        history: Map<String, Double>,
        todayIst: String,
        price: Double,
        keep: Int = 30
    ): Map<String, Double> {
        if (!price.isFinite() || price <= 0.0 || todayIst.isBlank()) return history
        val next = history.toSortedMap()
        next[todayIst] = price
        while (next.size > keep) next.remove(next.firstKey())
        return next
    }

    /**
     * Prefer the source's previous-day rate, then implied percent, then the
     * last locally stored value from an earlier IST date.
     */
    fun attachPrevious(
        quote: MarketQuote,
        history: Map<String, Double>,
        nowMs: Long
    ): Pair<MarketQuote, Map<String, Double>> {
        val today = istDate(nowMs)
        val previous = quote.previousPrice?.takeIf { it.isFinite() && it > 0.0 }
            ?: previousFromPercent(quote.price, quote.changePercent)
            ?: previousFromHistory(history, today)
        val change = dayChange(quote.price, previous)
        val next = quote.copy(
            previousPrice = previous,
            changePercent = change?.percent
        )
        return next to recordDay(history, today, quote.price)
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

    private fun parseHistoryDate(label: String): String? {
        val parsed = runCatching {
            val fmt = java.text.SimpleDateFormat("MMM d, yyyy", Locale.US)
            fmt.isLenient = false
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(label.trim())
        }.getOrNull() ?: return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = parsed
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
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
