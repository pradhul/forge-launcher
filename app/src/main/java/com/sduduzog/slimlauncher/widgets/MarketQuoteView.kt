package com.sduduzog.slimlauncher.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.market.MarketCardColors
import com.sduduzog.slimlauncher.market.MarketKinds
import com.sduduzog.slimlauncher.market.MarketQuoteLogic
import com.sduduzog.slimlauncher.market.MarketQuoteUi
import com.sduduzog.slimlauncher.market.MarketThemeLogic
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class MarketQuoteView(
    context: Context,
    val kind: String,
    private val onTapRefresh: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val badge = TextView(context)
    private val label = TextView(context)
    private val refresh = ImageView(context)
    private val price = TextView(context)
    private val chip = TextView(context)
    private val synced = TextView(context)
    private var colors: MarketCardColors = MarketThemeLogic.colors(PackTheme.STOCK)
    private var ui = MarketQuoteUi(null)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(16)
        setPadding(pad, dp(10), pad, dp(10))
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        badge.gravity = Gravity.CENTER
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        badge.setTypeface(Typeface.DEFAULT_BOLD)
        header.addView(badge, LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(10) })
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        label.setTypeface(Typeface.DEFAULT_BOLD)
        label.text = MarketKinds.label(kind)
        header.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        refresh.setImageResource(R.drawable.ic_refresh)
        refresh.contentDescription = "Refresh"
        refresh.isClickable = true
        refresh.isFocusable = true
        refresh.setPadding(dp(6), dp(6), dp(6), dp(6))
        refresh.setOnClickListener { onTapRefresh() }
        header.addView(refresh, LayoutParams(dp(32), dp(32)))
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        price.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        price.setTypeface(Typeface.DEFAULT_BOLD)
        price.setPadding(0, dp(2), 0, 0)
        price.maxLines = 1
        addView(price, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        chip.setTypeface(Typeface.DEFAULT_BOLD)
        chip.setPadding(dp(8), dp(3), dp(8), dp(3))
        chip.maxLines = 2
        addView(
            chip,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }
        )
        synced.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        synced.maxLines = 1
        addView(
            synced,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) }
        )
        isClickable = true
        isFocusable = true
        setOnClickListener { onTapRefresh() }
        bind(MarketQuoteUi(null))
    }

    fun applyColors(next: MarketCardColors) {
        colors = next
        bind(ui)
    }

    fun bind(next: MarketQuoteUi) {
        ui = next
        val palette = colors
        val gold = kind == MarketKinds.GOLD
        badge.text = if (gold) "\u20B9" else "N"
        badge.setTextColor(if (gold) 0xFF3A2208.toInt() else Color.WHITE)
        badge.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (gold) 0xFFE0B45A.toInt() else palette.accent)
        }
        label.setTextColor(palette.muted)
        price.setTextColor(palette.text)
        synced.setTextColor(palette.muted)
        refresh.setColorFilter(palette.text)
        val quote = next.quote
        price.text = quote?.let { MarketQuoteLogic.formatPrice(kind, it.price) } ?: "—"
        val change = quote?.let { MarketQuoteLogic.dayChange(it.price, it.previousPrice) }
        val dir = change?.let { MarketQuoteLogic.changeDirection(it) } ?: 0
        if (change == null) {
            chip.visibility = GONE
        } else {
            chip.visibility = VISIBLE
            chip.text = MarketQuoteLogic.formatDayChange(kind, change)
            chip.setTextColor(MarketThemeLogic.chipForeground(palette, dir))
            chip.background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(MarketThemeLogic.chipBackground(palette, dir))
            }
        }
        val syncedText = quote?.let { MarketQuoteLogic.formatSynced(it.updatedAtMs) }.orEmpty()
        val footer = MarketQuoteLogic.formatFooter(
            synced = syncedText,
            failed = next.failed && !next.refreshing,
            spot = quote?.spot == true
        )
        synced.text = footer
        synced.visibility = if (footer.isEmpty()) GONE else VISIBLE
        refresh.isEnabled = !next.refreshing
        if (next.refreshing) startSpin() else refresh.clearAnimation()
    }

    private fun startSpin() {
        if (refresh.animation != null) return
        val spin = RotateAnimation(
            0f, 360f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        spin.duration = 700
        spin.repeatCount = RotateAnimation.INFINITE
        spin.interpolator = LinearInterpolator()
        refresh.startAnimation(spin)
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
