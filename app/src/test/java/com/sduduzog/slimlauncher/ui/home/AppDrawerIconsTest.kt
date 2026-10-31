package com.sduduzog.slimlauncher.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.FileIconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.IconRasterizer
import com.sduduzog.slimlauncher.icons.OnDeviceIconStylizer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AppDrawerIconsTest {

    @Test
    fun drawerItemsCarryVisibleNonZeroIcons() {
        val adaptive = AdaptiveIconDrawable(
            ColorDrawable(Color.rgb(20, 80, 200)),
            ColorDrawable(Color.WHITE)
        )
        val icon = IconRasterizer.toBitmap(adaptive, 96)
        assertThat(icon.width).isEqualTo(96)
        assertThat(icon.height).isEqualTo(96)
        assertThat(IconRasterizer.hasVisiblePixels(icon)).isTrue()

        val apps = listOf(
            App("Messages", "com.google.android.apps.messaging", "Home", 0L),
            App("Phone", "com.google.android.dialer", "Dialtacts", 0L)
        )
        val state = AppDrawerUiState(
            apps = AppDrawerLogic.filter(apps, "").map { app ->
                DrawerAppItem(app = app, pinned = false, icon = icon)
            }
        )

        assertThat(state.apps).isNotEmpty()
        assertThat(state.apps.size).isAtMost(
            AppDrawerLogic.filter(apps, "").size
        )
        state.apps.forEach { item ->
            val rendered = item.icon
            assertThat(rendered).isNotNull()
            assertThat(rendered!!.width).isGreaterThan(0)
            assertThat(rendered.height).isGreaterThan(0)
            assertThat(IconRasterizer.hasVisiblePixels(rendered)).isTrue()
        }
        assertThat(AppDrawerGrid.PHONE_COLUMNS).isEqualTo(4)
        assertThat(AppDrawerGrid.ICON_DP).isEqualTo(56)
    }

    @Test
    fun resolverIconsStayVisibleWhenToggleIsOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = IconPreferences(
            context.getSharedPreferences("drawer_icons_test", Context.MODE_PRIVATE)
        )
        prefs.showIcons = false
        val resolver = AppIconResolver(
            context,
            FileIconPackStore(context.filesDir),
            prefs,
            OnDeviceIconStylizer(context)
        )
        val bitmap = resolver.resolveBitmap(
            context.packageName,
            context.applicationInfo.className ?: "Launcher",
            0L,
            72
        )
        val item = DrawerAppItem(
            app = App("Forge", context.packageName, "Launcher", 0L),
            pinned = true,
            icon = bitmap
        )
        assertThat(item.icon).isNotNull()
        assertThat(item.icon!!.width).isEqualTo(72)
        assertThat(item.icon!!.height).isEqualTo(72)
        assertThat(IconRasterizer.hasVisiblePixels(item.icon!!)).isTrue()
    }
}
