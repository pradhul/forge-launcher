package com.sduduzog.slimlauncher.ui.home

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class LaunchStore(private val prefs: SharedPreferences) {

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun record(key: String, nowMillis: Long = System.currentTimeMillis()) {
        val next = LaunchRanker.record(key, records().firstOrNull { it.key == key }, nowMillis)
        val updated = records().associateBy { it.key }.toMutableMap()
        updated[key] = next
        write(updated.values)
        _revision.update { it + 1 }
    }

    fun records(): List<LaunchRecord> {
        val raw = prefs.getString(KEY_LAUNCHES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        LaunchRecord(
                            key = obj.getString("key"),
                            count = obj.getInt("count"),
                            lastLaunchMillis = obj.getLong("lastLaunchMillis")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun rankedKeys(nowMillis: Long = System.currentTimeMillis()): List<String> {
        return LaunchRanker.rank(records(), nowMillis)
    }

    private fun write(records: Collection<LaunchRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("key", record.key)
                    .put("count", record.count)
                    .put("lastLaunchMillis", record.lastLaunchMillis)
            )
        }
        prefs.edit { putString(KEY_LAUNCHES, array.toString()) }
    }

    companion object {
        const val PREFS_NAME = "launch_stats"
        const val KEY_LAUNCHES = "key_launch_records"
    }
}
