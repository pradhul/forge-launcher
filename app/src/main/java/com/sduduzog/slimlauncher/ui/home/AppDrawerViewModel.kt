package com.sduduzog.slimlauncher.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val drawerFadePercent: Int = com.sduduzog.slimlauncher.wallpaper.LauncherFadeLogic.DEFAULT_DRAWER_FADE,
    val drawerBlurPercent: Int = com.sduduzog.slimlauncher.wallpaper.LauncherFadeLogic.DEFAULT_DRAWER_BLUR
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
    private val launchStore: LaunchStore
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _installed = MutableStateFlow<List<App>>(emptyList())
    private val _home = MutableStateFlow<List<HomeApp>>(emptyList())
    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())

    val ui: StateFlow<AppDrawerUiState> = combine(
        combine(_query, _installed, _home, _icons) { query, installed, home, icons ->
            DrawerSnapshot(query, installed, home, icons)
        },
        prefs.revision,
        launchStore.revision,
        prefs.chrome
    ) { snapshot, _, _, chrome ->
        val pins = snapshot.home.map { it.packageName to it.userSerial }
        fun itemOf(app: App) = DrawerAppItem(
            app = app,
            pinned = AppDrawerLogic.isPinned(pins, app.packageName, app.userSerial),
            icon = snapshot.icons[iconKey(app)]
        )
        val items = AppDrawerLogic.filter(snapshot.installed, snapshot.query).map(::itemOf)
        val byKey = snapshot.installed.associateBy { iconKey(it) }
        val frequentKeys = launchStore.rankedKeys()
        val frequent = if (LaunchRanker.shouldShowFrequent(snapshot.query, frequentKeys)) {
            frequentKeys.mapNotNull { byKey[it]?.let(::itemOf) }
        } else {
            emptyList()
        }
        AppDrawerUiState(
            query = snapshot.query,
            apps = items,
            canRegenerate = !prefs.appliedPackId.isNullOrBlank() &&
                store.getPack(prefs.appliedPackId!!) != null,
            theme = themeController.active(),
            frequent = frequent,
            openKeyboardOnOpen = prefs.openKeyboardOnDrawer,
            drawerFadePercent = chrome.drawerFadePercent,
            drawerBlurPercent = chrome.drawerBlurPercent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState())

    private data class DrawerSnapshot(
        val query: String,
        val installed: List<App>,
        val home: List<HomeApp>,
        val icons: Map<String, Bitmap>
    )

    private val homeObserver = androidx.lifecycle.Observer<List<HomeApp>> { list ->
        _home.value = list.orEmpty()
    }

    init {
        repository.apps.observeForever(homeObserver)
        viewModelScope.launch {
            prefs.revision.collect { reloadIcons() }
        }
        refreshInstalled()
    }

    override fun onCleared() {
        repository.apps.removeObserver(homeObserver)
        super.onCleared()
    }

    fun refreshInstalled() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = installedAppsQuery.load()
            _installed.value = apps
            prefs.appliedPackId?.let { packId ->
                store.getPack(packId)?.let { generator.fillMissing(it, apps) }
            }
            reloadIcons()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
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

    private suspend fun reloadIcons() {
        val apps = _installed.value
        val loaded = withContext(Dispatchers.IO) {
            val next = LinkedHashMap<String, Bitmap>(apps.size)
            for (app in apps) {
                try {
                    next[iconKey(app)] = iconResolver.resolveBitmap(
                        app.packageName,
                        app.activityName,
                        app.userSerial,
                        ICON_PX
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
    }
}
