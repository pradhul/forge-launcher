package com.pradhul.forge.habits

/** Public constants. Kept free of Android types so :app unit tests can import them. */
object Habits {
    const val KIND = "forge.habits"
    const val LABEL = "Habits"
    const val CELLS_W = 4
    const val CELLS_H = 2
    const val VERSION = "1.0.0"
    const val DEFAULT_HEIGHT_DP = 360

    fun widgetHeightDp(habitCount: Int): Int = 88 + 16 + habitCount.coerceAtLeast(1) * 56
}
