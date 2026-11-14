package com.pradhul.forge.habits

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.TimeZone
import java.util.UUID

class HabitRepository(
    private val persist: HabitPersist,
    var nowMs: () -> Long = { System.currentTimeMillis() },
    var zone: TimeZone = TimeZone.getDefault()
) {
    private val _state = MutableStateFlow(persist.load())
    val state: StateFlow<HabitSnapshot> = _state

    fun snapshot(): HabitSnapshot = _state.value

    fun today(): String = HabitLogic.today(nowMs(), zone)

    fun add(name: String, emoji: String): Habit? {
        val clean = HabitLogic.validateName(name) ?: return null
        val icon = if (HabitLogic.validateEmoji(emoji)) emoji else HabitLogic.DEFAULT_EMOJI
        val current = snapshot()
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = clean,
            emoji = icon,
            order = current.activeHabits().size,
            createdOn = today()
        )
        commit(current.copy(habits = current.habits + habit))
        return habit
    }

    fun edit(id: String, name: String, emoji: String): Habit? {
        val clean = HabitLogic.validateName(name) ?: return null
        val icon = if (HabitLogic.validateEmoji(emoji)) emoji else HabitLogic.DEFAULT_EMOJI
        val current = snapshot()
        var updated: Habit? = null
        val habits = current.habits.map { habit ->
            if (habit.id == id && habit.active) {
                habit.copy(name = clean, emoji = icon).also { updated = it }
            } else habit
        }
        if (updated == null) return null
        commit(current.copy(habits = habits))
        return updated
    }

    fun delete(id: String): Boolean {
        val current = snapshot()
        val day = today()
        var found = false
        val habits = current.habits.map { habit ->
            if (habit.id == id && habit.active) {
                found = true
                habit.copy(deletedOn = day)
            } else habit
        }
        if (!found) return false
        val remaining = HabitLogic.compactOrders(habits.filter { it.active })
        val merged = habits.map { habit ->
            remaining.firstOrNull { it.id == habit.id } ?: habit
        }
        commit(current.copy(habits = merged))
        return true
    }

    fun reorder(activeIds: List<String>) {
        val current = snapshot()
        val order = activeIds.withIndex().associate { it.value to it.index }
        val habits = current.habits.map { habit ->
            val next = order[habit.id]
            if (next != null) habit.copy(order = next) else habit
        }
        commit(current.copy(habits = habits))
    }

    fun move(id: String, delta: Int) {
        val active = snapshot().activeHabits().toMutableList()
        val i = active.indexOfFirst { it.id == id }
        if (i < 0) return
        val j = (i + delta).coerceIn(0, active.lastIndex)
        if (i == j) return
        val item = active.removeAt(i)
        active.add(j, item)
        reorder(active.map { it.id })
    }

    fun setDone(id: String, done: Boolean, date: String = today()) {
        val current = snapshot()
        val habit = current.habits.firstOrNull { it.id == id && it.existsOn(date) } ?: return
        val check = HabitCheck(habit.id, date)
        val checks = if (done) current.checks + check else current.checks - check
        commit(current.copy(checks = checks))
    }

    fun toggle(id: String, date: String = today()): Boolean {
        val next = !snapshot().isDone(id, date)
        setDone(id, next, date)
        return next
    }

    private fun commit(next: HabitSnapshot) {
        persist.save(next)
        _state.value = next
    }

    companion object {
        @Volatile
        private var instance: HabitRepository? = null

        fun get(context: Context): HabitRepository {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                return HabitRepository(PrefsPersist(context.applicationContext)).also { instance = it }
            }
        }

        fun resetForTests() {
            instance = null
        }
    }
}

class PrefsPersist(context: Context) : HabitPersist {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): HabitSnapshot = HabitStore.parse(prefs.getString(KEY, null))

    override fun save(snapshot: HabitSnapshot) {
        prefs.edit().putString(KEY, HabitStore.serialize(snapshot)).apply()
    }

    companion object {
        const val PREFS = "forge_habits"
        const val KEY = "snapshot"
    }
}
