package com.sduduzog.slimlauncher.adapters

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.ui.home.HomeGridLogic
import com.sduduzog.slimlauncher.ui.home.HomeIconGestureLogic
import com.sduduzog.slimlauncher.ui.home.HomeSlot
import com.sduduzog.slimlauncher.utils.OnItemActionListener
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener

class HomeAdapter(
    private val listener: OnLaunchAppListener,
    private val iconResolver: AppIconResolver,
    private val onLongPressReady: (HomeApp, View) -> Unit = { _, _ -> },
    private val onDragStarted: () -> Unit = {},
    private val onReorder: (List<HomeApp>) -> Unit = {}
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>(), OnItemActionListener {

    private val slots = mutableListOf<HomeSlot>()
    private var homeApps: List<HomeApp> = emptyList()
    private var viewportRows = 0
    private var labelColor: Int? = null
    private var dragging = false
    private var draggedItem = false
    private var dragHelper: ItemTouchHelper? = null
    private var dragSlop = 16f

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun attachDrag(recyclerView: RecyclerView) {
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        dragSlop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop * 2f
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val pos = viewHolder.bindingAdapterPosition
                if (pos !in slots.indices || slots[pos] is HomeSlot.Empty) {
                    return makeMovementFlags(0, 0)
                }
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

            override fun isLongPressDragEnabled() = false

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
        dragHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(recyclerView) }
    }

    fun isDragging(): Boolean = dragging

    override fun getItemViewType(position: Int): Int =
        if (slots[position] is HomeSlot.Empty) TYPE_EMPTY else TYPE_APP

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_fragment_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bindAll(holder, slots[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        val slot = slots[position]
        if (slot is HomeSlot.Empty) {
            bindEmpty(holder)
            return
        }
        val item = (slot as HomeSlot.App).app
        if (payloads.contains(HomeAppDiff.PAYLOAD_LABEL_COLOR)) {
            labelColor?.let { holder.mLabelView.setTextColor(it) }
        }
        if (payloads.contains(HomeAppDiff.PAYLOAD_ICON)) {
            bindIcon(holder, item)
        }
    }

    private fun bindAll(holder: ViewHolder, slot: HomeSlot) {
        if (slot is HomeSlot.Empty) {
            bindEmpty(holder)
            return
        }
        val item = (slot as HomeSlot.App).app
        holder.longPressed = false
        holder.itemView.visibility = View.VISIBLE
        holder.itemView.isClickable = true
        holder.mLabelView.text = item.appNickname ?: item.appName
        labelColor?.let { holder.mLabelView.setTextColor(it) }
        bindIcon(holder, item)
        holder.itemView.setOnClickListener {
            if (!dragging) listener.onLaunch(item, it)
        }
        holder.itemView.setOnLongClickListener {
            if (!dragging) {
                holder.longPressed = true
                onLongPressReady(item, holder.mIconView)
            }
            true
        }
        holder.itemView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    holder.downX = event.rawX
                    holder.downY = event.rawY
                    holder.longPressed = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (HomeIconGestureLogic.shouldStartDrag(
                            holder.longPressed,
                            dragging,
                            event.rawX - holder.downX,
                            event.rawY - holder.downY,
                            dragSlop
                        )
                    ) {
                        onDragStarted()
                        dragHelper?.startDrag(holder)
                    }
                }
            }
            false
        }
    }

    private fun bindEmpty(holder: ViewHolder) {
        holder.longPressed = false
        holder.itemView.visibility = View.INVISIBLE
        holder.itemView.isClickable = false
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
        holder.itemView.setOnTouchListener(null)
        holder.mLabelView.text = ""
        holder.mIconView.setImageDrawable(null)
    }

    private fun bindIcon(holder: ViewHolder, item: HomeApp) {
        iconResolver.bind(holder.mIconView, item.packageName, item.activityName, item.userSerial)
    }

    override fun getItemCount(): Int = slots.size

    override fun getItemId(position: Int): Long = when (val slot = slots[position]) {
        is HomeSlot.App -> HomeAppDiff.stableId(slot.app)
        is HomeSlot.Empty -> EMPTY_ID_BASE + slot.key
    }

    fun setViewportRows(rows: Int) {
        if (viewportRows == rows) return
        viewportRows = rows
        if (!dragging) applySlots()
    }

    fun setItems(list: List<HomeApp>) {
        if (dragging) return
        homeApps = list
        applySlots()
    }

    private fun applySlots() {
        val next = HomeGridLogic.displaySlots(homeApps, viewportRows)
        slots.clear()
        slots.addAll(next)
        notifyDataSetChanged()
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
        val moved = HomeGridLogic.reorder(slots, oldPosition, newPosition)
        if (moved === slots) return false
        slots.clear()
        slots.addAll(moved)
        draggedItem = true
        notifyItemMoved(oldPosition, newPosition)
        return true
    }

    override fun onViewIdle() {
        val moved = draggedItem
        dragging = false
        draggedItem = false
        if (moved) {
            onReorder(HomeGridLogic.withIndices(HomeGridLogic.logicalFromVisual(slots)))
        }
    }

    override fun onViewSwiped(position: Int) = Unit

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mLabelView: TextView = mView.findViewById(R.id.home_fragment_list_item_app_name)
        val mIconView: ImageView = mView.findViewById(R.id.home_fragment_list_item_app_icon)
        var downX = 0f
        var downY = 0f
        var longPressed = false
    }

    companion object {
        private const val TYPE_APP = 0
        private const val TYPE_EMPTY = 1
        private const val EMPTY_ID_BASE = Long.MIN_VALUE / 2
    }
}
