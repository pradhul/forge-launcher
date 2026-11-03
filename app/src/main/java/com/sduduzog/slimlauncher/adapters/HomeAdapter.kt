package com.sduduzog.slimlauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener

class HomeAdapter(
    private val listener: OnLaunchAppListener,
    private val iconResolver: AppIconResolver,
    private val onLongPress: (HomeApp, View) -> Unit = { _, _ -> }
) : ListAdapter<HomeApp, HomeAdapter.ViewHolder>(HomeAppDiff) {

    private var labelColor: Int? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bindAll(holder, getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        val item = getItem(position)
        if (payloads.contains(HomeAppDiff.PAYLOAD_LABEL_COLOR)) {
            labelColor?.let { holder.mLabelView.setTextColor(it) }
        }
        if (payloads.contains(HomeAppDiff.PAYLOAD_ICON)) {
            bindIcon(holder, item)
        }
    }

    private fun bindAll(holder: ViewHolder, item: HomeApp) {
        holder.mLabelView.text = item.appNickname ?: item.appName
        labelColor?.let { holder.mLabelView.setTextColor(it) }
        bindIcon(holder, item)
        holder.itemView.setOnClickListener {
            listener.onLaunch(item, it)
        }
        holder.itemView.setOnLongClickListener {
            onLongPress(item, holder.mIconView)
            true
        }
    }

    private fun bindIcon(holder: ViewHolder, item: HomeApp) {
        iconResolver.bind(holder.mIconView, item.packageName, item.activityName, item.userSerial)
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long = HomeAppDiff.stableId(getItem(position))

    fun setItems(list: List<HomeApp>) {
        submitList(list)
    }

    fun setLabelColor(color: Int) {
        if (labelColor == color) return
        labelColor = color
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, HomeAppDiff.PAYLOAD_LABEL_COLOR)
        }
    }

    fun rebindIcons() {
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, HomeAppDiff.PAYLOAD_ICON)
        }
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mLabelView: TextView = mView.findViewById(R.id.home_fragment_list_item_app_name)
        val mIconView: ImageView = mView.findViewById(R.id.home_fragment_list_item_app_icon)
    }
}
