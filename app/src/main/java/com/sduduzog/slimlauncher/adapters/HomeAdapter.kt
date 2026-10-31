package com.sduduzog.slimlauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener


class HomeAdapter(
    private val listener: OnLaunchAppListener,
    private val iconResolver: AppIconResolver
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    private var apps: List<HomeApp> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.main_fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = apps.elementAt(position)
        holder.mLabelView.text = item.appNickname ?: item.appName
        iconResolver.bind(holder.mIconView, item.packageName, item.activityName, item.userSerial)
        holder.itemView.setOnClickListener {
            listener.onLaunch(item, it)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun setItems(list: List<HomeApp>) {
        this.apps = list
        notifyDataSetChanged()
    }


    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mLabelView: TextView = mView.findViewById(R.id.home_fragment_list_item_app_name)
        val mIconView: ImageView = mView.findViewById(R.id.home_fragment_list_item_app_icon)

        override fun toString(): String {
            return super.toString() + " '" + mLabelView.text + "'"
        }
    }
}
