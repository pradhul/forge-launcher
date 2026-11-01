package com.sduduzog.slimlauncher.ui.home

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.IconCacheLogic
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.Repository
import com.sduduzog.slimlauncher.wallpaper.LauncherFadeLogic
import com.sduduzog.slimlauncher.wallpaper.WallpaperSnapshotter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DrawerAppItem(
    val app: App,
    val pinned: Boolean,
    val icon: Bitmap?
)

data class AppDrawerUiState(
    val query: String = "",
    val apps: List<DrawerAppItem> = emptyList(),
    val canRegenerate: Boolean = false,
    val theme: PackTheme = PackTheme.STOCK,
    val frequent: List<DrawerAppItem> = emptyList(),
    val openKeyboardOnOpen: Boolean = false,
    val drawerBlurPercent: Int = LauncherFadeLogic.DEFAULT_DRAWER_BLUR,
    val backdrop: Bitmap? = null,
    val crossWindowBlur: Boolean = false
)

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val repository: Repository,
    private val installedAppsQuery: InstalledAppsQuery,
    private val iconResolver: AppIconResolver,
    private val prefs: IconPreferences,
    private val store: IconPackStore,
    private val generator: IconPackGenerator,
    private val themeController: PackThemeController,
    private val launchStore: LaunchStore,
    private val snapshotter: WallpaperSnapshotter
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filterQuery = MutableStateFlow("")
    private val _installed = MutableStateFlow<List<App>>(emptyList())
    private val _home = MutableStateFlow<List<HomeApp>>(emptyList())
    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    private val _backdrop = MutableStateFlow<Bitmap?>(null)
    private val _crossWindowBlur = MutableStateFlow(false)
    private var reloadJob: Job? = null
    private var queryJob: Job? = null
    @Volatile
    private var lastInstalledMs = 0L

    private data class DrawerCatalog(
        val apps: List<DrawerAppItem>,
        val frequent: List<DrawerAppItem>,
        val canRegenerate: Boolean,
        val theme: PackTheme,
        val openKeyboardOnOpen: Boolean
    )

    private val items = combine(_installed, _home, _icons) { installed, home, icons ->
        val pins = home.map { it.packageName to it.userSerial }
        installed.map { app ->
            DrawerAppItem(
                app = app,
                pinned = AppDrawerLogic.isPinned(pins, app.packageName, app.userSerial),
                icon = icons[iconKey(app)]
            )
        }
    }.flowOn(Dispatchers.Default)

    private val catalog = combine(items, _filterQuery, launchStore.revision, prefs.revision) {
            all,
            query,
            _,
            _
        ->
        val filtered = if (query.isBlank()) {
            all
        } else {
            val allowed = AppDrawerLogic.filter(all.map { it.app }, query).toSet()
            all.filter { it.app in allowed }
        }
        val byKey = all.associateBy { iconKey(it.app) }
        val frequentKeys = launchStore.rankedKeys()
        val frequent = if (LaunchRanker.shouldShowFrequent(query, frequentKeys)) {
            frequentKeys.mapNotNull { byKey[it] }
        } else {
            emptyList()
        }
        DrawerCatalog(
            apps = filtered,
            frequent = frequent,
            canRegenerate = !prefs.appliedPackId.isNullOrBlank() &&
                store.getPack(prefs.appliedPackId!!) != null,
            theme = themeController.active(),
            openKeyboardOnOpen = prefs.openKeyboardOnDrawer
        )
    }.flowOn(Dispatchers.Default)

    val ui: StateFlow<AppDrawerUiState> = combine(
        catalog,
        _query,
        prefs.chrome,
        combine(_backdrop, _crossWindowBlur) { backdrop, crossWindow ->
            backdrop to crossWindow
        }
    ) { cat, query, chrome, backdropAndBlur ->
        AppDrawerUiState(
            query = query,
            apps = cat.apps,
            canRegenerate = cat.canRegenerate,
            theme = cat.theme,
            frequent = cat.frequent,
            openKeyboardOnOpen = cat.openKeyboardOnOpen,
            drawerBlurPercent = chrome.drawerBlurPercent,
            backdrop = backdropAndBlur.first,
            crossWindowBlur = backdropAndBlur.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState())

    private val homeObserver = androidx.lifecycle.Observer<List<HomeApp>> { list ->
        _home.value = list.orEmpty()
    }

    init {
        repository.apps.observeForever(homeObserver)
        viewModelScope.launch {
            prefs.revision.collect {
                reloadJob?.cancel()
                reloadJob = viewModelScope.launch {
                    delay(RELOAD_DEBOUNCE_MS)
                    iconResolver.evictCache()
                    _icons.value = emptyMap()
                    reloadIcons(force = true)
                }
            }
        }
        viewModelScope.launch {
            prefs.chrome.collect { refreshBackdrop(it.drawerBlurPercent, force = false) }
        }
        refreshInstalled(force = true)
        refreshBackdrop()
    }

    override fun onCleared() {
        repository.apps.removeObserver(homeObserver)
        super.onCleared()
    }

    fun setCrossWindowBlur(enabled: Boolean) {
        if (_crossWindowBlur.value == enabled) return
        _crossWindowBlur.value = enabled
    }

    fun refreshBackdrop() {
        refreshBackdrop(prefs.drawerBlurPercent, force = false)
    }

    private fun refreshBackdrop(percent: Int, force: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (force) snapshotter.invalidate()
            _backdrop.value = snapshotter.blurred(percent)
        }
    }

    fun refreshInstalled(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = SystemClock.uptimeMillis()
            if (!force &&
                !IconCacheLogic.shouldRefreshCatalog(
                    lastLoadMs = lastInstalledMs,
                    nowMs = now,
                    staleAfterMs = AppDrawerLogic.CATALOG_STALE_MS,
                    hasCache = _installed.value.isNotEmpty()
                )
            ) {
                reloadIcons(force = false)
                return@launch
            }
            val apps = installedAppsQuery.load()
            lastInstalledMs = now
            if (apps != _installed.value) {
                _installed.value = apps
            }
            prefs.appliedPackId?.let { packId ->
                store.getPack(packId)?.let { generator.fillMissing(it, apps) }
            }
            reloadIcons(force)
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(AppDrawerLogic.SEARCH_DEBOUNCE_MS)
            _filterQuery.value = value
        }
    }

    fun recordLaunch(app: App) {
        launchStore.record(iconKey(app))
    }

    fun regenerate(app: App) {
        val packId = prefs.appliedPackId ?: return
        if (generator.isRunning) return
        val pack = store.getPack(packId) ?: return
        generator.regenerate(packId, app)
    }

    private suspend fun reloadIcons(force: Boolean) {
        val apps = _installed.value
        val existing = _icons.value
        val wanted = apps.map { iconKey(it) }
        val missing = if (force) {
            wanted
        } else {
            IconCacheLogic.missingKeys(existing.keys, wanted)
        }
        if (missing.isEmpty()) return
        val missingSet = missing.toSet()
        val loaded = withContext(Dispatchers.IO) {
            val next = if (force) LinkedHashMap<String, Bitmap>(apps.size) else LinkedHashMap(existing)
            for (app in apps) {
                val key = iconKey(app)
                if (key !in missingSet) continue
                try {
                    next[key] = iconResolver.resolveBitmap(
                        app.packageName,
                        app.activityName,
                        app.userSerial,
                        ICON_PX,
                        allowStyle = true
                    )
                } catch (_: Exception) {
                    // Keep going so one bad icon cannot blank the drawer.
                }
            }
            next
        }
        _icons.value = loaded
    }

    private fun iconKey(app: App) = AppDrawerLogic.itemKey(app)

    companion object {
        private const val ICON_PX = AppIconResolver.ICON_PX
        private const val RELOAD_DEBOUNCE_MS = 250L
    }
}
