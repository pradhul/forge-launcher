package com.sduduzog.slimlauncher.models

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.data.model.App
import org.junit.Test

class HomeDefaultsTest {

    private fun app(name: String, pkg: String) = App(name, pkg, "$pkg.Main", 0L)

    @Test
    fun picksPreferredPackagesInRoleOrder() {
        val installed = listOf(
            app("Play Store", "com.android.vending"),
            app("Settings", "com.android.settings"),
            app("Camera", "com.google.android.GoogleCamera"),
            app("Chrome", "com.android.chrome"),
            app("Messages", "com.google.android.apps.messaging"),
            app("Phone", "com.google.android.dialer"),
            app("Maps", "com.google.android.apps.maps")
        )
        val picked = HomeDefaults.pick(installed)
        assertThat(picked.map { it.packageName }).containsExactly(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.GoogleCamera",
            "com.android.settings",
            "com.android.vending"
        ).inOrder()
    }

    @Test
    fun matchesByLabelWhenPackageIsUnknown() {
        val installed = listOf(
            app("Phone", "com.oem.dial"),
            app("Messages", "com.oem.sms"),
            app("Other", "com.oem.other")
        )
        val picked = HomeDefaults.pick(installed, limit = 2)
        assertThat(picked.map { it.appName }).containsExactly("Phone", "Messages").inOrder()
    }

    @Test
    fun fillsRemainingSlotsSoHomeIsNeverEmpty() {
        val installed = listOf(
            app("Alpha", "com.a"),
            app("Beta", "com.b"),
            app("Chrome", "com.android.chrome")
        )
        val picked = HomeDefaults.pick(installed, limit = 3)
        assertThat(picked).hasSize(3)
        assertThat(picked.map { it.packageName }).contains("com.android.chrome")
    }

    @Test
    fun emptyInstalledYieldsEmpty() {
        assertThat(HomeDefaults.pick(emptyList())).isEmpty()
    }
}
