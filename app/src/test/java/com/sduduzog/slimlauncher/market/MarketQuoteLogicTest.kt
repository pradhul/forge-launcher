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
    fun goldParserUses24kPerGramTimesTenAndHasNoChangeChip() {
        val json = """
            {"currency":"INR","timestamp":"2026-11-10T17:12:03Z",
             "price_gram_24k":"12133.75","price_gram_22k":"11122.61"}
        """.trimIndent()
        val quote = MarketQuoteLogic.parseGold24kPer10g(json, 1_000L)
        assertThat(quote.kind).isEqualTo(MarketKinds.GOLD)
        assertThat(quote.label).isEqualTo("Gold · 24K")
        assertThat(quote.price).isWithin(0.01).of(121337.5)
        assertThat(quote.changePercent).isNull()
        assertThat(quote.source).isEqualTo(MarketKinds.SOURCE_GOLD)
        assertThat(MarketQuoteLogic.changeDirection(quote.changePercent)).isEqualTo(0)
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
        assertThat(MarketQuoteLogic.url(MarketKinds.GOLD)).contains("goldprice.dev")
        assertThat(MarketQuoteLogic.url(MarketKinds.NIFTY)).contains("%5ENSEI")
    }

    @Test
    fun cacheRoundTripKeepsNullChange() {
        val gold = MarketQuote(MarketKinds.GOLD, MarketKinds.LABEL_GOLD, 121338.0, null, 9L, "goldprice.dev")
        val nifty = MarketQuote(MarketKinds.NIFTY, MarketKinds.LABEL_NIFTY, 25044.65, 0.233, 9L, "yahoo")
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(gold))).isEqualTo(gold)
        assertThat(MarketQuoteStore.parse(MarketQuoteStore.serialize(nifty))).isEqualTo(nifty)
        assertThat(MarketQuoteStore.parse(null)).isNull()
        assertThat(MarketQuoteStore.parse("not-json")).isNull()
    }
}
