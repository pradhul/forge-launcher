package com.sduduzog.slimlauncher.icons

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.sduduzog.slimlauncher.data.model.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
    private val stylizer: OnDeviceIconStylizer,
    private val prefs: IconPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _progress = MutableStateFlow(GenerationProgress())
    val progress: StateFlow<GenerationProgress> = _progress.asStateFlow()

    val isRunning: Boolean get() = _progress.value.running

    fun startPack(pack: IconPack, apps: List<App>) {
        val unique = apps.distinctBy { it.packageName }
        if (unique.isEmpty()) {
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
            runGeneration(pack, unique, applyWhenDone = true)
        }
    }

    fun fillMissing(pack: IconPack, apps: List<App>) {
        if (isRunning) return
        val missing = apps.distinctBy { it.packageName }
            .filter { !store.iconExists(pack.id, it.packageName) }
        if (missing.isEmpty()) return
        job = scope.launch {
            runGeneration(pack, missing, applyWhenDone = false)
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
        val palette = pack.generatorMode == GenerationMode.PALETTE
        val onDevice = pack.generatorMode == GenerationMode.ON_DEVICE
        val styleContext = if (onDevice) prepareOnDeviceStyle(pack) else null
        try {
            apps.forEachIndexed { index, app ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                _progress.update {
                    it.copy(currentIndex = index, currentAppName = app.appName)
                }
                try {
                    val bytes = if (palette) {
                        generatePalette(app, pack)
                    } else if (onDevice && styleContext != null) {
                        generateOnDeviceOrFallback(app, pack, styleContext)
                    } else {
                        val reference = iconResolver.stockIconPng(
                            app.packageName,
                            app.activityName,
                            app.userSerial
                        )
                        client.generateIcon(app.appName, pack.stylePrompt, reference)
                    }
                    store.saveIcon(
                        pack.id,
                        app.packageName,
                        if (onDevice || palette) bytes else ImageUtils.downscaleToSquarePng(bytes)
                    )
                    prefs.notifyIconsChanged()
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
                if (!onDevice && !palette) {
                    delay(GEMINI_PACING_MS)
                }
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

    private fun prepareOnDeviceStyle(pack: IconPack): OnDeviceStyleContext {
        val preset = StylePreset.byId(pack.stylePresetId)
        val styleBitmap = store.getCustomStyleFile(pack.id)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)
        } ?: stylizer.loadPresetStyle(preset)
        val bottleneck = stylizer.predictStyleBottleneck(styleBitmap)
        return OnDeviceStyleContext(preset, styleBitmap, bottleneck)
    }

    private fun generatePalette(app: App, pack: IconPack): ByteArray {
        val drawable = stockDrawable(app) ?: iconResolver.fallbackIcon()
        val theme = pack.theme ?: prefs.wallpaperTheme ?: PackTheme.STOCK
        return stylizer.recolorWithPalette(drawable, theme, pack.styleStrength)
    }

    private fun generateOnDeviceOrFallback(
        app: App,
        pack: IconPack,
        style: OnDeviceStyleContext
    ): ByteArray {
        return try {
            generateOnDevice(app, pack, style)
        } catch (_: Exception) {
            val drawable = stockDrawable(app) ?: iconResolver.fallbackIcon()
            stylizer.styleWithoutTransfer(drawable, style.preset)
        }
    }

    private fun generateOnDevice(
        app: App,
        pack: IconPack,
        style: OnDeviceStyleContext
    ): ByteArray {
        val drawable = stockDrawable(app)
            ?: throw IllegalStateException("No icon for ${app.packageName}")
        return if (style.bottleneck != null) {
            stylizer.stylizeWithBottleneck(
                drawable,
                style.bottleneck,
                style.preset,
                pack.styleStrength
            )
        } else {
            stylizer.stylizeIcon(drawable, style.styleBitmap, style.preset, pack.styleStrength)
        }
    }

    private fun stockDrawable(app: App): Drawable? {
        return iconResolver.loadStockIcon(app.packageName, app.activityName, app.userSerial)
    }

    private data class OnDeviceStyleContext(
        val preset: StylePreset,
        val styleBitmap: android.graphics.Bitmap,
        val bottleneck: FloatArray?
    )

    companion object {
        private const val GEMINI_PACING_MS = 1_200L
    }
}
