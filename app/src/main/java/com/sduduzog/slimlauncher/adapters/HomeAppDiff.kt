package com.sduduzog.slimlauncher.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.ui.home.HomeSlot

object HomeAppDiff : DiffUtil.ItemCallback<HomeApp>() {
    const val PAYLOAD_LABEL_COLOR = "label_color"
    const val PAYLOAD_ICON = "icon"

    override fun areItemsTheSame(oldItem: HomeApp, newItem: HomeApp): Boolean {
        return oldItem.packageName == newItem.packageName &&
            oldItem.activityName == newItem.activityName &&
            oldItem.userSerial == newItem.userSerial
    }

    override fun areContentsTheSame(oldItem: HomeApp, newItem: HomeApp): Boolean {
        return oldItem.appName == newItem.appName &&
            oldItem.appNickname == newItem.appNickname &&
            oldItem.sortingIndex == newItem.sortingIndex &&
            areItemsTheSame(oldItem, newItem)
    }

    fun stableId(app: HomeApp): Long {
        var hash = app.packageName.hashCode().toLong()
        hash = hash * 31 + app.activityName.hashCode()
        hash = hash * 31 + app.userSerial
        return hash
    }
}

object HomeSlotDiff {
    fun areItemsTheSame(oldItem: HomeSlot, newItem: HomeSlot): Boolean {
        return when {
            oldItem is HomeSlot.App && newItem is HomeSlot.App ->
                HomeAppDiff.areItemsTheSame(oldItem.app, newItem.app)
            oldItem is HomeSlot.Empty && newItem is HomeSlot.Empty ->
                oldItem.key == newItem.key
            else -> false
        }
    }

    fun areContentsTheSame(oldItem: HomeSlot, newItem: HomeSlot): Boolean {
        return when {
            oldItem is HomeSlot.App && newItem is HomeSlot.App ->
                HomeAppDiff.areContentsTheSame(oldItem.app, newItem.app)
            oldItem is HomeSlot.Empty && newItem is HomeSlot.Empty ->
                oldItem.key == newItem.key
            else -> false
        }
    }
}
