package com.pradhul.forge.habits

import org.json.JSONArray
import org.json.JSONObject

interface HabitPersist {
    fun load(): HabitSnapshot
    fun save(snapshot: HabitSnapshot)
}

object HabitStore {
    fun serialize(snapshot: HabitSnapshot): String {
        val habits = JSONArray()
        for (habit in snapshot.habits) {
            val obj = JSONObject()
                .put("id", habit.id)
                .put("name", habit.name)
                .put("emoji", habit.emoji)
                .put("order", habit.order)
                .put("createdOn", habit.createdOn)
            if (habit.deletedOn != null) obj.put("deletedOn", habit.deletedOn)
            else obj.put("deletedOn", JSONObject.NULL)
            habits.put(obj)
        }
        val checks = JSONArray()
        for (check in snapshot.checks) {
            checks.put(
                JSONObject()
                    .put("habitId", check.habitId)
                    .put("date", check.date)
            )
        }
        return JSONObject()
            .put("habits", habits)
            .put("checks", checks)
            .toString()
    }

    fun parse(raw: String?): HabitSnapshot {
        if (raw.isNullOrBlank()) return HabitSnapshot()
        return try {
            val obj = JSONObject(raw)
            val habits = ArrayList<Habit>()
            val habitArr = obj.optJSONArray("habits") ?: JSONArray()
            for (i in 0 until habitArr.length()) {
                val h = habitArr.getJSONObject(i)
                val deleted = if (h.has("deletedOn") && !h.isNull("deletedOn")) h.getString("deletedOn") else null
                habits += Habit(
                    id = h.getString("id"),
                    name = h.getString("name"),
                    emoji = h.getString("emoji"),
                    order = h.getInt("order"),
                    createdOn = h.getString("createdOn"),
                    deletedOn = deleted
                )
            }
            val checks = HashSet<HabitCheck>()
            val checkArr = obj.optJSONArray("checks") ?: JSONArray()
            for (i in 0 until checkArr.length()) {
                val c = checkArr.getJSONObject(i)
                checks += HabitCheck(c.getString("habitId"), c.getString("date"))
            }
            HabitSnapshot(habits, checks)
        } catch (_: Exception) {
            HabitSnapshot()
        }
    }
}

class MemoryPersist(initial: HabitSnapshot = HabitSnapshot()) : HabitPersist {
    var raw: String? = HabitStore.serialize(initial)
    override fun load(): HabitSnapshot = HabitStore.parse(raw)
    override fun save(snapshot: HabitSnapshot) {
        raw = HabitStore.serialize(snapshot)
    }
}
