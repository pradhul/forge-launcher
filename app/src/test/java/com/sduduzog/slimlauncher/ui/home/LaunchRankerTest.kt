package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LaunchRankerTest {

    private val day = 24L * 60L * 60L * 1000L
    private val now = 1_700_000_000_000L

    @Test
    fun firstLaunchCreatesCountOfOne() {
        val recorded = LaunchRanker.record("chrome:c:0", existing = null, nowMillis = now)
        assertThat(recorded.key).isEqualTo("chrome:c:0")
        assertThat(recorded.count).isEqualTo(1)
        assertThat(recorded.lastLaunchMillis).isEqualTo(now)
    }

    @Test
    fun laterLaunchIncrementsCountAndUpdatesRecency() {
        val first = LaunchRanker.record("chrome:c:0", null, now)
        val second = LaunchRanker.record("chrome:c:0", first, now + day)
        assertThat(second.count).isEqualTo(2)
        assertThat(second.lastLaunchMillis).isEqualTo(now + day)
    }

    @Test
    fun recentAppOutranksOlderHigherCount() {
        val stale = LaunchRecord("old:a:0", count = 12, lastLaunchMillis = now - 30 * day)
        val fresh = LaunchRecord("new:a:0", count = 3, lastLaunchMillis = now)
        val ranked = LaunchRanker.rank(listOf(stale, fresh), now)
        assertThat(ranked.first()).isEqualTo("new:a:0")
    }

    @Test
    fun rankReturnsAtMostFourKeys() {
        val records = (1..8).map { index ->
            LaunchRecord("app$index:a:0", count = index, lastLaunchMillis = now)
        }
        val ranked = LaunchRanker.rank(records, now)
        assertThat(ranked).hasSize(LaunchRanker.LIMIT)
        assertThat(ranked).containsExactly("app8:a:0", "app7:a:0", "app6:a:0", "app5:a:0").inOrder()
    }

    @Test
    fun hideFrequentRowWhenSearchingOrEmpty() {
        assertThat(LaunchRanker.shouldShowFrequent("", emptyList())).isFalse()
        assertThat(LaunchRanker.shouldShowFrequent("chr", listOf("chrome:c:0"))).isFalse()
        assertThat(LaunchRanker.shouldShowFrequent("", listOf("chrome:c:0"))).isTrue()
    }
}
