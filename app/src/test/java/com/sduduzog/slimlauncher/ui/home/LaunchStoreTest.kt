package com.sduduzog.slimlauncher.ui.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LaunchStoreTest {

    @Test
    fun recordsPersistAndRankAcrossReloads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("launch_store_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = LaunchStore(prefs)
        val now = 1_700_000_000_000L
        store.record("chrome:c:0", now)
        store.record("camera:c:0", now + 1)
        store.record("chrome:c:0", now + 2)

        val reloaded = LaunchStore(prefs)
        assertThat(reloaded.records()).hasSize(2)
        assertThat(reloaded.rankedKeys(now + 3).first()).isEqualTo("chrome:c:0")
        assertThat(reloaded.rankedKeys(now + 3)).hasSize(2)
    }
}
