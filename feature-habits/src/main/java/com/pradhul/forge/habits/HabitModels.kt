package com.pradhul.forge.habits

data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val order: Int,
    val createdOn: String,
    val deletedOn: String? = null
) {
    fun existsOn(date: String): Boolean =
        createdOn <= date && (deletedOn == null || date < deletedOn)

    val active: Boolean get() = deletedOn == null
}

data class HabitCheck(
    val habitId: String,
    val date: String
)

data class HabitSnapshot(
    val habits: List<Habit> = emptyList(),
    val checks: Set<HabitCheck> = emptySet()
) {
    fun activeHabits(): List<Habit> = habits.filter { it.active }.sortedBy { it.order }

    fun existingOn(date: String): List<Habit> =
        habits.filter { it.existsOn(date) }.sortedBy { it.order }

    fun isDone(habitId: String, date: String): Boolean =
        checks.contains(HabitCheck(habitId, date))
}

data class WeekStats(
    val percent: Int?,
    val lastPercent: Int?,
    val points: Int?,
    val rangeLabel: String,
    val lastRangeLabel: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val rows: List<HabitWeekRow>
)

data class HabitWeekRow(
    val id: String,
    val emoji: String,
    val name: String,
    val dots: List<DayDot>,
    val percent: Int
)

data class DayDot(
    val date: String,
    val weekday: Int,
    val state: DotState
)

enum class DotState { FUTURE, EMPTY, MISSED, DONE, TODAY_DONE, TODAY_MISSED }

data class MonthStats(
    val title: String,
    val year: Int,
    val month: Int,
    val leadingBlanks: Int,
    val days: List<MonthDay>
)

data class MonthDay(
    val date: String,
    val dayOfMonth: Int,
    val ratio: Double?,
    val future: Boolean,
    val today: Boolean
)

data class TodayView(
    val dateLabel: String,
    val done: Int,
    val total: Int,
    val rows: List<TodayRow>
)

data class TodayRow(
    val id: String,
    val emoji: String,
    val name: String,
    val done: Boolean
)
