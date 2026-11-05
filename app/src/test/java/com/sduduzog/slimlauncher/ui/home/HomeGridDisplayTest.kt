package com.sduduzog.slimlauncher.ui.home

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.data.BaseDatabase
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.Repository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class HomeGridDisplayTest {

    @get:Rule
    val instant = InstantTaskExecutorRule()

    private lateinit var db: BaseDatabase
    private lateinit var repository: Repository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaseDatabase::class.java
        )
            .addMigrations(*BaseDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        repository = Repository(db.baseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun pinThenReadShowsAFilledHomeSlot() {
        assertThat(HomeGridLogic.displaySlots(repository.getAppsSync())).isEmpty()
        repository.add(HomeApp("Chrome", "com.android.chrome", "Main", 0, null, 0L))
        val slots = HomeGridLogic.displaySlots(repository.getAppsSync())
        assertThat(slots).isNotEmpty()
        assertThat(slots.filterIsInstance<HomeSlot.App>().map { it.app.appName })
            .containsExactly("Chrome")
        repository.add(HomeApp("Phone", "com.android.dialer", "Dialtacts", 1, null, 0L))
        val two = HomeGridLogic.displaySlots(repository.getAppsSync())
        assertThat(two.filterIsInstance<HomeSlot.App>()).hasSize(2)
        assertThat(HomeGridLogic.stale(repository.getAppsSync(), emptyList())).isEmpty()
        assertThat(HomeGridLogic.displaySlots(repository.getAppsSync()).filterIsInstance<HomeSlot.App>())
            .hasSize(2)
    }
}
