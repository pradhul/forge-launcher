package com.sduduzog.slimlauncher.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
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
            val defaults = HomeDefaults.pick(installedAppsQuery.load())
            defaults.forEachIndexed { index, app ->
                repository.add(HomeApp.from(app, index))
            }
            prefs.homeDefaultsSeeded = true
        }
    }

    fun pin(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            val already = repository.getAppsSync().any {
                it.packageName == app.packageName && it.userSerial == app.userSerial
            }
            if (already) return@launch
            repository.add(HomeApp.from(app, repository.countApps()))
        }
    }

    fun unpin(app: HomeApp) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.remove(app)
        }
    }

    fun unpin(packageName: String, userSerial: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val match = repository.getAppsSync().firstOrNull {
                it.packageName == packageName && it.userSerial == userSerial
            } ?: return@launch
            repository.remove(match)
        }
    }
}
