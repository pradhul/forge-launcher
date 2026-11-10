package com.sduduzog.slimlauncher.market

import android.content.Context
import org.json.JSONObject

class MarketQuoteStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(kind: String): MarketQuote? = parse(prefs.getString(kind, null))

    fun lastSuccessMs(kind: String): Long = prefs.getLong(successKey(kind), 0L)

    fun save(quote: MarketQuote) {
        prefs.edit()
            .putString(quote.kind, serialize(quote))
            .putLong(successKey(quote.kind), quote.updatedAtMs)
            .apply()
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
            if (quote.changePercent != null) obj.put("changePercent", quote.changePercent)
            else obj.put("changePercent", JSONObject.NULL)
            return obj.toString()
        }

        fun parse(raw: String?): MarketQuote? {
            if (raw.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val pct = if (obj.has("changePercent") && !obj.isNull("changePercent")) {
                    obj.getDouble("changePercent")
                } else null
                MarketQuote(
                    kind = obj.getString("kind"),
                    label = obj.getString("label"),
                    price = obj.getDouble("price"),
                    changePercent = pct,
                    updatedAtMs = obj.getLong("updatedAtMs"),
                    source = obj.optString("source", "")
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun successKey(kind: String) = "${kind}_ok"
    }
}
