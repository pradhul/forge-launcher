package com.sduduzog.slimlauncher.adapters

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.models.HomeApp
import org.junit.Test

class HomeAppDiffTest {

    private val chrome = HomeApp("Chrome", "com.android.chrome", "Main", 0, null, 0L)
    private val chromeNick = chrome.copy(appNickname = "Web")
    private val camera = HomeApp("Camera", "com.android.camera", "Cam", 1, null, 0L)

    @Test
    fun identityUsesComponentAndUser() {
        assertThat(HomeAppDiff.areItemsTheSame(chrome, chromeNick)).isTrue()
        assertThat(HomeAppDiff.areContentsTheSame(chrome, chromeNick)).isFalse()
        assertThat(HomeAppDiff.areItemsTheSame(chrome, camera)).isFalse()
        assertThat(HomeAppDiff.stableId(chrome)).isEqualTo(
            HomeAppDiff.stableId(chrome.copy(appName = "Browser"))
        )
        assertThat(HomeAppDiff.stableId(chrome)).isNotEqualTo(HomeAppDiff.stableId(camera))
    }
}
