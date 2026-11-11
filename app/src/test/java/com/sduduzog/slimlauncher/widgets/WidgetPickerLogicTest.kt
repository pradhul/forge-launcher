package com.sduduzog.slimlauncher.widgets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetPickerLogicTest {
    private fun w(pkg: String, app: String, label: String, cw: Int = 2, ch: Int = 2, serial: Long = 0) =
        PickerWidget("$pkg/$label#$serial", pkg, serial, app, label, cw, ch)

    private val all = listOf(
        w("com.google.clock", "Clock", "Digital clock", 4, 2),
        w("com.spotify", "Spotify", "Player", 4, 1),
        w("com.google.clock", "Clock", "Analog clock", 2, 2),
        w("com.google.weather", "weather", "Forecast", 4, 3),
        w("com.spotify", "Spotify", "Player", 4, 1, serial = 10)
    )

    @Test fun groupsByAppAndProfileSortedAlphabetically() {
        val s = WidgetPickerLogic.group(all)
        assertThat(s.map { it.appLabel to it.profileSerial })
            .containsExactly("Clock" to 0L, "Spotify" to 0L, "Spotify" to 10L, "weather" to 0L).inOrder()
        assertThat(s[0].widgets.map { it.label }).containsExactly("Analog clock", "Digital clock").inOrder()
    }

    @Test fun emptyInputGivesNoSections() =
        assertThat(WidgetPickerLogic.group(emptyList())).isEmpty()

    @Test fun blankQueryKeepsEverything() {
        val s = WidgetPickerLogic.group(all)
        assertThat(WidgetPickerLogic.filter(s, "  ")).isEqualTo(s)
    }

    @Test fun appNameMatchKeepsWholeSection() {
        val f = WidgetPickerLogic.filter(WidgetPickerLogic.group(all), "CLO")
        assertThat(f).hasSize(1)
        assertThat(f[0].widgets).hasSize(2)
    }

    @Test fun widgetNameMatchKeepsOnlyMatchingWidgets() {
        val f = WidgetPickerLogic.filter(WidgetPickerLogic.group(all), "analog")
        assertThat(f.map { it.appLabel }).containsExactly("Clock")
        assertThat(f[0].widgets.map { it.label }).containsExactly("Analog clock")
    }

    @Test fun noMatchDropsSections() =
        assertThat(WidgetPickerLogic.filter(WidgetPickerLogic.group(all), "zzz")).isEmpty()

    @Test fun targetCellsWin() =
        assertThat(WidgetPickerLogic.cells(4, 2, 40, 40)).isEqualTo(4 to 2)

    @Test fun classicFormulaFromMinDp() {
        assertThat(WidgetPickerLogic.cells(0, 0, 250, 110)).isEqualTo(4 to 2) // 4x2 = 250x110dp
        assertThat(WidgetPickerLogic.cells(0, 0, 40, 40)).isEqualTo(1 to 1)
        assertThat(WidgetPickerLogic.cells(0, 0, 180, 180)).isEqualTo(3 to 3)
    }

    @Test fun cellsClampedToGrid() {
        assertThat(WidgetPickerLogic.cells(0, 0, 2000, 3000)).isEqualTo(4 to 6)
        assertThat(WidgetPickerLogic.cells(0, 0, 0, -5)).isEqualTo(1 to 1)
    }

    @Test fun sizeLabelAndCardWidth() {
        assertThat(w("p", "A", "x", 4, 2).sizeLabel).isEqualTo("4×2")
        assertThat(WidgetPickerLogic.cardWidthDp(1)).isEqualTo(132)
        assertThat(WidgetPickerLogic.cardWidthDp(4)).isEqualTo(300)
    }

    @Test fun forgeSectionStaysPinnedAtTopAndMatchesGoldQuery() {
        val forge = WidgetPickerLogic.forgeSection("com.pradhul.forge")
        assertThat(forge.widgets.map { it.label }).containsExactly("Gold · 22K · 1g", "NIFTY 50").inOrder()
        assertThat(forge.widgets.map { it.sizeLabel }).containsExactly("2×1", "2×1")
        val rest = WidgetPickerLogic.group(all)
        val pinned = WidgetPickerLogic.withPinned(forge, rest)
        assertThat(pinned.first().appLabel).isEqualTo("Forge")
        assertThat(pinned.map { it.appLabel }).contains("Clock")
        val gold = WidgetPickerLogic.filter(pinned, "gold")
        assertThat(gold).hasSize(1)
        assertThat(gold[0].widgets.map { it.label }).containsExactly("Gold · 22K · 1g")
        val nifty = WidgetPickerLogic.filter(pinned, "nifty")
        assertThat(nifty[0].widgets.map { it.label }).containsExactly("NIFTY 50")
    }
}
