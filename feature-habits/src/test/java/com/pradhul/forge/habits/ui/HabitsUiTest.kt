package com.pradhul.forge.habits.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.pradhul.forge.habits.BuildConfig
import com.pradhul.forge.habits.HabitLogic
import com.pradhul.forge.habits.HabitPalette
import com.pradhul.forge.habits.HabitRepository
import com.pradhul.forge.habits.Habits
import com.pradhul.forge.habits.MemoryPersist
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private fun repo(): HabitRepository {
        val cal = Calendar.getInstance(ist)
        cal.set(2026, Calendar.NOVEMBER, 14, 10, 50, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = cal.timeInMillis
        return HabitRepository(MemoryPersist(), nowMs = { now }, zone = ist)
    }

    @Test
    fun versionIsIndependentOfLauncher() {
        assertThat(Habits.VERSION).isEqualTo("1.1.0")
        assertThat(BuildConfig.HABITS_VERSION).isEqualTo("1.1.0")
    }

    @Test
    fun newHabitSaveDisabledWhileNameBlank() {
        val repo = repo()
        compose.setContent {
            HabitsApp(palette = HabitPalette.FALLBACK, repo = repo, onClose = {})
        }
        compose.onNodeWithContentDescription("New habit").performClick()
        compose.onNodeWithText("New habit").assertExists()
        compose.onNodeWithContentDescription("Save habit").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save habit").assertIsNotEnabled()
    }

    @Test
    fun toggleEditDeleteFromManageList() {
        val repo = repo()
        repo.add("Drink water", "💧")
        repo.add("Workout", "🏋️")
        compose.setContent {
            HabitsApp(palette = HabitPalette.FALLBACK, repo = repo, onClose = {})
        }
        compose.onNodeWithText("Drink water").assertExists()

        compose.onNodeWithContentDescription("Menu Drink water").performClick()
        compose.onNodeWithText("Edit", substring = true).performClick()
        compose.onNodeWithText("Edit habit").assertExists()
        compose.onNodeWithContentDescription("Save habit").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Menu Drink water").performClick()
        compose.onNodeWithText("Delete", substring = true).performClick()
        compose.onNodeWithText("Delete this habit?").assertExists()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Drink water").assertDoesNotExist()
        assertThat(repo.snapshot().activeHabits().map { it.name }).containsExactly("Workout")
    }

    @Test
    fun widgetToggleUpdatesSharedRepository() {
        val repo = repo()
        val water = repo.add("Drink water", "💧")!!
        var opened = false
        compose.setContent {
            HabitsWidgetContent(
                view = HabitLogic.todayView(repo.snapshot(), repo.today(), repo.zone),
                palette = HabitPalette.FALLBACK,
                onOpen = { opened = true },
                onToggle = { repo.toggle(it) }
            )
        }
        compose.onNodeWithText("Habits · Today").assertExists()
        compose.onNodeWithText("Drink water").assertExists()
        compose.onNodeWithContentDescription("Not done").performClick()
        compose.waitForIdle()
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isTrue()
        compose.onNodeWithText("Open ›").performClick()
        assertThat(opened).isTrue()
    }
}
