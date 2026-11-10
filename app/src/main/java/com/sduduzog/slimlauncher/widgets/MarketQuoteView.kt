package com.sduduzog.slimlauncher.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.market.MarketCardColors
import com.sduduzog.slimlauncher.market.MarketKinds
import com.sduduzog.slimlauncher.market.MarketQuote
import com.sduduzog.slimlauncher.market.MarketQuoteLogic
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
    private val price = TextView(context)
    private val chip = TextView(context)
    private var colors: MarketCardColors = MarketThemeLogic.colors(PackTheme.STOCK)
    private var quote: MarketQuote? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(16)
        setPadding(pad, dp(12), pad, dp(12))
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
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        price.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
        price.setTypeface(Typeface.DEFAULT_BOLD)
        price.setPadding(0, dp(4), 0, 0)
        price.maxLines = 1
        addView(price, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        chip.setTypeface(Typeface.DEFAULT_BOLD)
        chip.setPadding(dp(8), dp(3), dp(8), dp(3))
        addView(
            chip,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        )
        isClickable = true
        isFocusable = true
        setOnClickListener { onTapRefresh() }
        bind(null)
    }

    fun applyColors(next: MarketCardColors) {
        colors = next
        bind(quote)
    }

    fun bind(next: MarketQuote?) {
        quote = next
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
        price.text = next?.let { MarketQuoteLogic.formatPrice(kind, it.price) } ?: "—"
        val pct = next?.changePercent
        val dir = MarketQuoteLogic.changeDirection(pct)
        if (pct == null) {
            chip.visibility = GONE
        } else {
            chip.visibility = VISIBLE
            chip.text = MarketQuoteLogic.formatChange(pct)
            chip.setTextColor(MarketThemeLogic.chipForeground(palette, dir))
            chip.background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(MarketThemeLogic.chipBackground(palette, dir))
            }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
