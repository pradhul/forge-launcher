package com.sduduzog.slimlauncher.ui.home

import com.sduduzog.slimlauncher.models.HomeApp

/**
 * Home is one 4-column grid. The bottom row is just more slots — there is
 * no separate dock.
 */
object HomeGridLogic {
    const val COLUMNS = AppDrawerGrid.PHONE_COLUMNS
    const val MAX = 24
    const val DEFAULT_COUNT = 8

    fun canAdd(currentCount: Int): Boolean = currentCount < MAX

    fun <T> apps(favourites: List<T>): List<T> = favourites.take(MAX)

    fun reorder(apps: List<HomeApp>, from: Int, to: Int): List<HomeApp> {
        if (from !in apps.indices || to !in apps.indices || from == to) return apps
        val next = apps.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        return next.mapIndexed { index, app -> app.copy(sortingIndex = index) }
    }
}
