package com.sduduzog.slimlauncher.ui.home

import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.models.HomeApp

sealed class HomeSlot {
    data class App(val app: HomeApp) : HomeSlot()
    data class Empty(val key: Int) : HomeSlot()
}

/**
 * Home is one 4-column grid. The bottom row is just more slots — there is
 * no separate dock. Extra apps fill rows above that bottom row.
 */
object HomeGridLogic {
    const val COLUMNS = AppDrawerGrid.PHONE_COLUMNS
    const val MAX = 24
    const val DEFAULT_COUNT = 8

    fun canAdd(currentCount: Int): Boolean = currentCount < MAX

    fun <T> apps(favourites: List<T>): List<T> = favourites.take(MAX)

    /**
     * True only if this exact launcher is in the visible home grid
     * (including the bottom row). Package-only matches are not enough —
     * leftover dock rows and sibling activities must not count.
     */
    fun isOnHome(
        home: List<HomeApp>,
        packageName: String,
        activityName: String,
        userSerial: Long
    ): Boolean {
        return apps(home).any {
            it.packageName == packageName &&
                it.activityName == activityName &&
                it.userSerial == userSerial
        }
    }

    /**
     * Home rows whose launcher is no longer installed. Never wipes the
     * grid when the installed catalog is empty — that is a load failure,
     * not "every app was uninstalled."
     */
    fun stale(home: List<HomeApp>, installed: List<App>): List<HomeApp> {
        if (home.isEmpty() || installed.isEmpty()) return emptyList()
        val live = installed.map { Triple(it.packageName, it.activityName, it.userSerial) }.toSet()
        return home.filter { Triple(it.packageName, it.activityName, it.userSerial) !in live }
    }

    /** Filled cells the home RecyclerView should show. Empty home → empty list. */
    fun displaySlots(home: List<HomeApp>): List<HomeSlot> = visualSlots(apps(home))

    /**
     * Cells for a bottom-anchored 4-column grid. The first [COLUMNS] apps
     * occupy the visual bottom row; anything else sits in rows above, with
     * empty pads so the bottom row stays a full home row.
     */
    fun visualSlots(apps: List<HomeApp>, columns: Int = COLUMNS): List<HomeSlot> {
        val list = apps.take(MAX)
        if (list.size <= columns) return list.map { HomeSlot.App(it) }
        val bottom = list.take(columns).map { HomeSlot.App(it) }
        val above = list.drop(columns)
        val pad = (columns - above.size % columns) % columns
        val pads = List(pad) { HomeSlot.Empty(it) }
        return above.map { HomeSlot.App(it) } + pads + bottom
    }

    /**
     * Persisted order: the last [columns] filled cells in reading order are
     * the bottom home row (first slots). Extra icons sit in the rows above.
     * Empty pads are ignored so a drag that leaves holes still compact back.
     */
    fun logicalFromVisual(slots: List<HomeSlot>, columns: Int = COLUMNS): List<HomeApp> {
        val filled = slots.mapNotNull { (it as? HomeSlot.App)?.app }
        if (filled.size <= columns) return filled
        val aboveCount = filled.size - columns
        return filled.drop(aboveCount) + filled.take(aboveCount)
    }

    fun reorder(slots: List<HomeSlot>, from: Int, to: Int): List<HomeSlot> {
        if (from !in slots.indices || to !in slots.indices || from == to) return slots
        val next = slots.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        return next
    }

    fun withIndices(apps: List<HomeApp>): List<HomeApp> {
        return apps.mapIndexed { index, app -> app.copy(sortingIndex = index) }
    }
}
