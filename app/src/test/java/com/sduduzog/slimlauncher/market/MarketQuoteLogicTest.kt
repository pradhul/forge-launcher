package com.sduduzog.slimlauncher.market

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MarketQuoteLogicTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private fun istMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(ist)
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun goldRetailParserReads22kPerGramFromGoodreturns() {
        val html = """
            let currentMetalPrices = {
              '24': parseMetalPrice('14268'),
              '22': parseMetalPrice('13079'),
              '18': parseMetalPrice('10701')
            };
        """.trimIndent()
        val quote = MarketQuoteLogic.parseGoodreturns22k1g(html, 1_000L)
        assertThat(quote.kind).isEqualTo(MarketKinds.GOLD)
        assertThat(quote.label).isEqualTo("Gold · 22K · 1g")
        assertThat(quote.price).isWithin(0.01).of(13079.0)
        assertThat(quote.spot).isFalse()
        assertThat(quote.source).isEqualTo(MarketKinds.SOURCE_GOLD_RETAIL)
    }

    @Test
    fun goldWidgetHtmlFallsBackWhenJsBlobMissing() {
        val html = """
            <span>22k Gold</span>
            <p>&#8377; 13,079 /gm <span class="down">(-0.01%)</span></p>
        """.trimIndent()
        val quote = MarketQuoteLogic.parseGoodreturns22k1g(html, 2_000L)
        assertThat(quote.price).isWithin(0.01).of(13079.0)
        assertThat(quote.changePercent).isWithin(0.001).of(-0.01)
        assertThat(quote.previousPrice).isNull()
    }

    @Test
    fun goldHistoryTableSuppliesYesterday22kRate() {
        val html = """
            let currentMetalPrices = { '22': parseMetalPrice('13079') };
            <h2>Gold Rate in India for Last 10 Days (1 gram)</h2>
            <table>
              <tr><td>Nov 11, 2026</td><td>₹14,268</td><td>₹13,079 <span>(-1)</span></td></tr>
              <tr><td>Nov 10, 2026</td><td>₹14,269</td><td>₹13,080 <span>(-175)</span></td></tr>
              <tr><td>Nov 09, 2026</td><td>₹14,460</td><td>₹13,255 <span>(+95)</span></td></tr>
            </table>
        """.trimIndent()
        val now = istMs(2026, Calendar.NOVEMBER, 11, 8, 5)
        val quote = MarketQuoteLogic.parseGoodreturns22k1g(html, now)
        assertThat(quote.previousPrice).isWithin(0.01).of(13080.0)
        val change = MarketQuoteLogic.dayChange(quote.price, quote.previousPrice)!!
        assertThat(change.amount).isWithin(0.01).of(-1.0)
        assertThat(MarketQuoteLogic.changeDirection(change)).isEqualTo(-1)
    }

    @Test
    fun goldGram1RupeeDeltaSuppliesPreviousWhenHistoryMissing() {
        val html = """
            let currentMetalPrices = { '22': parseMetalPrice('13079') };
            <table>
              <tr>
                <td>1</td>
                <td>₹14,268 <span class="gr-delta-down">(-1)</span></td>
                <td>₹13,079 <span class="gr-delta-down">(-1)</span></td>
                <td>₹10,701 <span class="gr-delta-down">(-1)</span></td>
              </tr>
            </table>
        """.trimIndent()
        val quote = MarketQuoteLogic.parseGoodreturns22k1g(html, istMs(2026, Calendar.NOVEMBER, 11, 10, 0))
        assertThat(quote.previousPrice).isWithin(0.01).of(13080.0)
    }

    @Test
    fun goldSpotFallbackIs22kPerGramAndMarkedSpot() {
        val json = """
            {"currency":"INR","price_gram_24k":"12184.96","price_gram_22k":"11169.55"}
        """.trimIndent()
        val quote = MarketQuoteLogic.parseGoldSpot22k1g(json, 3_000L)
        assertThat(quote.price).isWithin(0.01).of(11169.55)
        assertThat(quote.spot).isTrue()
        assertThat(quote.source).isEqualTo(MarketKinds.SOURCE_GOLD_SPOT)
        assertThat(quote.label).isEqualTo("Gold · 22K · 1g")
    }

    @Test
    fun niftyParserReadsPriceAndPercentFromYahooChart() {
        val json = """
            {"chart":{"result":[{"meta":{
              "currency":"INR","symbol":"^NSEI",
              "regularMarketPrice":25044.65,
              "regularMarketChangePercent":0.233,
              "chartPreviousClose":25030.3
            }}],"error":null}}
        """.trimIndent()
        val quote = MarketQuoteLogic.parseNifty(json, 2_000L)
        assertThat(quote.kind).isEqualTo(MarketKinds.NIFTY)
        assertThat(quote.label).isEqualTo("NIFTY 50")
        assertThat(quote.price).isWithin(0.01).of(25044.65)
        assertThat(quote.changePercent).isWithin(0.001).of(0.233)
        assertThat(quote.previousPrice).isWithin(0.01).of(25030.3)
        assertThat(quote.source).isEqualTo(MarketKinds.SOURCE_NIFTY)
        assertThat(MarketQuoteLogic.changeDirection(quote.changePercent)).isEqualTo(1)
    }

    @Test
    fun niftyFallsBackToPreviousCloseWhenPercentMissing() {
        val json = """
            {"chart":{"result":[{"meta":{
              "regularMarketPrice":100.0,"chartPreviousClose":80.0
            }}],"error":null}}
        """.trimIndent()
        val quote = MarketQuoteLogic.parseNifty(json, 0L)
        assertThat(quote.changePercent).isWithin(0.01).of(25.0)
        assertThat(quote.previousPrice).isWithin(0.01).of(80.0)
    }

    @Test
    fun niftyPrefersFulldayChangeAsPreviousClose() {
        val json = """
            {"chart":{"result":[{"meta":{
              "regularMarketPrice":25044.65,
              "regularMarketChangePercent":0.233,
              "fulldayChange":58.25,
              "fulldayChangePercent":0.233,
              "chartPreviousClose":25030.3
            }}],"error":null}}
        """.trimIndent()
        val quote = MarketQuoteLogic.parseNifty(json, 0L)
        assertThat(quote.previousPrice).isWithin(0.01).of(24986.40)
        val change = MarketQuoteLogic.dayChange(quote.price, quote.previousPrice)!!
        assertThat(change.amount).isWithin(0.01).of(58.25)
        assertThat(MarketQuoteLogic.formatDayChange(MarketKinds.NIFTY, change))
            .isEqualTo("▲ 58.25 (0.2%) vs yesterday")
    }

    @Test
    fun nseHoursAreWeekdayNineFifteenToThreeThirtyIst() {
        // Tuesday 10 Nov 2026
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 10, 9, 14))).isFalse()
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 10, 9, 15))).isTrue()
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 10, 12, 0))).isTrue()
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 10, 15, 30))).isTrue()
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 10, 15, 31))).isFalse()
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 7, 12, 0))).isFalse() // Saturday
        assertThat(MarketQuoteLogic.isNseOpen(istMs(2026, Calendar.NOVEMBER, 8, 12, 0))).isFalse() // Sunday
    }

    @Test
    fun refreshGatesNiftyToSessionAndGoldToHalfHour() {
        val t = 1_000_000L
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.NIFTY, t, t + 60_000, marketOpen = true, force = false)
        ).isFalse()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.NIFTY, t, t + 91_000, marketOpen = true, force = false)
        ).isTrue()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.NIFTY, t, t + 91_000, marketOpen = false, force = false)
        ).isFalse()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.GOLD, t, t + 29 * 60_000, marketOpen = false, force = false)
        ).isFalse()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.GOLD, t, t + 31 * 60_000, marketOpen = false, force = false)
        ).isTrue()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.NIFTY, t, t + 1_000, marketOpen = false, force = true)
        ).isTrue()
        assertThat(
            MarketQuoteLogic.shouldFetch(MarketKinds.GOLD, 0L, t, marketOpen = true, force = false)
        ).isTrue()
    }

    @Test
    fun indianGroupingAndChips() {
        assertThat(MarketQuoteLogic.formatInr(78450.4)).isEqualTo("₹78,450")
        assertThat(MarketQuoteLogic.formatInr(121337.5)).isEqualTo("₹1,21,338")
        assertThat(MarketQuoteLogic.formatIndex(24312.6)).isEqualTo("24,312.60")
        assertThat(MarketQuoteLogic.formatIndex(25044.65)).isEqualTo("25,044.65")
        assertThat(MarketQuoteLogic.formatChange(0.41)).isEqualTo("▲ 0.41%")
        assertThat(MarketQuoteLogic.formatChange(-0.35)).isEqualTo("▼ 0.35%")
        assertThat(MarketQuoteLogic.changeDirection(0.0)).isEqualTo(0)
        assertThat(MarketQuoteLogic.url(MarketKinds.GOLD)).contains("goodreturns.in")
        assertThat(MarketQuoteLogic.url(MarketKinds.NIFTY)).contains("%5ENSEI")
        assertThat(MarketQuoteLogic.formatInr(13079.4)).isEqualTo("₹13,079")
        val goldUp = DayChange(45.0, 0.4)
        assertThat(MarketQuoteLogic.formatDayChange(MarketKinds.GOLD, goldUp))
            .isEqualTo("▲ ₹45 (0.4%) vs yesterday")
        assertThat(MarketQuoteLogic.formatDayChange(MarketKinds.GOLD, DayChange(-1.0, -0.0076)))
            .isEqualTo("▼ ₹1 (0.01%) vs yesterday")
        assertThat(MarketQuoteLogic.formatDayChange(MarketKinds.GOLD, DayChange(0.0, 0.0)))
            .isEqualTo("₹0 (0.0%) vs yesterday")
        assertThat(MarketQuoteLogic.formatDayChange(MarketKinds.NIFTY, DayChange(-80.5, -0.32)))
            .isEqualTo("▼ 80.50 (0.3%) vs yesterday")
    }

    @Test
    fun dayChangeHidesWhenNoEarlierValue() {
        assertThat(MarketQuoteLogic.dayChange(13079.0, null)).isNull()
        assertThat(MarketQuoteLogic.dayChange(13079.0, 0.0)).isNull()
        val flat = MarketQuoteLogic.dayChange(13079.0, 13079.0)!!
        assertThat(flat.amount).isWithin(0.001).of(0.0)
        assertThat(flat.percent).isWithin(0.001).of(0.0)
        val down = MarketQuoteLogic.dayChange(13079.0, 13080.0)!!
        assertThat(down.amount).isWithin(0.01).of(-1.0)
        assertThat(down.percent).isWithin(0.001).of(-1.0 / 13080.0 * 100.0)
    }

    @Test
    fun istKeyedHistoryComparesWithMostRecentEarlierDay() {
        val nov10 = istMs(2026, Calendar.NOVEMBER, 10, 23, 59)
        val nov11 = istMs(2026, Calendar.NOVEMBER, 11, 0, 0)
        assertThat(MarketQuoteLogic.istDate(nov10)).isEqualTo("2026-11-10")
        assertThat(MarketQuoteLogic.istDate(nov11)).isEqualTo("2026-11-11")
        var history = emptyMap<String, Double>()
        history = MarketQuoteLogic.recordDay(history, "2026-11-09", 13255.0)
        history = MarketQuoteLogic.recordDay(history, "2026-11-10", 13080.0)
        history = MarketQuoteLogic.recordDay(history, "2026-11-11", 13070.0)
        history = MarketQuoteLogic.recordDay(history, "2026-11-11", 13079.0)
        assertThat(history["2026-11-11"]).isWithin(0.01).of(13079.0)
        assertThat(MarketQuoteLogic.previousFromHistory(history, "2026-11-11"))
            .isWithin(0.01).of(13080.0)
        assertThat(MarketQuoteLogic.previousFromHistory(history, "2026-11-10"))
            .isWithin(0.01).of(13255.0)
        assertThat(MarketQuoteLogic.previousFromHistory(history, "2026-11-09")).isNull()
        assertThat(MarketQuoteLogic.previousFromHistory(emptyMap(), "2026-11-11")).isNull()
        val pruned = MarketQuoteLogic.recordDay(
            (1..30).associate { d -> "2026-10-%02d".format(d) to d.toDouble() },
            "2026-11-11",
            13079.0,
            keep = 30
        )
        assertThat(pruned).hasSize(30)
        assertThat(pruned).doesNotContainKey("2026-10-01")
        assertThat(pruned["2026-11-11"]).isWithin(0.01).of(13079.0)
    }

    @Test
    fun attachPreviousPrefersSourceThenPercentThenLocalHistory() {
        val now = istMs(2026, Calendar.NOVEMBER, 11, 8, 5)
        val history = mapOf("2026-11-10" to 13000.0)
        val fromSource = MarketQuote(
            MarketKinds.GOLD, MarketKinds.LABEL_GOLD, 13079.0, null, now,
            MarketKinds.SOURCE_GOLD_RETAIL, previousPrice = 13080.0
        )
        val (sourced, recorded) = MarketQuoteLogic.attachPrevious(fromSource, history, now)
        assertThat(sourced.previousPrice).isWithin(0.01).of(13080.0)
        assertThat(recorded["2026-11-11"]).isWithin(0.01).of(13079.0)
        val fromPercent = fromSource.copy(previousPrice = null, changePercent = -0.01)
        val (implied, _) = MarketQuoteLogic.attachPrevious(fromPercent, emptyMap(), now)
        assertThat(implied.previousPrice!!).isWithin(0.5).of(13080.3)
        val fromHistory = fromSource.copy(previousPrice = null, changePercent = null)
        val (stored, _) = MarketQuoteLogic.attachPrevious(fromHistory, history, now)
        assertThat(stored.previousPrice).isWithin(0.01).of(13000.0)
        val (none, _) = MarketQuoteLogic.attachPrevious(fromHistory, emptyMap(), now)
        assertThat(none.previousPrice).isNull()
        assertThat(none.changePercent).isNull()
        assertThat(MarketQuoteLogic.dayChange(none.price, none.previousPrice)).isNull()
    }

    @Test
    fun syncedTimeAndRefreshFooter() {
        val zone = TimeZone.getTimeZone("GMT")
        val cal = Calendar.getInstance(zone)
        cal.set(2026, Calendar.NOVEMBER, 11, 8, 5, 0)
        cal.set(Calendar.MILLISECOND, 0)
        assertThat(MarketQuoteLogic.formatSynced(cal.timeInMillis, zone, java.util.Locale.US))
            .isEqualTo("Synced 11 Nov, 8:05 AM")
        assertThat(MarketQuoteLogic.formatSynced(0L)).isEmpty()
        assertThat(MarketQuoteLogic.formatFooter("Synced 11 Nov, 8:05 AM", failed = false, spot = false))
            .isEqualTo("Synced 11 Nov, 8:05 AM")
        assertThat(MarketQuoteLogic.formatFooter("Synced 11 Nov, 8:05 AM", failed = true, spot = false))
            .isEqualTo("Couldn't update · Synced 11 Nov, 8:05 AM")
        assertThat(MarketQuoteLogic.formatFooter("Synced 11 Nov, 8:05 AM", failed = false, spot = true))
            .isEqualTo("Synced 11 Nov, 8:05 AM · spot")
        assertThat(MarketQuoteLogic.formatFooter("", failed = true, spot = false))
            .isEqualTo("Couldn't update")
    }

    @Test
    fun cacheRoundTripKeepsNullChangeAndSpotFlag() {
        val gold = MarketQuote(
            MarketKinds.GOLD, MarketKinds.LABEL_GOLD, 13079.0, null, 9L,
            MarketKinds.SOURCE_GOLD_RETAIL, spot = false
        )
        val spot = gold.copy(source = MarketKinds.SOURCE_GOLD_SPOT, spot = true, price = 11169.55)
        val nifty = MarketQuote(
            MarketKinds.NIFTY, MarketKinds.LABEL_NIFTY, 25044.65, 0.233, 9L, MarketKinds.SOURCE_NIFTY,
            previousPrice = 24986.40
        )
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(gold))).isEqualTo(gold)
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(spot))).isEqualTo(spot)
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(nifty))).isEqualTo(nifty)
        assertThat(MarketQuoteStore.parse(null)).isNull()
        assertThat(MarketQuoteStore.parse("not-json")).isNull()
        val legacy = """{"kind":"forge.gold","label":"Gold · 22K · 1g","price":13079.0,"updatedAtMs":9,"source":"goodreturns","spot":false,"changePercent":null}"""
        val parsedLegacy = MarketQuoteStore.parse(legacy)
        assertThat(parsedLegacy?.previousPrice).isNull()
        val days = mapOf("2026-11-10" to 13080.0, "2026-11-11" to 13079.0)
        assertThat(MarketQuoteStore.parseHistory(MarketQuoteStore.serializeHistory(days)))
            .containsExactly("2026-11-10", 13080.0, "2026-11-11", 13079.0)
        assertThat(MarketQuoteStore.parseHistory(null)).isEmpty()
        assertThat(MarketQuoteStore.parseHistory("not-json")).isEmpty()
    }
}
