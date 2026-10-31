package com.sduduzog.slimlauncher.ui.options

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.navigation.Navigation
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.databinding.OptionsFragmentBinding
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.ui.dialogs.ChangeThemeDialog
import com.sduduzog.slimlauncher.ui.dialogs.ChooseTimeFormatDialog
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.wallpaper.LauncherFadeLogic
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OptionsFragment : BaseFragment() {

    private var _binding: OptionsFragmentBinding? = null
    private val binding get() = _binding
    override fun getFragmentView(): ViewGroup = binding!!.root

    @Inject
    lateinit var iconPreferences: IconPreferences

    @Inject
    lateinit var themeController: PackThemeController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = OptionsFragmentBinding.inflate(inflater, container, false)
        binding!!.optionsFragmentDeviceSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS)
            launchActivity(it, intent)
        }
        binding!!.optionsFragmentDeviceSettings.setOnLongClickListener {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            launchActivity(it, intent)
            true
        }
        binding!!.optionsFragmentChangeTheme.setOnClickListener {
            val changeThemeDialog = ChangeThemeDialog.getThemeChooser()
            changeThemeDialog.showNow(childFragmentManager, "THEME_CHOOSER")
        }
        binding!!.optionsFragmentChooseTimeFormat.setOnClickListener {
            val chooseTimeFormatDialog = ChooseTimeFormatDialog.getInstance()
            chooseTimeFormatDialog.showNow(childFragmentManager, "TIME_FORMAT_CHOOSER")
        }
        binding!!.optionsFragmentOpenKeyboard.setOnClickListener {
            iconPreferences.openKeyboardOnDrawer = !iconPreferences.openKeyboardOnDrawer
        }
        binding!!.optionsFragmentToggleStatusBar.setOnClickListener {
            val settings = requireContext().getSharedPreferences(
                getString(R.string.prefs_settings),
                MODE_PRIVATE
            )
            val isHidden =
                settings.getBoolean(getString(R.string.prefs_settings_key_toggle_status_bar), false)
            settings.edit {
                putBoolean(getString(R.string.prefs_settings_key_toggle_status_bar), !isHidden)
            }
        }
        binding!!.optionsFragmentToggleIcons.setOnClickListener {
            val settings = requireContext().getSharedPreferences(
                getString(R.string.prefs_settings),
                MODE_PRIVATE
            )
            val showing = settings.getBoolean(
                getString(R.string.prefs_settings_key_show_icons),
                true
            )
            settings.edit {
                putBoolean(getString(R.string.prefs_settings_key_show_icons), !showing)
            }
        }
        binding!!.optionsFragmentMatchBackground.setOnClickListener {
            iconPreferences.matchBackgroundToPack = !iconPreferences.matchBackgroundToPack
        }
        binding!!.optionsFragmentIconPacks.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_optionsFragment_to_iconPacksFragment
            )
        )
        binding!!.optionsFragmentCustomiseApps.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_optionsFragment_to_customiseAppsFragment
            )
        )
        bindFadeControls()
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        applyWallpaperTheme()
        bindFadeControls()
    }

    private fun bindFadeControls() {
        val views = binding ?: return
        views.optionsFragmentDrawerFade.progress = iconPreferences.drawerFadePercent
        views.optionsFragmentHomeFade.progress = iconPreferences.homeFadePercent
        views.optionsFragmentDrawerBlur.progress = iconPreferences.drawerBlurPercent
        val blurSupported = LauncherFadeLogic.blurSupported(Build.VERSION.SDK_INT)
        views.optionsFragmentDrawerBlur.isEnabled = blurSupported
        updateFadeLabels()
        views.optionsFragmentDrawerFade.setOnSeekBarChangeListener(
            percentListener { iconPreferences.drawerFadePercent = it }
        )
        views.optionsFragmentHomeFade.setOnSeekBarChangeListener(
            percentListener { iconPreferences.homeFadePercent = it }
        )
        views.optionsFragmentDrawerBlur.setOnSeekBarChangeListener(
            percentListener { iconPreferences.drawerBlurPercent = it }
        )
    }

    private fun updateFadeLabels() {
        val views = binding ?: return
        views.optionsFragmentDrawerFadeLabel.text = getString(
            R.string.options_fragment_drawer_fade,
            iconPreferences.drawerFadePercent
        )
        views.optionsFragmentHomeFadeLabel.text = getString(
            R.string.options_fragment_home_fade,
            iconPreferences.homeFadePercent
        )
        views.optionsFragmentDrawerBlurLabel.text = if (
            LauncherFadeLogic.blurSupported(Build.VERSION.SDK_INT)
        ) {
            getString(R.string.options_fragment_drawer_blur, iconPreferences.drawerBlurPercent)
        } else {
            getString(R.string.options_fragment_drawer_blur_unsupported)
        }
    }

    private fun percentListener(onChange: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                onChange(progress)
                updateFadeLabels()
                applyWallpaperTheme()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
    }

    private fun applyWallpaperTheme() {
        val views = binding ?: return
        val theme = themeController.active()
        views.optionsFragment.setBackgroundColor(theme.background)
        tintText(views.optionsFragment, theme.onBackground)
        val accent = ColorStateList.valueOf(theme.accent)
        views.optionsFragmentDrawerFade.progressTintList = accent
        views.optionsFragmentDrawerFade.thumbTintList = accent
        views.optionsFragmentHomeFade.progressTintList = accent
        views.optionsFragmentHomeFade.thumbTintList = accent
        views.optionsFragmentDrawerBlur.progressTintList = accent
        views.optionsFragmentDrawerBlur.thumbTintList = accent
    }

    private fun tintText(view: View, color: Int) {
        if (view is TextView) {
            view.setTextColor(color)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                tintText(view.getChildAt(index), color)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}