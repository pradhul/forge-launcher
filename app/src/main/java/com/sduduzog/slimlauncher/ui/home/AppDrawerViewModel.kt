package com.sduduzog.slimlauncher.ui.home

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
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
    val canRegenerate: Boolean = false
)

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val repository: Repository,
    private val installedAppsQuery: InstalledAppsQuery,
    private val iconResolver: AppIconResolver,
    private val prefs: IconPreferences,
    private val store: IconPackStore,
    private val generator: IconPackGenerator
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _installed = MutableStateFlow<List<App>>(emptyList())
    private val _home = MutableStateFlow<List<HomeApp>>(emptyList())
    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())

    val ui: StateFlow<AppDrawerUiState> = combine(
        _query,
        _installed,
        _home,
        _icons,
        prefs.revision
    ) { query, installed, home, icons, _ ->
        val pins = home.map { it.packageName to it.userSerial }
        val items = AppDrawerLogic.filter(installed, query).map { app ->
            DrawerAppItem(
                app = app,
                pinned = AppDrawerLogic.isPinned(pins, app.packageName, app.userSerial),
                icon = icons[iconKey(app)]
            )
        }
        AppDrawerUiState(
            query = query,
            apps = items,
            canRegenerate = !prefs.appliedPackId.isNullOrBlank() &&
                store.getPack(prefs.appliedPackId!!) != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState())

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
            _installed.value = installedAppsQuery.load()
            reloadIcons()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
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
                val drawable = iconResolver.resolveDrawable(
                    app.packageName,
                    app.activityName,
                    app.userSerial
                ) ?: continue
                val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                    drawable.bitmap
                } else {
                    drawable.toBitmap(ICON_PX, ICON_PX)
                }
                next[iconKey(app)] = bitmap
            }
            next
        }
        _icons.value = loaded
    }

    private fun iconKey(app: App) = "${app.packageName}:${app.userSerial}"

    companion object {
        private const val ICON_PX = 96
    }
}
