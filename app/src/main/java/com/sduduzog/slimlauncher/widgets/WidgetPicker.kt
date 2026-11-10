package com.sduduzog.slimlauncher.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.ThemedIconPalette
import com.sduduzog.slimlauncher.market.MarketThemeLogic
import com.sduduzog.slimlauncher.ui.home.DrawerSearchStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A provider plus the platform handles needed to bind it. */
class PickerProvider(val widget: PickerWidget, val info: AppWidgetProviderInfo, val user: UserHandle)

/** Loads every widget provider for every profile, off the main thread. */
object WidgetPickerSource {
    fun load(context: Context): List<PickerProvider> {
        val manager = AppWidgetManager.getInstance(context)
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val pm = context.packageManager
        val density = context.resources.displayMetrics.density
        val appLabels = HashMap<String, String>()
        val out = ArrayList<PickerProvider>()
        val profiles = runCatching { um.userProfiles }.getOrNull().orEmpty()
        for (user in profiles) {
            val serial = um.getSerialNumberForUser(user)
            val infos = runCatching { manager.getInstalledProvidersForProfile(user) }.getOrNull().orEmpty()
            for (info in infos) {
                val pkg = info.provider.packageName
                val appLabel = appLabels.getOrPut("$pkg#$serial") {
                    runCatching {
                        launcher.getActivityList(pkg, user).firstOrNull()?.label?.toString()
                    }.getOrNull() ?: runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                }
                val label = runCatching { info.loadLabel(pm) }.getOrNull()?.takeIf { it.isNotBlank() } ?: appLabel
                val (tw, th) = if (Build.VERSION.SDK_INT >= 31) info.targetCellWidth to info.targetCellHeight else 0 to 0
                val (cw, ch) = WidgetPickerLogic.cells(
                    tw, th, (info.minWidth / density).toInt(), (info.minHeight / density).toInt()
                )
                val key = "${info.provider.flattenToString()}#$serial"
                out += PickerProvider(PickerWidget(key, pkg, serial, appLabel, label, cw, ch), info, user)
            }
        }
        return out
    }
}

/** Async-loaded, cached widget preview bitmaps. */
object WidgetPreviewCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun load(context: Context, p: PickerProvider, maxPx: Int): Bitmap? {
        cache.get(p.widget.key)?.let { return it }
        val dpi = context.resources.displayMetrics.densityDpi
        val d = runCatching { p.info.loadPreviewImage(context, dpi) }.getOrNull()
            ?: runCatching { p.info.loadIcon(context, dpi) }.getOrNull()
            ?: return null
        val iw = d.intrinsicWidth.takeIf { it > 0 } ?: maxPx
        val ih = d.intrinsicHeight.takeIf { it > 0 } ?: maxPx
        val scale = minOf(1f, maxPx.toFloat() / maxOf(iw, ih))
        val bw = (iw * scale).toInt().coerceAtLeast(1)
        val bh = (ih * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, bw, bh)
        runCatching { d.draw(Canvas(bmp)) }.onFailure { return null }
        cache.put(p.widget.key, bmp)
        return bmp
    }
}

@Composable
fun WidgetPickerScreen(
    theme: PackTheme,
    backdrop: Bitmap?,
    appIcon: (String, Long) -> Bitmap?,
    onPick: (PickerProvider) -> Unit,
    onPickBuiltin: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val providers by produceState<List<PickerProvider>?>(null) {
        value = withContext(Dispatchers.IO) { WidgetPickerSource.load(context.applicationContext) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val byKey = remember(providers) { providers.orEmpty().associateBy { it.widget.key } }
    val forge = remember(context.packageName) {
        WidgetPickerLogic.forgeSection(context.packageName)
    }
    val sections = remember(providers, forge) {
        WidgetPickerLogic.withPinned(forge, WidgetPickerLogic.group(providers.orEmpty().map { it.widget }))
    }
    val shown = remember(sections, query) { WidgetPickerLogic.filter(sections, query) }

    val onBg = Color(theme.onBackground)
    val cardColor = remember(theme) {
        val base = if (theme.darkSurface) ThemedIconPalette.tone(theme.accent, 18.0, 1.2, 0.5)
        else ThemedIconPalette.tone(theme.accent, 92.0, 1.2, 0.4)
        Color((base and 0x00FFFFFF) or (0xE6 shl 24))
    }
    val backdropImage = remember(backdrop) { backdrop?.asImageBitmap() }

    Box(Modifier.fillMaxSize().background(Color(theme.background))) {
        backdropImage?.let {
            Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Color(theme.background).copy(alpha = if (backdropImage != null) 0.4f else 0.7f)
            )
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (providers == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(theme.accent))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(shown, key = { it.key }) { section ->
                            PickerSectionRow(
                                section, byKey, cardColor, onBg, theme, appIcon, onPick, onPickBuiltin
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PickerSearchField(theme, query) { query = it }
        }
    }
}

@Composable
private fun PickerSectionRow(
    section: PickerSection,
    byKey: Map<String, PickerProvider>,
    cardColor: Color,
    onBg: Color,
    theme: PackTheme,
    appIcon: (String, Long) -> Bitmap?,
    onPick: (PickerProvider) -> Unit,
    onPickBuiltin: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val icon by produceState<Bitmap?>(null, section.key) {
                value = withContext(Dispatchers.IO) { runCatching { appIcon(section.packageName, section.profileSerial) }.getOrNull() }
            }
            Box(Modifier.size(32.dp)) {
                icon?.let { Image(it.asImageBitmap(), null, Modifier.size(32.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                section.appLabel, color = onBg, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.widgets, key = { it.key }) { w ->
                val p = byKey[w.key]
                if (p != null) {
                    WidgetCard(p, cardColor, onBg) { onPick(p) }
                } else {
                    BuiltinPickerCard(w, theme, cardColor, onBg) { onPickBuiltin(w.key) }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(p: PickerProvider, cardColor: Color, onBg: Color, onClick: () -> Unit) {
    val w = p.widget
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .width(WidgetPickerLogic.cardWidthDp(w.cellsW).dp)
            .clip(shape)
            .background(cardColor, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { WidgetPreview(p) }
        Spacer(Modifier.height(8.dp))
        Text(w.label, color = onBg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(w.sizeLabel, color = onBg.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun BuiltinPickerCard(
    widget: PickerWidget,
    theme: PackTheme,
    cardColor: Color,
    onBg: Color,
    onClick: () -> Unit
) {
    val colors = remember(theme) { MarketThemeLogic.colors(theme) }
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .width(WidgetPickerLogic.cardWidthDp(widget.cellsW).dp)
            .clip(shape)
            .background(cardColor, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(colors.card))
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(widget.label, color = Color(colors.muted), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "—",
                color = Color(colors.text),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(widget.label, color = onBg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(widget.sizeLabel, color = onBg.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun WidgetPreview(p: PickerProvider) {
    val context = LocalContext.current
    var layoutFailed by remember(p.widget.key) { mutableStateOf(false) }
    val previewLayout = if (Build.VERSION.SDK_INT >= 31) p.info.previewLayout else 0
    if (previewLayout != 0 && !layoutFailed) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).also { frame ->
                    runCatching {
                        val rv = android.widget.RemoteViews(p.info.provider.packageName, previewLayout)
                        frame.addView(rv.apply(ctx, frame))
                    }.onFailure { layoutFailed = true }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    val maxPx = (context.resources.displayMetrics.density * 280).toInt()
    val bmp by produceState(WidgetPreviewCache.get(p.widget.key), p.widget.key) {
        if (value == null) value = withContext(Dispatchers.IO) { WidgetPreviewCache.load(context, p, maxPx) }
    }
    bmp?.let { Image(it.asImageBitmap(), p.widget.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
}

@Composable
private fun PickerSearchField(theme: PackTheme, query: String, onChange: (String) -> Unit) {
    val onBackground = theme.onBackground
    val container = Color(DrawerSearchStyle.containerColor(theme.background, theme.accent))
    val text = Color(DrawerSearchStyle.textColor(onBackground))
    val icon = Color(DrawerSearchStyle.iconColor(onBackground))
    val cursor = Color(DrawerSearchStyle.cursorColor(theme.accent))
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = true,
        shape = CircleShape,
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = "Search widgets", tint = icon) },
        colors = TextFieldDefaults.colors(
            focusedTextColor = text,
            unfocusedTextColor = text,
            cursorColor = cursor,
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = icon,
            unfocusedLeadingIconColor = icon
        )
    )
}
