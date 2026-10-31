package com.sduduzog.slimlauncher.icons

import com.sduduzog.slimlauncher.data.model.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class GenerationProgress(
    val packId: String? = null,
    val packName: String = "",
    val running: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val currentAppName: String = "",
    val succeeded: Int = 0,
    val failed: Int = 0,
    val lastError: String? = null,
    val cancelled: Boolean = false,
    val completed: Boolean = false
)

@Singleton
class IconPackGenerator @Inject constructor(
    private val store: IconPackStore,
    private val client: GeminiIconClient,
    private val iconResolver: AppIconResolver,
    private val prefs: IconPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _progress = MutableStateFlow(GenerationProgress())
    val progress: StateFlow<GenerationProgress> = _progress.asStateFlow()

    val isRunning: Boolean get() = _progress.value.running

    fun startPack(pack: IconPack, apps: List<App>) {
        if (apps.isEmpty()) {
            _progress.value = GenerationProgress(
                packId = pack.id,
                packName = pack.name,
                lastError = "No installed apps to generate icons for",
                completed = true
            )
            return
        }
        job?.cancel()
        job = scope.launch {
            runGeneration(pack, apps, applyWhenDone = true)
        }
    }

    fun regenerate(packId: String, app: App) {
        val pack = store.getPack(packId) ?: return
        if (_progress.value.running) return
        job = scope.launch {
            runGeneration(pack, listOf(app), applyWhenDone = false)
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun runGeneration(
        pack: IconPack,
        apps: List<App>,
        applyWhenDone: Boolean
    ) {
        _progress.value = GenerationProgress(
            packId = pack.id,
            packName = pack.name,
            running = true,
            total = apps.size
        )
        try {
            apps.forEachIndexed { index, app ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                _progress.update {
                    it.copy(
                        currentIndex = index,
                        currentAppName = app.appName
                    )
                }
                try {
                    val reference = iconResolver.stockIconPng(
                        app.packageName,
                        app.activityName,
                        app.userSerial
                    )
                    val generated = client.generateIcon(app.appName, pack.stylePrompt, reference)
                    store.saveIcon(
                        pack.id,
                        app.packageName,
                        ImageUtils.downscaleToSquarePng(generated)
                    )
                    _progress.update { it.copy(succeeded = it.succeeded + 1, lastError = null) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: GeminiException) {
                    _progress.update {
                        it.copy(failed = it.failed + 1, lastError = error.message)
                    }
                    if (error.isFatal) return
                } catch (_: Exception) {
                    _progress.update {
                        it.copy(
                            failed = it.failed + 1,
                            lastError = "Could not generate an icon for ${app.appName}"
                        )
                    }
                }
                delay(REQUEST_PACING_MS)
            }
        } catch (_: CancellationException) {
            _progress.update {
                it.copy(running = false, cancelled = true, completed = false)
            }
            return
        }
        val succeeded = _progress.value.succeeded
        if (applyWhenDone && succeeded > 0) {
            prefs.applyPack(pack.id)
        }
        _progress.update {
            it.copy(running = false, completed = true, currentAppName = "")
        }
    }

    companion object {
        private const val REQUEST_PACING_MS = 400L
    }
}
