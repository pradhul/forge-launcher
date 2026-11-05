package com.sduduzog.slimlauncher.ui.main

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home date line. Locale.ROOT turns `MMM` into CLDR numeric months
 * ("M10" for October); always format with a real language locale.
 */
object HomeDateFormat {
    const val PATTERN = "EEE, MMM d"

    fun format(date: Date, locale: Locale = Locale.getDefault()): String {
        val language = locale.takeUnless {
            it.language.isNullOrBlank() || it == Locale.ROOT
        } ?: Locale.US
        return SimpleDateFormat(PATTERN, language).format(date)
    }
}
