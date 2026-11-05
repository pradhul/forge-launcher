package com.sduduzog.slimlauncher.widgets

import android.content.Context

/** Persists the widgets page layout. AppWidget ids survive reboot with the host. */
class WidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("forge_widgets", Context.MODE_PRIVATE)

    fun load(): List<WidgetEntry> = WidgetLayoutLogic.parse(prefs.getString(KEY, null))

    fun save(list: List<WidgetEntry>) {
        prefs.edit().putString(KEY, WidgetLayoutLogic.serialize(list)).apply()
    }

    companion object {
        private const val KEY = "layout"
        const val HOST_ID = 0x0F06E
    }
}
