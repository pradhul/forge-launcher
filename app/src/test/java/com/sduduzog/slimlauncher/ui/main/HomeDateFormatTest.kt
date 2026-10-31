package com.sduduzog.slimlauncher.ui.main

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeDateFormatTest {

    @Test
    fun octoberThirtyFirstIsLocalizedMonthNameNotNumericRoot() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
        calendar.set(2026, Calendar.OCTOBER, 31, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val formatted = HomeDateFormat.format(calendar.time, Locale.US)
        assertThat(formatted).isEqualTo("Sat, Oct 31")
        assertThat(formatted).doesNotContain("M10")
    }

    @Test
    fun rootLocaleFallsBackToUsMonthName() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(2026, Calendar.OCTOBER, 31, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val formatted = HomeDateFormat.format(calendar.time, Locale.ROOT)
        assertThat(formatted).isEqualTo("Sat, Oct 31")
    }

    @Test
    fun patternIsWeekdayShortMonthDay() {
        assertThat(HomeDateFormat.PATTERN).isEqualTo("EEE, MMM d")
    }
}
