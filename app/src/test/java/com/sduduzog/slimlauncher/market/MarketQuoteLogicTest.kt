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
            MarketKinds.NIFTY, MarketKinds.LABEL_NIFTY, 25044.65, 0.233, 9L, MarketKinds.SOURCE_NIFTY
        )
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(gold))).isEqualTo(gold)
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(spot))).isEqualTo(spot)
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(nifty))).isEqualTo(nifty)
        assertThat(MarketQuoteStore.parse(null)).isNull()
        assertThat(MarketQuoteStore.parse("not-json")).isNull()
    }
}
