package com.pradhul.forge.habits

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HabitLogicTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private val utc = TimeZone.getTimeZone("UTC")

    private fun day(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0, zone: TimeZone = ist): Long {
        val cal = Calendar.getInstance(zone)
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun habit(
        id: String,
        name: String,
        createdOn: String,
        deletedOn: String? = null,
        order: Int = 0,
        emoji: String = "💧"
    ) = Habit(id, name, emoji, order, createdOn, deletedOn)

    @Test
    fun todayUsesLocalMidnight() {
        assertThat(HabitLogic.today(day(2026, Calendar.NOVEMBER, 14, 23, 59), ist)).isEqualTo("2026-11-14")
        assertThat(HabitLogic.today(day(2026, Calendar.NOVEMBER, 15, 0, 1), ist)).isEqualTo("2026-11-15")
        val justBeforeIstMidnight = day(2026, Calendar.NOVEMBER, 14, 18, 29, utc)
        assertThat(HabitLogic.today(justBeforeIstMidnight, ist)).isEqualTo("2026-11-14")
        val justAfterIstMidnight = day(2026, Calendar.NOVEMBER, 14, 18, 31, utc)
        assertThat(HabitLogic.today(justAfterIstMidnight, ist)).isEqualTo("2026-11-15")
    }

    @Test
    fun nameValidationRejectsBlankAndOver40() {
        assertThat(HabitLogic.validateName("")).isNull()
        assertThat(HabitLogic.validateName("   ")).isNull()
        assertThat(HabitLogic.canSave("")).isFalse()
        assertThat(HabitLogic.validateName("  Drink water  ")).isEqualTo("Drink water")
        assertThat(HabitLogic.validateName("a".repeat(40))).hasLength(40)
        assertThat(HabitLogic.validateName("a".repeat(41))).isNull()
        assertThat(HabitLogic.validateEmoji("💧")).isTrue()
        assertThat(HabitLogic.validateEmoji("🍕")).isFalse()
        assertThat(HabitLogic.EMOJIS).hasSize(18)
    }

    @Test
    fun mondayFirstWeekAndWidgetDate() {
        assertThat(HabitLogic.mondayOf("2026-11-14", ist)).isEqualTo("2026-11-09")
        assertThat(HabitLogic.weekdayMondayFirst("2026-11-09", ist)).isEqualTo(0)
        assertThat(HabitLogic.weekdayMondayFirst("2026-11-14", ist)).isEqualTo(5)
        assertThat(HabitLogic.weekdayMondayFirst("2026-11-15", ist)).isEqualTo(6)
        assertThat(HabitLogic.widgetDateLabel("2026-11-14", utc, Locale.US)).isEqualTo("Sat, 14 Nov")
    }

    @Test
    fun midWeekAddDoesNotCountEarlierDaysAsMissed() {
        val water = habit("w", "Drink water", "2026-11-09")
        val late = habit("l", "Night walk", "2026-11-12", order = 1)
        val snap = HabitSnapshot(
            listOf(water, late),
            setOf(
                HabitCheck("w", "2026-11-09"),
                HabitCheck("w", "2026-11-10"),
                HabitCheck("w", "2026-11-11"),
                HabitCheck("w", "2026-11-12"),
                HabitCheck("l", "2026-11-12")
            )
        )
        assertThat(HabitLogic.dayRatio(snap, "2026-11-11")).isWithin(0.001).of(1.0)
        assertThat(snap.existingOn("2026-11-11").map { it.id }).containsExactly("w")
        val row = HabitLogic.habitWeekRow(snap, late, "2026-11-09", "2026-11-12", ist)
        assertThat(row.dots.take(3).map { it.state }).containsExactly(
            DotState.FUTURE, DotState.FUTURE, DotState.FUTURE
        )
        assertThat(row.percent).isEqualTo(100)
    }

    @Test
    fun deletedHabitStaysInPastStatsAndLeavesFromThenOn() {
        val water = habit("w", "Drink water", "2026-11-01")
        val sugar = habit("s", "No sugar", "2026-11-01", deletedOn = "2026-11-13", order = 1)
        val snap = HabitSnapshot(
            listOf(water, sugar),
            setOf(
                HabitCheck("w", "2026-11-12"),
                HabitCheck("s", "2026-11-12"),
                HabitCheck("w", "2026-11-13")
            )
        )
        assertThat(snap.existingOn("2026-11-12").map { it.id }).containsExactly("w", "s")
        assertThat(snap.existingOn("2026-11-13").map { it.id }).containsExactly("w")
        assertThat(HabitLogic.dayRatio(snap, "2026-11-12")).isWithin(0.001).of(1.0)
        assertThat(HabitLogic.dayRatio(snap, "2026-11-13")).isWithin(0.001).of(1.0)
        assertThat(snap.activeHabits().map { it.id }).containsExactly("w")
        val week = HabitLogic.weekStats(snap, "2026-11-14", ist)
        assertThat(week.rows.map { it.id }).containsExactly("w")
    }

    @Test
    fun weekPercentLastWeekAndPoints() {
        val h = habit("w", "Water", "2026-11-02")
        val checks = HashSet<HabitCheck>()
        // last week Mon 2 – Sun 8 Nov: 4 of 7 days perfect → ~57%
        listOf("2026-11-02", "2026-11-03", "2026-11-05", "2026-11-07").forEach {
            checks += HabitCheck("w", it)
        }
        // this week Mon 9 – Sat 14: 5 of 6 → 83%
        listOf("2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-14").forEach {
            checks += HabitCheck("w", it)
        }
        val snap = HabitSnapshot(listOf(h), checks)
        val week = HabitLogic.weekStats(snap, "2026-11-14", ist, Locale.US)
        assertThat(week.percent).isEqualTo(83)
        assertThat(week.lastPercent).isEqualTo(57)
        assertThat(week.points).isEqualTo(26)
        assertThat(week.rangeLabel).contains("9")
        assertThat(week.rows[0].percent).isEqualTo(83)
        assertThat(week.rows[0].dots[6].state).isEqualTo(DotState.FUTURE)
        assertThat(week.rows[0].dots[5].state).isEqualTo(DotState.TODAY_DONE)
    }

    @Test
    fun streaksSkipEmptyDaysAndBreakOnMiss() {
        val h = habit("w", "Water", "2026-11-01")
        val checks = setOf(
            HabitCheck("w", "2026-11-10"),
            HabitCheck("w", "2026-11-11"),
            HabitCheck("w", "2026-11-12"),
            HabitCheck("w", "2026-11-14")
        )
        val snap = HabitSnapshot(listOf(h), checks)
        // 13 missed, so current streak is just today (14)
        assertThat(HabitLogic.currentStreak(snap, "2026-11-14", ist)).isEqualTo(1)
        assertThat(HabitLogic.bestStreak(snap, "2026-11-14", ist)).isEqualTo(3)
        val perfectTodayIncomplete = HabitSnapshot(listOf(h), setOf(HabitCheck("w", "2026-11-13"), HabitCheck("w", "2026-11-12")))
        assertThat(HabitLogic.currentStreak(perfectTodayIncomplete, "2026-11-14", ist)).isEqualTo(2)
    }

    @Test
    fun emptyDaysDoNotBreakOrCountStreak() {
        val h = habit("w", "Water", "2026-11-12")
        val snap = HabitSnapshot(listOf(h), setOf(HabitCheck("w", "2026-11-12"), HabitCheck("w", "2026-11-14")))
        // 13 is after createdOn and missed, so streak is 1 (today). 11 is before created — skipped.
        assertThat(HabitLogic.currentStreak(snap, "2026-11-14", ist)).isEqualTo(1)
        val noMiss = HabitSnapshot(listOf(h), setOf(HabitCheck("w", "2026-11-12"), HabitCheck("w", "2026-11-13"), HabitCheck("w", "2026-11-14")))
        assertThat(HabitLogic.currentStreak(noMiss, "2026-11-14", ist)).isEqualTo(3)
        assertThat(HabitLogic.isPerfect(noMiss, "2026-11-11")).isNull()
    }

    @Test
    fun monthHeatmapRatiosAndFutureGrey() {
        val h = habit("w", "Water", "2026-11-01")
        val checks = (1..10).map { HabitCheck("w", "2026-11-%02d".format(it)) }.toSet()
        val snap = HabitSnapshot(listOf(h), checks)
        val month = HabitLogic.monthStats(snap, "2026-11-14", ist, Locale.US)
        assertThat(month.month).isEqualTo(11)
        assertThat(month.year).isEqualTo(2026)
        assertThat(month.leadingBlanks).isEqualTo(6) // 1 Nov 2026 is Sunday, Monday-first
        assertThat(month.days).hasSize(30)
        assertThat(month.days[0].ratio).isWithin(0.001).of(1.0)
        assertThat(month.days[10].ratio).isWithin(0.001).of(0.0) // 11 Nov not done
        assertThat(month.days[13].today).isTrue()
        assertThat(month.days[14].future).isTrue()
        assertThat(month.days[14].ratio).isNull()
        assertThat(month.title).contains("November")
    }

    @Test
    fun todayViewCountsDoneAndHidesDeleted() {
        val snap = HabitSnapshot(
            listOf(
                habit("a", "A", "2026-11-01", order = 1),
                habit("b", "B", "2026-11-01", deletedOn = "2026-11-14", order = 0),
                habit("c", "C", "2026-11-01", order = 0)
            ),
            setOf(HabitCheck("c", "2026-11-14"), HabitCheck("b", "2026-11-14"))
        )
        val view = HabitLogic.todayView(snap, "2026-11-14", ist, Locale.US)
        assertThat(view.rows.map { it.id }).containsExactly("c", "a").inOrder()
        assertThat(view.done).isEqualTo(1)
        assertThat(view.total).isEqualTo(2)
        assertThat(view.dateLabel).isEqualTo("Sat, 14 Nov")
    }

    @Test
    fun contrastMeetsAaOnDarkAndLightPalettes() {
        val dark = HabitPalette.FALLBACK
        assertThat(HabitContrast.contrastRatio(dark.card or 0xFF000000.toInt(), dark.text))
            .isAtLeast(HabitContrast.WCAG_AA)
        val light = HabitPalette(
            background = 0xFFF6F1E8.toInt(),
            backgroundEnd = 0xFFECE4D4.toInt(),
            onBackground = 0xFF1A1208.toInt(),
            accent = 0xFF8A4B12.toInt(),
            onAccent = 0xFFFFF8EE.toInt(),
            darkSurface = false
        )
        assertThat(HabitContrast.contrastRatio(light.card or 0xFF000000.toInt(), light.text))
            .isAtLeast(HabitContrast.WCAG_AA)
    }
}
