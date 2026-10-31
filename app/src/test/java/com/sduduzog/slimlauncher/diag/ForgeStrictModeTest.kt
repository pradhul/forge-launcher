package com.sduduzog.slimlauncher.diag

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForgeStrictModeTest {

    @Test
    fun logsOnlyAppPackageFrames() {
        val app = arrayOf(
            StackTraceElement("android.os.StrictMode", "read", "StrictMode.java", 1),
            StackTraceElement(
                "com.sduduzog.slimlauncher.icons.IconPreferences",
                "get",
                "IconPreferences.kt",
                10
            )
        )
        val framework = arrayOf(
            StackTraceElement("android.os.StrictMode", "read", "StrictMode.java", 1),
            StackTraceElement(
                "android.content.res.Resources",
                "loadXmlResourceParser",
                "Resources.java",
                20
            ),
            StackTraceElement("android.view.LayoutInflater", "inflate", "LayoutInflater.java", 40)
        )
        assertThat(ForgeStrictMode.shouldLog(app)).isTrue()
        assertThat(ForgeStrictMode.shouldLog(framework)).isFalse()
        assertThat(ForgeStrictMode.APP_PREFIX).isEqualTo("com.sduduzog.slimlauncher")
    }
}
