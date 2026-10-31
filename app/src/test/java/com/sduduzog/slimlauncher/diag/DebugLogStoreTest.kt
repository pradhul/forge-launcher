package com.sduduzog.slimlauncher.diag

import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DebugLogStoreTest {

    @Test
    fun reportIncludesAppendedStallLine() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DebugLogStore.file(context).delete()
        DebugLogStore.append(context, "STALL gap=900ms test")
        val deadline = System.currentTimeMillis() + 2_000
        var report = ""
        while (System.currentTimeMillis() < deadline) {
            report = DebugLogStore.buildReport(context)
            if (report.contains("STALL gap=900ms test")) break
            Thread.sleep(50)
        }
        assertThat(report).contains("Forge")
        assertThat(report).contains("STALL gap=900ms test")
        assertThat(DebugLogStore.file(context).name).isEqualTo(DebugLogLogic.FILE_NAME)
    }

    @Test
    fun anrReasonMatchesFrameworkConstant() {
        assertThat(DebugLogLogic.REASON_ANR).isEqualTo(ApplicationExitInfo.REASON_ANR)
        assertThat(DebugLogLogic.REASON_CRASH).isEqualTo(ApplicationExitInfo.REASON_CRASH)
    }
}
