package com.sduduzog.slimlauncher.widgets

import kotlin.math.abs

/** One placed widget: AppWidget id + card height in dp. */
data class WidgetEntry(val id: Int, val heightDp: Int)

/** Pure list operations + persistence format for the widgets page. */
object WidgetLayoutLogic {
    const val MIN_HEIGHT_DP = 72
    const val MAX_HEIGHT_DP = 640
    const val DEFAULT_HEIGHT_DP = 180

    fun clampHeight(dp: Int): Int = dp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)

    fun defaultHeight(minHeightDp: Int): Int =
        clampHeight(if (minHeightDp <= 0) DEFAULT_HEIGHT_DP else minHeightDp + 16)

    fun add(list: List<WidgetEntry>, entry: WidgetEntry): List<WidgetEntry> =
        list.filterNot { it.id == entry.id } + entry.copy(heightDp = clampHeight(entry.heightDp))

    fun remove(list: List<WidgetEntry>, id: Int): List<WidgetEntry> = list.filterNot { it.id == id }

    fun move(list: List<WidgetEntry>, id: Int, delta: Int): List<WidgetEntry> {
        val i = list.indexOfFirst { it.id == id }
        if (i < 0) return list
        val j = (i + delta).coerceIn(0, list.size - 1)
        if (i == j) return list
        val out = list.toMutableList()
        val item = out.removeAt(i)
        out.add(j, item)
        return out
    }

    fun resize(list: List<WidgetEntry>, id: Int, heightDp: Int): List<WidgetEntry> =
        list.map { if (it.id == id) it.copy(heightDp = clampHeight(heightDp)) else it }

    /** Drops entries whose widget no longer exists (uninstalled provider). */
    fun prune(list: List<WidgetEntry>, alive: (Int) -> Boolean): List<WidgetEntry> = list.filter { alive(it.id) }

    fun serialize(list: List<WidgetEntry>): String = list.joinToString(";") { "${it.id}:${it.heightDp}" }

    fun parse(raw: String?): List<WidgetEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { part ->
            val bits = part.split(':')
            val id = bits.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val h = bits.getOrNull(1)?.toIntOrNull() ?: DEFAULT_HEIGHT_DP
            WidgetEntry(id, clampHeight(h))
        }.distinctBy { it.id }
    }
}

/**
 * Horizontal page swipe (home <-> widgets). Needs a long, clearly
 * horizontal move so vertical scrolling and widget taps keep working.
 */
object PageSwipeLogic {
    const val COMMIT_DP = 56f
    const val DOMINANCE = 1.8f

    fun isHorizontal(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy) * DOMINANCE

    /** Home -> widgets: finger moves left. */
    fun opensWidgets(dx: Float, dy: Float, density: Float): Boolean =
        -dx > COMMIT_DP * density && isHorizontal(dx, dy)

    /** Widgets -> home: finger moves right. */
    fun returnsHome(dx: Float, dy: Float, density: Float, editing: Boolean): Boolean =
        !editing && dx > COMMIT_DP * density && isHorizontal(dx, dy)
}
