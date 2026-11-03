package com.sduduzog.slimlauncher.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.ThemedIconPalette
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Widgets page (swipe left from home). AppWidgetHost-backed vertical stack of
 * rounded themed cards; long-press a card to edit (resize / move / remove).
 */
class WidgetsPage(
    private val activity: Activity,
    private val container: FrameLayout,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun launchPick(intent: Intent)
        fun launchBind(intent: Intent)
        fun launchConfigure(intent: Intent)
        fun onCloseRequested()
    }

    private val app: Context = activity.applicationContext
    private val density = activity.resources.displayMetrics.density
    private val host = AppWidgetHost(app, WidgetStore.HOST_ID)
    private val manager = AppWidgetManager.getInstance(app)
    private val store = WidgetStore(app)
    private var entries: List<WidgetEntry> = emptyList()
    private val cards = LinkedHashMap<Int, WidgetCard>()
    private var editingId: Int? = null
    private var pendingId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var cardColor = Color.argb(0xE0, 58, 18, 92)
    private var accent = Color.rgb(214, 170, 255)
    private var onAccent = Color.rgb(60, 10, 90)
    private var textColor = Color.WHITE

    private val frame = PageFrame(activity)
    private val backdrop = ImageView(activity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val scrim = View(activity)
    private val scroll = ScrollView(activity).apply {
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        clipToPadding = false
    }
    private val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val addButton = TextView(activity)
    private val dots = LinearLayout(activity)

    val isEditing: Boolean get() = editingId != null

    init {
        container.removeAllViews()
        container.addView(frame, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(backdrop, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))
        scroll.setPadding(0, dp(56), 0, dp(150))
        scroll.addView(column, FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        addButton.apply {
            text = "+"
            textSize = 30f
            gravity = Gravity.CENTER
            contentDescription = "Add widget"
            setOnClickListener { startAdd() }
        }
        frame.addView(addButton, FrameLayout.LayoutParams(dp(56), dp(56)).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(64)
        })
        dots.orientation = LinearLayout.HORIZONTAL
        frame.addView(dots, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(36)
        })
        styleChrome()
    }

    fun start() { runCatching { host.startListening() } }

    fun stop() { runCatching { host.stopListening() } }

    fun applyTheme(theme: PackTheme, blurredWallpaper: Bitmap?) {
        val colors = ThemedIconPalette.forTheme(theme)
        accent = colors.foreground.takeIf { theme.darkSurface } ?: colors.background
        onAccent = if (theme.darkSurface) colors.background else colors.foreground
        textColor = theme.onBackground
        // Themed card: wallpaper-tinted tone, mostly opaque.
        val base = if (theme.darkSurface) ThemedIconPalette.tone(theme.accent, 18.0, 1.2, 0.5)
        else ThemedIconPalette.tone(theme.accent, 92.0, 1.2, 0.4)
        cardColor = (base and 0x00FFFFFF) or (0xE6 shl 24)
        backdrop.setImageBitmap(blurredWallpaper)
        backdrop.visibility = if (blurredWallpaper != null) View.VISIBLE else View.GONE
        scrim.setBackgroundColor((theme.background and 0x00FFFFFF) or ((if (blurredWallpaper != null) 0x66 else 0xB3) shl 24))
        styleChrome()
        cards.values.forEach { it.restyle() }
    }

    fun reload() {
        entries = WidgetLayoutLogic.prune(store.load()) { manager.getAppWidgetInfo(it) != null }
        store.save(entries)
        column.removeAllViews()
        cards.clear()
        for (entry in entries) {
            val info = manager.getAppWidgetInfo(entry.id) ?: continue
            val card = WidgetCard(entry, info)
            cards[entry.id] = card
            column.addView(card, cardParams(entry.heightDp))
        }
        editingId = null
    }

    fun exitEdit() {
        editingId = null
        cards.values.forEach { it.setEditing(false); it.alpha = 1f }
    }

    // ---- add flow ----

    fun startAdd() {
        exitEdit()
        val id = host.allocateAppWidgetId()
        pendingId = id
        val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList())
            .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList())
        try {
            callbacks.launchPick(pick)
        } catch (_: ActivityNotFoundException) {
            showProviderList(id)
        } catch (_: SecurityException) {
            showProviderList(id)
        }
    }

    fun onPickResult(ok: Boolean, data: Intent?) {
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId) ?: pendingId
        if (!ok) { discard(id); return }
        if (manager.getAppWidgetInfo(id) == null) {
            // Picker returned without binding: fall back to the in-app list.
            showProviderList(id)
            return
        }
        finishBind(id)
    }

    fun onBindResult(ok: Boolean) {
        if (ok && manager.getAppWidgetInfo(pendingId) != null) finishBind(pendingId) else discard(pendingId)
    }

    fun onConfigureResult(ok: Boolean) {
        if (ok) place(pendingId) else discard(pendingId)
    }

    private fun showProviderList(id: Int) {
        val providers = manager.installedProviders.sortedBy { it.loadLabel(activity.packageManager).lowercase() }
        if (providers.isEmpty()) { discard(id); return }
        val labels = providers.map { it.loadLabel(activity.packageManager) }.toTypedArray()
        AlertDialog.Builder(activity)
            .setItems(labels) { _, which ->
                val provider = providers[which].provider
                if (manager.bindAppWidgetIdIfAllowed(id, provider)) {
                    finishBind(id)
                } else {
                    pendingId = id
                    val bind = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                    runCatching { callbacks.launchBind(bind) }.onFailure { discard(id) }
                }
            }
            .setOnCancelListener { discard(id) }
            .show()
    }

    private fun finishBind(id: Int) {
        pendingId = id
        val info = manager.getAppWidgetInfo(id) ?: run { discard(id); return }
        if (info.configure != null) {
            val configure = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            try {
                callbacks.launchConfigure(configure)
                return
            } catch (_: Exception) {
                // Non-exported configure activity: let the host launch it.
                runCatching {
                    host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQ_CONFIGURE, null)
                }
            }
        }
        place(id)
    }

    private fun place(id: Int) {
        val info = manager.getAppWidgetInfo(id) ?: run { discard(id); return }
        val minDp = (info.minHeight / density).roundToInt()
        val entry = WidgetEntry(id, WidgetLayoutLogic.defaultHeight(minDp))
        entries = WidgetLayoutLogic.add(entries, entry)
        store.save(entries)
        val card = WidgetCard(entry, info)
        cards[id] = card
        column.addView(card, cardParams(entry.heightDp))
        scroll.post { scroll.smoothScrollTo(0, column.height) }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun discard(id: Int) {
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID && entries.none { it.id == id }) {
            runCatching { host.deleteAppWidgetId(id) }
        }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    // ---- edit ops ----

    private fun enterEdit(id: Int) {
        editingId = id
        cards.forEach { (cid, card) ->
            card.setEditing(cid == id)
            card.alpha = if (cid == id) 1f else 0.35f
        }
    }

    private fun removeWidget(id: Int) {
        entries = WidgetLayoutLogic.remove(entries, id)
        store.save(entries)
        cards.remove(id)?.let { column.removeView(it) }
        runCatching { host.deleteAppWidgetId(id) }
        exitEdit()
    }

    private fun moveWidget(id: Int, delta: Int) {
        entries = WidgetLayoutLogic.move(entries, id, delta)
        store.save(entries)
        val card = cards[id] ?: return
        column.removeView(card)
        column.addView(card, entries.indexOfFirst { it.id == id }, card.layoutParams)
    }

    private fun commitHeight(id: Int, heightDp: Int) {
        entries = WidgetLayoutLogic.resize(entries, id, heightDp)
        store.save(entries)
        cards[id]?.updateSizeOptions(heightDp)
    }

    // ---- views ----

    private fun styleChrome() {
        addButton.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(accent) }
        addButton.setTextColor(onAccent)
        dots.removeAllViews()
        for (i in 0 until 2) {
            val dot = View(activity)
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == 0) textColor else (textColor and 0x00FFFFFF) or (0x66 shl 24))
            }
            dots.addView(dot, LinearLayout.LayoutParams(dp(7), dp(7)).apply { marginStart = dp(5); marginEnd = dp(5) })
        }
    }

    private fun cardParams(heightDp: Int) = LinearLayout.LayoutParams(MATCH, dp(heightDp)).apply {
        marginStart = dp(16); marginEnd = dp(16); topMargin = dp(8); bottomMargin = dp(8)
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()

    @SuppressLint("ViewConstructor")
    private inner class WidgetCard(var entry: WidgetEntry, info: AppWidgetProviderInfo) : FrameLayout(activity) {
        private val hostView: AppWidgetHostView = host.createView(app, entry.id, info)
        private val controls = LinearLayout(activity)
        private val topHandle = View(activity)
        private val bottomHandle = View(activity)
        private var editing = false
        private val handler = Handler(Looper.getMainLooper())
        private val slop = ViewConfiguration.get(activity).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var longPressed = false
        private val longPress = Runnable {
            longPressed = true
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            enterEdit(entry.id)
        }

        init {
            clipToOutline = true
            addView(hostView, LayoutParams(MATCH, MATCH))
            controls.orientation = LinearLayout.HORIZONTAL
            listOf("\u2193" to { moveWidget(entry.id, 1) }, "\u2191" to { moveWidget(entry.id, -1) }, "\u00D7" to { removeWidget(entry.id) })
                .forEach { (glyph, action) ->
                    val b = TextView(activity).apply {
                        text = glyph; textSize = 18f; gravity = Gravity.CENTER
                        setOnClickListener { action() }
                        contentDescription = when (glyph) { "\u00D7" -> "Remove"; "\u2191" -> "Move up"; else -> "Move down" }
                    }
                    controls.addView(b, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginStart = dp(8) })
                }
            addView(controls, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END; topMargin = dp(10); marginEnd = dp(10)
            })
            addView(topHandle, LayoutParams(dp(56), dp(10)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL })
            addView(bottomHandle, LayoutParams(dp(56), dp(10)).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL })
            attachResize(topHandle, top = true)
            attachResize(bottomHandle, top = false)
            setEditing(false)
            restyle()
            updateSizeOptions(entry.heightDp)
        }

        fun restyle() {
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(cardColor)
                if (editing) setStroke(dp(3), accent)
            }
            for (i in 0 until controls.childCount) {
                val b = controls.getChildAt(i) as TextView
                val remove = i == controls.childCount - 1
                b.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (remove) Color.rgb(230, 80, 100) else accent)
                }
                b.setTextColor(if (remove) Color.WHITE else onAccent)
            }
            listOf(topHandle, bottomHandle).forEach {
                it.background = GradientDrawable().apply { cornerRadius = dp(5).toFloat(); setColor(accent) }
            }
        }

        fun setEditing(on: Boolean) {
            editing = on
            val vis = if (on) View.VISIBLE else View.GONE
            controls.visibility = vis; topHandle.visibility = vis; bottomHandle.visibility = vis
            restyle()
        }

        fun updateSizeOptions(heightDp: Int) {
            val widthDp = ((activity.resources.displayMetrics.widthPixels - dp(32)) / density).roundToInt()
            val opts = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
            runCatching { manager.updateAppWidgetOptions(entry.id, opts) }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun attachResize(handle: View, top: Boolean) {
            var startY = 0f
            var startH = 0
            handle.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = e.rawY; startH = layoutParams.height
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = e.rawY - startY
                        val h = (startH + if (top) -dy else dy).roundToInt()
                        val clamped = WidgetLayoutLogic.clampHeight((h / density).roundToInt())
                        layoutParams = layoutParams.also { it.height = dp(clamped) }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        commitHeight(entry.id, (layoutParams.height / density).roundToInt())
                    }
                }
                true
            }
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (editing) {
                // In edit mode the widget itself is inert; only controls act.
                return !isOnChild(ev, controls) && !isOnChild(ev, topHandle) && !isOnChild(ev, bottomHandle)
            }
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x; downY = ev.y; longPressed = false
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) handler.removeCallbacks(longPress)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPress)
            }
            return longPressed
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean = editing || longPressed

        private fun isOnChild(ev: MotionEvent, child: View): Boolean {
            if (child.visibility != View.VISIBLE) return false
            val r = Rect(); child.getHitRect(r)
            return r.contains(ev.x.toInt(), ev.y.toInt())
        }
    }

    /** Root: horizontal swipe right returns home; tap outside the edited card exits edit. */
    private inner class PageFrame(context: Context) : FrameLayout(context) {
        private var downX = 0f
        private var downY = 0f
        private var stolen = false

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY; stolen = false
                    val editing = editingId?.let { cards[it] }
                    if (editing != null && !hit(editing, ev)) {
                        exitEdit()
                        stolen = true
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (PageSwipeLogic.returnsHome(ev.rawX - downX, ev.rawY - downY, density, isEditing)) {
                        stolen = true
                        callbacks.onCloseRequested()
                        return true
                    }
                }
            }
            return false
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                downX = event.rawX; downY = event.rawY
                return true
            }
            if (!stolen && event.actionMasked == MotionEvent.ACTION_MOVE &&
                PageSwipeLogic.returnsHome(event.rawX - downX, event.rawY - downY, density, isEditing)
            ) {
                stolen = true
                callbacks.onCloseRequested()
            }
            return true
        }

        private fun hit(view: View, ev: MotionEvent): Boolean {
            val loc = IntArray(2); view.getLocationOnScreen(loc)
            return ev.rawX >= loc[0] && ev.rawX <= loc[0] + view.width && ev.rawY >= loc[1] && ev.rawY <= loc[1] + view.height
        }
    }

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val REQ_CONFIGURE = 4711
    }
}
