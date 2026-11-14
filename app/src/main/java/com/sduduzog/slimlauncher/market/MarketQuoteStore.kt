package com.sduduzog.slimlauncher.market

import android.content.Context
import org.json.JSONObject

class MarketQuoteStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(kind: String): MarketQuote? = parse(prefs.getString(kind, null))

    fun lastSuccessMs(kind: String): Long = prefs.getLong(successKey(kind), 0L)

    fun loadHistory(kind: String): Map<String, Double> = parseHistory(prefs.getString(historyKey(kind), null))

    fun save(quote: MarketQuote) {
        prefs.edit()
            .putString(quote.kind, serialize(quote))
            .putLong(successKey(quote.kind), quote.updatedAtMs)
            .apply()
    }

    fun saveHistory(kind: String, history: Map<String, Double>) {
        prefs.edit().putString(historyKey(kind), serializeHistory(history)).apply()
    }

    companion object {
        private const val PREFS = "forge_market_quotes"

        fun serialize(quote: MarketQuote): String {
            val obj = JSONObject()
                .put("kind", quote.kind)
                .put("label", quote.label)
                .put("price", quote.price)
                .put("updatedAtMs", quote.updatedAtMs)
                .put("source", quote.source)
                .put("spot", quote.spot)
            if (quote.changePercent != null) obj.put("changePercent", quote.changePercent)
            else obj.put("changePercent", JSONObject.NULL)
            if (quote.previousPrice != null) obj.put("previousPrice", quote.previousPrice)
            else obj.put("previousPrice", JSONObject.NULL)
            return obj.toString()
        }

        fun parse(raw: String?): MarketQuote? {
            if (raw.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val pct = if (obj.has("changePercent") && !obj.isNull("changePercent")) {
                    obj.getDouble("changePercent")
                } else null
                val previous = if (obj.has("previousPrice") && !obj.isNull("previousPrice")) {
                    obj.getDouble("previousPrice")
                } else null
                MarketQuote(
                    kind = obj.getString("kind"),
                    label = obj.getString("label"),
                    price = obj.getDouble("price"),
                    changePercent = pct,
                    updatedAtMs = obj.getLong("updatedAtMs"),
                    source = obj.optString("source", ""),
                    spot = obj.optBoolean("spot", false),
                    previousPrice = previous
                )
            } catch (_: Exception) {
                null
            }
        }

        fun serializeHistory(history: Map<String, Double>): String {
            val obj = JSONObject()
            for ((date, price) in history) {
                if (date.isNotBlank() && price.isFinite()) obj.put(date, price)
            }
            return obj.toString()
        }

        fun parseHistory(raw: String?): Map<String, Double> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                val obj = JSONObject(raw)
                val out = LinkedHashMap<String, Double>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val date = keys.next()
                    val price = obj.optDouble(date, Double.NaN)
                    if (price.isFinite() && price > 0.0) out[date] = price
                }
                out
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private fun successKey(kind: String) = "${kind}_ok"
        private fun historyKey(kind: String) = "${kind}_days"
    }
}
