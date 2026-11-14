package com.pradhul.forge.habits

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object HabitLogic {
    const val NAME_MAX = 40
    const val VERSION = "1.1.0"

    val EMOJIS = listOf(
        "💧", "📖", "🏋️", "🧘", "🍬", "🚶",
        "🛏️", "🥗", "✍️", "🎸", "💊", "🌱",
        "🚭", "☀️", "🧹", "💻", "🙏", "🚴"
    )

    const val DEFAULT_EMOJI = "💧"

    fun today(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String = formatDay(nowMs, zone)

    fun formatDay(nowMs: Long, zone: TimeZone): String {
        val cal = calendar(nowMs, zone)
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun widgetDateLabel(date: String, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()): String {
        val cal = parse(date, zone) ?: return date
        val fmt = java.text.SimpleDateFormat("EEE, d MMM", locale)
        fmt.timeZone = zone
        return fmt.format(cal.time)
    }

    fun validateName(raw: String): String? {
        val name = raw.trim()
        if (name.isEmpty()) return null
        if (name.length > NAME_MAX) return null
        return name
    }

    fun canSave(raw: String): Boolean = validateName(raw) != null

    fun validateEmoji(emoji: String): Boolean = EMOJIS.contains(emoji)

    fun addDays(date: String, days: Int, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = parse(date, zone) ?: error("bad date $date")
        cal.add(Calendar.DAY_OF_MONTH, days)
        return formatDay(cal.timeInMillis, zone)
    }

    fun mondayOf(date: String, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = parse(date, zone) ?: error("bad date $date")
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val sinceMonday = (dow + 5) % 7 // Mon=0 … Sun=6
        cal.add(Calendar.DAY_OF_MONTH, -sinceMonday)
        return formatDay(cal.timeInMillis, zone)
    }

    fun weekdayMondayFirst(date: String, zone: TimeZone = TimeZone.getDefault()): Int {
        val cal = parse(date, zone) ?: return 0
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    fun daysInclusive(start: String, end: String, zone: TimeZone = TimeZone.getDefault()): List<String> {
        if (start > end) return emptyList()
        val out = ArrayList<String>()
        var cursor = start
        var guard = 0
        while (cursor <= end && guard < 400) {
            out += cursor
            cursor = addDays(cursor, 1, zone)
            guard++
        }
        return out
    }

    fun isPerfect(snapshot: HabitSnapshot, date: String): Boolean? {
        val existing = snapshot.existingOn(date)
        if (existing.isEmpty()) return null
        return existing.all { snapshot.isDone(it.id, date) }
    }

    fun dayRatio(snapshot: HabitSnapshot, date: String): Double? {
        val existing = snapshot.existingOn(date)
        if (existing.isEmpty()) return null
        val done = existing.count { snapshot.isDone(it.id, date) }
        return done.toDouble() / existing.size
    }

    fun periodPercent(
        snapshot: HabitSnapshot,
        start: String,
        end: String,
        zone: TimeZone = TimeZone.getDefault()
    ): Int? {
        val ratios = daysInclusive(start, end, zone).mapNotNull { dayRatio(snapshot, it) }
        if (ratios.isEmpty()) return null
        return (ratios.average() * 100.0).roundToInt()
    }

    fun currentStreak(snapshot: HabitSnapshot, today: String, zone: TimeZone = TimeZone.getDefault()): Int {
        var date = today
        val first = isPerfect(snapshot, date)
        if (first == false) date = addDays(date, -1, zone)
        var streak = 0
        val earliest = snapshot.habits.minOfOrNull { it.createdOn } ?: return 0
        var guard = 0
        while (date >= earliest && guard < 4000) {
            when (isPerfect(snapshot, date)) {
                true -> streak++
                false -> return streak
                null -> { /* skip empty days */ }
            }
            date = addDays(date, -1, zone)
            guard++
        }
        return streak
    }

    fun bestStreak(snapshot: HabitSnapshot, today: String, zone: TimeZone = TimeZone.getDefault()): Int {
        val earliest = snapshot.habits.minOfOrNull { it.createdOn } ?: return 0
        var best = 0
        var run = 0
        for (date in daysInclusive(earliest, today, zone)) {
            when (isPerfect(snapshot, date)) {
                true -> {
                    run++
                    if (run > best) best = run
                }
                false -> run = 0
                null -> { }
            }
        }
        return best
    }

    fun weekStats(
        snapshot: HabitSnapshot,
        today: String,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): WeekStats {
        val monday = mondayOf(today, zone)
        val sunday = addDays(monday, 6, zone)
        val lastMonday = addDays(monday, -7, zone)
        val lastSunday = addDays(monday, -1, zone)
        val percent = periodPercent(snapshot, monday, today, zone)
        val lastPercent = periodPercent(snapshot, lastMonday, lastSunday, zone)
        val points = if (percent != null && lastPercent != null) percent - lastPercent else null
        val rows = snapshot.activeHabits().map { habit ->
            habitWeekRow(snapshot, habit, monday, today, zone)
        }
        return WeekStats(
            percent = percent,
            lastPercent = lastPercent,
            points = points,
            rangeLabel = rangeLabel(monday, minOf(today, sunday), zone, locale),
            lastRangeLabel = rangeLabel(lastMonday, lastSunday, zone, locale),
            currentStreak = currentStreak(snapshot, today, zone),
            bestStreak = bestStreak(snapshot, today, zone),
            rows = rows
        )
    }

    fun habitWeekRow(
        snapshot: HabitSnapshot,
        habit: Habit,
        monday: String,
        today: String,
        zone: TimeZone
    ): HabitWeekRow {
        val dots = (0..6).map { offset ->
            val date = addDays(monday, offset, zone)
            val exists = habit.existsOn(date)
            val future = date > today
            val isToday = date == today
            val done = exists && snapshot.isDone(habit.id, date)
            val state = when {
                future || !exists -> DotState.FUTURE
                isToday && done -> DotState.TODAY_DONE
                isToday -> DotState.TODAY_MISSED
                done -> DotState.DONE
                else -> DotState.MISSED
            }
            DayDot(date, offset, state)
        }
        val countable = dots.filter { it.date <= today && habit.existsOn(it.date) }
        val percent = if (countable.isEmpty()) 0
        else (countable.count { snapshot.isDone(habit.id, it.date) } * 100.0 / countable.size).roundToInt()
        return HabitWeekRow(habit.id, habit.emoji, habit.name, dots, percent)
    }

    fun monthStats(
        snapshot: HabitSnapshot,
        today: String,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): MonthStats {
        val cal = parse(today, zone) ?: error("bad date $today")
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val first = formatDay(cal.timeInMillis, zone)
        val leading = weekdayMondayFirst(first, zone)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val days = (1..lastDay).map { day ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            MonthDay(
                date = date,
                dayOfMonth = day,
                ratio = if (date > today) null else dayRatio(snapshot, date),
                future = date > today,
                today = date == today
            )
        }
        val titleFmt = java.text.SimpleDateFormat("MMMM yyyy", locale)
        titleFmt.timeZone = zone
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return MonthStats(
            title = "${titleFmt.format(cal.time)} · Month view",
            year = year,
            month = month + 1,
            leadingBlanks = leading,
            days = days
        )
    }

    fun todayView(
        snapshot: HabitSnapshot,
        today: String,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): TodayView {
        val rows = snapshot.activeHabits().map {
            TodayRow(it.id, it.emoji, it.name, snapshot.isDone(it.id, today))
        }
        return TodayView(
            dateLabel = widgetDateLabel(today, zone, locale),
            done = rows.count { it.done },
            total = rows.size,
            rows = rows
        )
    }

    fun rangeLabel(start: String, end: String, zone: TimeZone, locale: Locale): String {
        val a = parse(start, zone) ?: return start
        val b = parse(end, zone) ?: return end
        val day = java.text.SimpleDateFormat("d", locale).apply { timeZone = zone }
        val dayMonth = java.text.SimpleDateFormat("d MMM", locale).apply { timeZone = zone }
        return if (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
        ) {
            "${day.format(a.time)}–${dayMonth.format(b.time)}"
        } else {
            "${dayMonth.format(a.time)} – ${dayMonth.format(b.time)}"
        }
    }

    fun compactOrders(habits: List<Habit>): List<Habit> =
        habits.mapIndexed { index, habit -> habit.copy(order = index) }

    fun newId(nowMs: Long): String = "h_$nowMs"

    private fun calendar(nowMs: Long, zone: TimeZone): Calendar {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        return cal
    }

    private fun parse(date: String, zone: TimeZone): Calendar? {
        val bits = date.split('-')
        if (bits.size != 3) return null
        val y = bits[0].toIntOrNull() ?: return null
        val m = bits[1].toIntOrNull() ?: return null
        val d = bits[2].toIntOrNull() ?: return null
        val cal = Calendar.getInstance(zone)
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
