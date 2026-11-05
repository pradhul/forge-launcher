package com.sduduzog.slimlauncher.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.ui.home.HomeGridLogic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val prefs: IconPreferences,
    private val installedAppsQuery: InstalledAppsQuery
) : ViewModel() {

    val apps: LiveData<List<HomeApp>> = repository.apps
    val iconRevision: StateFlow<Int> = prefs.revision

    fun seedDefaultsIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (prefs.homeDefaultsSeeded) return@launch
            if (repository.countApps() > 0) {
                prefs.homeDefaultsSeeded = true
                return@launch
            }
            val defaults = HomeDefaults.pick(installedAppsQuery.load(), HomeGridLogic.DEFAULT_COUNT)
            defaults.forEachIndexed { index, app ->
                repository.add(HomeApp.from(app, index))
            }
            prefs.homeDefaultsSeeded = true
        }
    }

    fun pin(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            val already = HomeGridLogic.isOnHome(
                repository.getAppsSync(), app.packageName, app.activityName, app.userSerial
            )
            if (already) return@launch
            val count = repository.countApps()
            if (!HomeGridLogic.canAdd(count)) return@launch
            repository.add(HomeApp.from(app, count))
        }
    }

    fun unpin(app: HomeApp) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.remove(app)
        }
    }

    fun unpin(packageName: String, userSerial: Long) {
        unpin(packageName, activityName = null, userSerial = userSerial)
    }

    fun unpin(packageName: String, activityName: String?, userSerial: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val match = repository.getAppsSync().firstOrNull {
                it.packageName == packageName &&
                    it.userSerial == userSerial &&
                    (activityName == null || it.activityName == activityName)
            } ?: return@launch
            repository.remove(match)
        }
    }

    fun reorder(apps: List<HomeApp>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(*apps.toTypedArray())
        }
    }
}
