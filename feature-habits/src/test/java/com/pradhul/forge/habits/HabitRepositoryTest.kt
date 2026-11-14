package com.pradhul.forge.habits

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HabitRepositoryTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private fun repoAt(year: Int, month: Int, day: Int, hour: Int = 10, minute: Int = 0, persist: MemoryPersist = MemoryPersist()): HabitRepository {
        val cal = Calendar.getInstance(ist)
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = cal.timeInMillis
        return HabitRepository(persist, nowMs = { now }, zone = ist)
    }

    @Test
    fun addEditDeleteReorderToggleAndPersist() {
        val persist = MemoryPersist()
        val repo = repoAt(2026, Calendar.NOVEMBER, 14, persist = persist)
        val water = repo.add("  Drink water  ", "💧")!!
        val read = repo.add("Read 20 min", "📖")!!
        val gym = repo.add("Workout", "🏋️")!!
        assertThat(repo.add("   ", "💧")).isNull()
        assertThat(repo.add("x".repeat(41), "💧")).isNull()
        assertThat(repo.snapshot().activeHabits().map { it.name })
            .containsExactly("Drink water", "Read 20 min", "Workout").inOrder()

        repo.edit(read.id, "Read 30 min", "🧘")
        assertThat(repo.snapshot().habits.first { it.id == read.id }.name).isEqualTo("Read 30 min")
        assertThat(repo.snapshot().habits.first { it.id == read.id }.emoji).isEqualTo("🧘")

        repo.reorder(listOf(gym.id, water.id, read.id))
        assertThat(repo.snapshot().activeHabits().map { it.id })
            .containsExactly(gym.id, water.id, read.id).inOrder()
        repo.move(water.id, -1)
        assertThat(repo.snapshot().activeHabits().map { it.id })
            .containsExactly(water.id, gym.id, read.id).inOrder()

        assertThat(repo.toggle(water.id)).isTrue()
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isTrue()
        assertThat(repo.toggle(water.id)).isFalse()
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isFalse()
        repo.setDone(water.id, true)

        assertThat(repo.delete(gym.id)).isTrue()
        val afterDelete = repo.snapshot()
        assertThat(afterDelete.activeHabits().map { it.id }).containsExactly(water.id, read.id).inOrder()
        assertThat(afterDelete.habits.first { it.id == gym.id }.deletedOn).isEqualTo("2026-11-14")

        val reloaded = HabitRepository(persist, nowMs = { repo.nowMs() }, zone = ist)
        assertThat(reloaded.snapshot()).isEqualTo(repo.snapshot())
        assertThat(HabitStore.parse(HabitStore.serialize(repo.snapshot()))).isEqualTo(repo.snapshot())
    }

    @Test
    fun dayRolloverClearsTodayWithoutLosingYesterday() {
        val persist = MemoryPersist()
        var now = dayMs(2026, Calendar.NOVEMBER, 14, 23, 40)
        val repo = HabitRepository(persist, nowMs = { now }, zone = ist)
        val water = repo.add("Drink water", "💧")!!
        repo.toggle(water.id)
        assertThat(repo.today()).isEqualTo("2026-11-14")
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isTrue()

        now = dayMs(2026, Calendar.NOVEMBER, 15, 0, 5)
        assertThat(repo.today()).isEqualTo("2026-11-15")
        assertThat(repo.snapshot().isDone(water.id, "2026-11-15")).isFalse()
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isTrue()
        val view = HabitLogic.todayView(repo.snapshot(), repo.today(), ist)
        assertThat(view.done).isEqualTo(0)
        assertThat(view.total).isEqualTo(1)
    }

    @Test
    fun storeIgnoresCorruptPayload() {
        assertThat(HabitStore.parse(null).habits).isEmpty()
        assertThat(HabitStore.parse("not-json").habits).isEmpty()
        val snap = HabitSnapshot(
            listOf(Habit("1", "A", "💧", 0, "2026-11-01", null)),
            setOf(HabitCheck("1", "2026-11-01"))
        )
        assertThat(HabitStore.parse(HabitStore.serialize(snap))).isEqualTo(snap)
        assertThat(HabitPalette.fromJson(HabitPalette.FALLBACK.toJson()).accent)
            .isEqualTo(HabitPalette.FALLBACK.accent)
        assertThat(Habits.VERSION).isEqualTo("1.0.0")
        assertThat(Habits.LABEL).isEqualTo("Habits")
        assertThat(Habits.KIND).isEqualTo("forge.habits")
    }

    private fun dayMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(ist)
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
