package com.sduduzog.slimlauncher

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.sduduzog.slimlauncher.databinding.MainActivityBinding
import com.sduduzog.slimlauncher.icons.GenerationMode
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.PackCreateSpec
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import com.sduduzog.slimlauncher.icons.StylePreset
import com.sduduzog.slimlauncher.wallpaper.WallpaperThemeMonitor
import java.io.File
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.HomeWatcher
import com.sduduzog.slimlauncher.utils.IPublisher
import com.sduduzog.slimlauncher.utils.ISubscriber
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        HomeWatcher.OnHomePressedListener, IPublisher {

    private lateinit var settings: SharedPreferences
    private lateinit var navigator: NavController
    private lateinit var homeWatcher: HomeWatcher
    private lateinit var binding: MainActivityBinding
    private val subscribers: MutableSet<BaseFragment> = mutableSetOf()

    @Inject lateinit var iconPackStore: IconPackStore
    @Inject lateinit var iconPackGenerator: IconPackGenerator
    @Inject lateinit var iconPreferences: IconPreferences
    @Inject lateinit var installedAppsQuery: InstalledAppsQuery
    @Inject lateinit var packThemeExtractor: PackThemeExtractor
    @Inject lateinit var wallpaperThemeMonitor: WallpaperThemeMonitor
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dispatchBack()
        }
    }

    override fun attachSubscriber(s: ISubscriber) {
        subscribers.add(s as BaseFragment)
    }

    override fun detachSubscriber(s: ISubscriber) {
        subscribers.remove(s as BaseFragment)
    }

    private fun dispatchBack() {
        for (s in subscribers) if (s.onBack()) return
        completeBackAction()
    }

    private fun dispatchHome() {
        for (s in subscribers) s.onHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wallpaperThemeMonitor.start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                windowInsets
            }
            ViewCompat.requestApplyInsets(binding.root)
        }
        settings = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        settings.registerOnSharedPreferenceChangeListener(this)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navigator = navHostFragment.navController
        homeWatcher = HomeWatcher(this)
        homeWatcher.setOnHomePressedListener(this)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        maybeStartDebugPack(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (maybeStartDebugPack(intent)) return
        goHome()
    }

    /**
     * Debug-only hook so emulator verification can generate an on-device
     * style pack (e.g. neon) without fighting System UI ANR dialogs.
     */
    private fun maybeStartDebugPack(intent: Intent?): Boolean {
        if (!BuildConfig.DEBUG) return false
        if (intent?.getBooleanExtra(EXTRA_USE_STOCK, false) == true) {
            iconPreferences.revertToStock()
            return false
        }
        val customPath = intent?.getStringExtra(EXTRA_CUSTOM_STYLE)
        if (!customPath.isNullOrBlank()) {
            val bytes = runCatching { File(customPath).readBytes() }.getOrNull() ?: return false
            val spec = PackCreateSpec(
                name = "My photo",
                stylePrompt = "My photo",
                generatorMode = GenerationMode.ON_DEVICE,
                stylePresetId = StylePreset.CUSTOM_ID,
                styleStrength = StylePreset.CUSTOM.defaultStrength,
                customStyleBytes = bytes
            )
            val pack = iconPackStore.createPack(spec)
            packThemeExtractor.ensureTheme(pack.id)
            iconPreferences.applyPack(pack.id)
            iconPackGenerator.startPack(pack, installedAppsQuery.load())
            return true
        }
        val presetId = intent?.getStringExtra(EXTRA_GENERATE_PRESET) ?: return false
        val preset = StylePreset.byId(presetId) ?: return false
        val spec = PackCreateSpec(
            name = preset.displayName,
            stylePrompt = preset.displayName,
            generatorMode = GenerationMode.ON_DEVICE,
            stylePresetId = preset.id,
            styleStrength = preset.defaultStrength,
            customStyleBytes = null
        )
        val pack = iconPackStore.createPack(spec)
        packThemeExtractor.ensureTheme(pack.id)
        iconPreferences.applyPack(pack.id)
        iconPackGenerator.startPack(pack, installedAppsQuery.load())
        return true
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        toggleStatusBar()
    }

    override fun onStart() {
        super.onStart()
        homeWatcher.startWatch()
    }

    override fun onStop() {
        super.onStop()
        homeWatcher.stopWatch()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) toggleStatusBar()
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, s: String?) {
        if (s.equals(getString(R.string.prefs_settings_key_theme), true)) {
            recreate()
        }
        if (s.equals(getString(R.string.prefs_settings_key_toggle_status_bar), true)) {
            toggleStatusBar()
        }
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        settings = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        val active = settings.getInt(getString(R.string.prefs_settings_key_theme), 0)
        theme.applyStyle(resolveTheme(active), true)
        return theme
    }

    override fun onHomePressed() {
        goHome()
    }

    private fun goHome() {
        if (::navigator.isInitialized) {
            navigator.popBackStack(R.id.homeFragment, false)
        }
        dispatchHome()
    }

    private fun showSystemUI() {
        WindowCompat.getInsetsController(window, binding.root)
            .show(WindowInsetsCompat.Type.statusBars())
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, binding.root)
            .hide(WindowInsetsCompat.Type.statusBars())
    }

    private fun toggleStatusBar() {
        val isHidden = settings.getBoolean(getString(R.string.prefs_settings_key_toggle_status_bar), false)
        if (isHidden) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    companion object {
        const val EXTRA_GENERATE_PRESET = "forge_generate_preset"
        const val EXTRA_USE_STOCK = "forge_use_stock"
        const val EXTRA_CUSTOM_STYLE = "forge_custom_style"

        fun resolveTheme(i: Int): Int {
            return when (i) {
                1 -> R.style.AppDarkTheme
                2 -> R.style.AppGreyTheme
                3 -> R.style.AppTealTheme
                4 -> R.style.AppCandyTheme
                5 -> R.style.AppPinkTheme
                else -> R.style.AppTheme
            }
        }
    }

    private fun completeBackAction() {
        backPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        backPressedCallback.isEnabled = true
    }
}
