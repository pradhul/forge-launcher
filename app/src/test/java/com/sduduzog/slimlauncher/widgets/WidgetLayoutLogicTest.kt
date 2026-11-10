package com.sduduzog.slimlauncher.widgets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetLayoutLogicTest {
    private val base = listOf(WidgetEntry(1, 180), WidgetEntry(2, 240), WidgetEntry(3, 120))

    @Test
    fun serializeRoundTripSurvivesRestart() {
        val raw = WidgetLayoutLogic.serialize(base)
        assertThat(WidgetLayoutLogic.parse(raw)).isEqualTo(base)
        assertThat(WidgetLayoutLogic.parse(null)).isEmpty()
        assertThat(WidgetLayoutLogic.parse("garbage;;5:x;7:9999")).containsExactly(
            WidgetEntry(5, WidgetLayoutLogic.DEFAULT_HEIGHT_DP), WidgetEntry(7, WidgetLayoutLogic.MAX_HEIGHT_DP)
        ).inOrder()
    }

    @Test
    fun moveUpDownAndBounds() {
        assertThat(WidgetLayoutLogic.move(base, 2, -1).map { it.id }).containsExactly(2, 1, 3).inOrder()
        assertThat(WidgetLayoutLogic.move(base, 2, 1).map { it.id }).containsExactly(1, 3, 2).inOrder()
        assertThat(WidgetLayoutLogic.move(base, 1, -1)).isEqualTo(base)
        assertThat(WidgetLayoutLogic.move(base, 3, 1)).isEqualTo(base)
    }

    @Test
    fun resizeClampsAndRemoveDrops() {
        assertThat(WidgetLayoutLogic.resize(base, 1, 10)[0].heightDp).isEqualTo(WidgetLayoutLogic.MIN_HEIGHT_DP)
        assertThat(WidgetLayoutLogic.resize(base, 1, 5000)[0].heightDp).isEqualTo(WidgetLayoutLogic.MAX_HEIGHT_DP)
        assertThat(WidgetLayoutLogic.remove(base, 2).map { it.id }).containsExactly(1, 3).inOrder()
    }

    @Test
    fun addAppendsWithoutDuplicatesAndPrunesDeadWidgets() {
        val added = WidgetLayoutLogic.add(base, WidgetEntry(4, 200))
        assertThat(added.last().id).isEqualTo(4)
        assertThat(WidgetLayoutLogic.add(added, WidgetEntry(4, 300)).count { it.id == 4 }).isEqualTo(1)
        assertThat(WidgetLayoutLogic.prune(base) { it != 2 }.map { it.id }).containsExactly(1, 3)
        assertThat(WidgetLayoutLogic.defaultHeight(0)).isEqualTo(WidgetLayoutLogic.DEFAULT_HEIGHT_DP)
    }

    @Test
    fun builtinEntriesSurviveSerializeAndAreNotPrunedWithHostedWidgets() {
        val gold = WidgetEntry(-1, 112, "forge.gold")
        val hosted = WidgetEntry(9, 180)
        val raw = WidgetLayoutLogic.serialize(listOf(gold, hosted))
        assertThat(raw).contains("forge.gold")
        assertThat(WidgetLayoutLogic.parse(raw)).containsExactly(gold, hosted).inOrder()
        assertThat(WidgetLayoutLogic.parse("9:180")).containsExactly(hosted)
        assertThat(WidgetLayoutLogic.prune(listOf(gold, hosted)) { false }).containsExactly(gold)
        assertThat(WidgetLayoutLogic.nextSyntheticId(listOf(hosted))).isEqualTo(-1)
        assertThat(WidgetLayoutLogic.nextSyntheticId(listOf(gold, hosted))).isEqualTo(-2)
    }

    @Test
    fun pageSwipeNeedsClearHorizontalMove() {
        val d = 2.75f
        assertThat(PageSwipeLogic.opensWidgets(-70f * d, 5f, d)).isTrue()
        assertThat(PageSwipeLogic.opensWidgets(70f * d, 5f, d)).isFalse()
        // diagonal / vertical (drawer, scrolling) never page
        assertThat(PageSwipeLogic.opensWidgets(-70f * d, -60f * d, d)).isFalse()
        assertThat(PageSwipeLogic.returnsHome(70f * d, 0f, d, editing = false)).isTrue()
        assertThat(PageSwipeLogic.returnsHome(70f * d, 0f, d, editing = true)).isFalse()
        assertThat(PageSwipeLogic.returnsHome(30f * d, 0f, d, editing = false)).isFalse()
    }
}
