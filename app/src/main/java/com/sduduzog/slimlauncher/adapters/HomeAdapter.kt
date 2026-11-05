package com.sduduzog.slimlauncher.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.ui.home.HomeGridLogic
import com.sduduzog.slimlauncher.utils.OnItemActionListener
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener

class HomeAdapter(
    private val listener: OnLaunchAppListener,
    private val iconResolver: AppIconResolver,
    private val onLongPressReady: (HomeApp, View) -> Unit = { _, _ -> },
    private val onLongPressUp: () -> Unit = {},
    private val onReorder: (List<HomeApp>) -> Unit = {}
) : ListAdapter<HomeApp, HomeAdapter.ViewHolder>(HomeAppDiff), OnItemActionListener {

    private var labelColor: Int? = null
    private var dragging = false
    private var draggedItem = false

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun attachDrag(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val flags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(flags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = onViewMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled() = true

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    dragging = true
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1f
                onViewIdle()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    fun isDragging(): Boolean = dragging

    /** True once the icon has actually moved to another cell. */
    fun didDrag(): Boolean = draggedItem

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
            if (!dragging) listener.onLaunch(item, it)
        }
        holder.itemView.setOnLongClickListener {
            if (!dragging) onLongPressReady(item, holder.mIconView)
            true
        }
        holder.itemView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                onLongPressUp()
            }
            false
        }
    }

    private fun bindIcon(holder: ViewHolder, item: HomeApp) {
        iconResolver.bind(holder.mIconView, item.packageName, item.activityName, item.userSerial)
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long = HomeAppDiff.stableId(getItem(position))

    fun setItems(list: List<HomeApp>) {
        if (dragging) return
        submitList(HomeGridLogic.apps(list))
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

    override fun onViewMoved(oldPosition: Int, newPosition: Int): Boolean {
        val moved = HomeGridLogic.reorder(currentList, oldPosition, newPosition)
        if (moved === currentList) return false
        draggedItem = true
        submitList(moved)
        return true
    }

    override fun onViewIdle() {
        val moved = draggedItem
        dragging = false
        draggedItem = false
        if (moved) onReorder(currentList)
    }

    override fun onViewSwiped(position: Int) = Unit

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mLabelView: TextView = mView.findViewById(R.id.home_fragment_list_item_app_name)
        val mIconView: ImageView = mView.findViewById(R.id.home_fragment_list_item_app_icon)
    }
}
