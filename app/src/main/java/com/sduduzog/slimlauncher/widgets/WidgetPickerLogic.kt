package com.sduduzog.slimlauncher.widgets

import com.sduduzog.slimlauncher.market.MarketKinds
import kotlin.math.ceil

/** One installable widget provider, platform-free so it can be unit-tested. */
data class PickerWidget(
    val key: String,
    val packageName: String,
    val profileSerial: Long,
    val appLabel: String,
    val label: String,
    val cellsW: Int,
    val cellsH: Int
) {
    val sizeLabel: String get() = WidgetPickerLogic.sizeLabel(cellsW, cellsH)
}

data class PickerSection(
    val packageName: String,
    val profileSerial: Long,
    val appLabel: String,
    val widgets: List<PickerWidget>
) {
    val key: String get() = "$packageName#$profileSerial"
}

object WidgetPickerLogic {
    const val MAX_COLS = 4
    const val MAX_ROWS = 6

    /** Sections per (app, profile), apps A→Z, widgets A→Z then by size. */
    fun group(widgets: List<PickerWidget>): List<PickerSection> =
        widgets.groupBy { it.packageName to it.profileSerial }
            .map { (k, list) ->
                PickerSection(
                    k.first, k.second, list.first().appLabel,
                    list.sortedWith(compareBy<PickerWidget>({ it.label.lowercase() }, { it.cellsW * it.cellsH }))
                )
            }
            .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.profileSerial }))

    /** Keep a launcher-owned section at the top of the picker. */
    fun withPinned(pinned: PickerSection?, rest: List<PickerSection>): List<PickerSection> {
        if (pinned == null || pinned.widgets.isEmpty()) return rest
        return listOf(pinned) + rest.filterNot { it.key == pinned.key }
    }

    fun forgeSection(packageName: String, profileSerial: Long = 0L): PickerSection {
        val widgets = listOf(
            PickerWidget(
                MarketKinds.GOLD, packageName, profileSerial,
                MarketKinds.APP_LABEL, MarketKinds.LABEL_GOLD, 2, 1
            ),
            PickerWidget(
                MarketKinds.NIFTY, packageName, profileSerial,
                MarketKinds.APP_LABEL, MarketKinds.LABEL_NIFTY, 2, 1
            )
        )
        return PickerSection(packageName, profileSerial, MarketKinds.APP_LABEL, widgets)
    }

    /**
     * App-name match keeps the whole section; otherwise keep only the widgets
     * whose label matches. Blank query keeps everything.
     */
    fun filter(sections: List<PickerSection>, query: String): List<PickerSection> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return sections
        return sections.mapNotNull { s ->
            when {
                s.appLabel.lowercase().contains(q) -> s
                else -> s.widgets.filter { it.label.lowercase().contains(q) }
                    .takeIf { it.isNotEmpty() }?.let { s.copy(widgets = it) }
            }
        }
    }

    /**
     * Size in launcher cells. API 31+ targetCell* wins when set; otherwise the
     * classic formula: n cells = 70n - 30 dp  →  n = ceil((dp + 30) / 70).
     */
    fun cells(targetCellW: Int, targetCellH: Int, minWidthDp: Int, minHeightDp: Int): Pair<Int, Int> {
        val w = if (targetCellW > 0) targetCellW else fromDp(minWidthDp)
        val h = if (targetCellH > 0) targetCellH else fromDp(minHeightDp)
        return w.coerceIn(1, MAX_COLS) to h.coerceIn(1, MAX_ROWS)
    }

    fun fromDp(dp: Int): Int = if (dp <= 0) 1 else ceil((dp + 30) / 70.0).toInt()

    fun sizeLabel(w: Int, h: Int): String = "$w×$h"

    /** Card width in dp for a preview of [cellsW] columns, kept in a readable range. */
    fun cardWidthDp(cellsW: Int): Int = (72 * cellsW + 40).coerceIn(132, 300)
}
