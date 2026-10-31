package com.sduduzog.slimlauncher.ui.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.IconPreferences
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DrawerKeyboardPrefsTest {

    @Test
    fun openKeyboardDefaultsOffAndPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.getSharedPreferences("keyboard_pref_test", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val prefs = IconPreferences(store)
        assertThat(prefs.openKeyboardOnDrawer).isFalse()
        prefs.openKeyboardOnDrawer = true
        assertThat(IconPreferences(store).openKeyboardOnDrawer).isTrue()
        prefs.openKeyboardOnDrawer = false
        assertThat(IconPreferences(store).openKeyboardOnDrawer).isFalse()
    }
}
