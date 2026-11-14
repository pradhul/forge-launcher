package com.pradhul.forge.habits.ui

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitMotionTest {
    @Test
    fun animatorDurationScaleZeroDisablesMotion() {
        assertThat(HabitMotion.enabled(0f)).isFalse()
        assertThat(HabitMotion.enabled(1f)).isTrue()
        assertThat(HabitMotion.enabled(0.5f)).isTrue()
        assertThat(HabitMotion.millis(240, enabled = false)).isEqualTo(0)
        assertThat(HabitMotion.millis(240, enabled = true)).isEqualTo(240)
        assertThat(HabitMotion.staggerDelayMs(10, enabled = false)).isEqualTo(0)
        assertThat(HabitMotion.staggerDelayMs(10, enabled = true)).isEqualTo(160)
        assertThat(HabitMotion.PRESS_SCALE).isWithin(0.001f).of(0.96f)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitsImeTest {
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
    fun habitsActivityUsesAdjustResizeAndEdgeToEdge() {
        val activity = Robolectric.buildActivity(HabitsActivity::class.java).setup().get()
        val windowMode = activity.window.attributes.softInputMode
        assertThat(windowMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
            .isEqualTo(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val info = activity.packageManager.getActivityInfo(activity.componentName, 0)
        assertThat(info.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
            .isEqualTo(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        assertThat(activity.window.decorView.fitsSystemWindows).isFalse()
    }

    @Test
    fun addSheetKeepsNameCounterAndSaveVisible() {
        compose.setContent {
            HabitsApp(palette = HabitPalette.FALLBACK, repo = repo(), onClose = {})
        }
        compose.onNodeWithContentDescription("New habit").performClick()
        compose.onNodeWithText("New habit").assertIsDisplayed()
        compose.onNodeWithContentDescription("Habit name").assertIsDisplayed()
        compose.onNodeWithText("0/${HabitLogic.NAME_MAX}").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save habit").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save habit").assertIsNotEnabled()
    }

    @Test
    fun editSheetKeepsNameCounterSaveAndDeleteVisible() {
        val repo = repo()
        repo.add("Drink water", "💧")
        compose.setContent {
            HabitsApp(palette = HabitPalette.FALLBACK, repo = repo, onClose = {})
        }
        compose.onNodeWithContentDescription("Menu Drink water").performClick()
        compose.onNodeWithText("Edit", substring = true).performClick()
        compose.onNodeWithText("Edit habit").assertIsDisplayed()
        compose.onNodeWithContentDescription("Habit name").assertIsDisplayed()
        compose.onNodeWithText("${"Drink water".length}/${HabitLogic.NAME_MAX}").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save habit").assertIsDisplayed()
        compose.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun reducedMotionStillTogglesAndSaves() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertThat(
            HabitMotion.enabled(
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            )
        ).isFalse()

        val repo = repo()
        val water = repo.add("Drink water", "💧")!!
        compose.setContent {
            HabitsWidgetContent(
                view = HabitLogic.todayView(repo.snapshot(), repo.today(), repo.zone),
                palette = HabitPalette.FALLBACK,
                onOpen = {},
                onToggle = { repo.toggle(it) }
            )
        }
        compose.onNodeWithContentDescription("Not done").performClick()
        compose.waitForIdle()
        assertThat(repo.snapshot().isDone(water.id, "2026-11-14")).isTrue()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitsVersionTest {
    @Test
    fun versionIs110() {
        assertThat(Habits.VERSION).isEqualTo("1.1.0")
        assertThat(BuildConfig.HABITS_VERSION).isEqualTo("1.1.0")
        assertThat(HabitLogic.VERSION).isEqualTo("1.1.0")
    }
}
