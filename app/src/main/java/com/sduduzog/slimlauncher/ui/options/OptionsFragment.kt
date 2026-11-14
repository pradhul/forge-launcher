package com.sduduzog.slimlauncher.ui.options

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.databinding.OptionsFragmentBinding
import com.sduduzog.slimlauncher.diag.AboutDebugLog
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.IconStyle
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.wallpaper.WallpaperPackCoordinator
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

    @Inject
    lateinit var wallpaperPackCoordinator: WallpaperPackCoordinator

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
        binding!!.optionsFragmentOpenKeyboard.setOnClickListener {
            iconPreferences.openKeyboardOnDrawer = !iconPreferences.openKeyboardOnDrawer
            syncToggles()
        }
        binding!!.optionsFragmentIconStyle.setOnClickListener { showIconStyleDialog() }
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
        binding!!.optionsFragmentAbout.setOnClickListener {
            AboutDebugLog.show(this)
        }
        bindFadeControls()
        syncToggles()
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        applyWallpaperTheme()
        bindFadeControls()
        syncToggles()
    }

    private fun bindFadeControls() {
        val views = binding ?: return
        views.optionsFragmentHomeFade.progress = iconPreferences.homeFadePercent
        views.optionsFragmentDrawerBlur.progress = iconPreferences.drawerBlurPercent
        views.optionsFragmentDrawerBlur.isEnabled = true
        updateFadeLabels()
        views.optionsFragmentHomeFade.setOnSeekBarChangeListener(
            percentListener { iconPreferences.homeFadePercent = it }
        )
        views.optionsFragmentDrawerBlur.setOnSeekBarChangeListener(
            percentListener { iconPreferences.drawerBlurPercent = it }
        )
    }

    private fun syncToggles() {
        val views = binding ?: return
        views.optionsFragmentOpenKeyboardSwitch.setOnCheckedChangeListener(null)
        views.optionsFragmentOpenKeyboardSwitch.isChecked = iconPreferences.openKeyboardOnDrawer
        views.optionsFragmentOpenKeyboardSwitch.setOnCheckedChangeListener { _, checked ->
            iconPreferences.openKeyboardOnDrawer = checked
        }
        val style = iconPreferences.iconStyle
        views.optionsFragmentIconStyleDesc.text = getString(
            R.string.options_fragment_icon_style_desc,
            getString(styleLabel(style)),
            getString(styleDesc(style))
        )
    }

    private fun styleLabel(style: IconStyle): Int = when (style) {
        IconStyle.THEMED -> R.string.icon_style_themed
        IconStyle.TINTED -> R.string.icon_style_tinted
        IconStyle.ORIGINAL -> R.string.icon_style_original
        IconStyle.ARTISTIC -> R.string.icon_style_artistic
    }

    private fun styleDesc(style: IconStyle): Int = when (style) {
        IconStyle.THEMED -> R.string.icon_style_themed_desc
        IconStyle.TINTED -> R.string.icon_style_tinted_desc
        IconStyle.ORIGINAL -> R.string.icon_style_original_desc
        IconStyle.ARTISTIC -> R.string.icon_style_artistic_desc
    }

    private fun showIconStyleDialog() {
        val styles = IconStyle.values()
        val items = styles.map {
            getString(R.string.options_fragment_icon_style_desc, getString(styleLabel(it)), getString(styleDesc(it)))
        }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.options_fragment_icon_style)
            .setSingleChoiceItems(items, styles.indexOf(iconPreferences.iconStyle)) { dialog, which ->
                setIconStyle(styles[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun setIconStyle(style: IconStyle) {
        iconPreferences.iconStyle = style
        syncToggles()
        Thread {
            runCatching { wallpaperPackCoordinator.setIconStyle(style) }
        }.start()
    }

    private fun updateFadeLabels() {
        val views = binding ?: return
        views.optionsFragmentHomeFadeLabel.text = getString(
            R.string.options_fragment_home_fade,
            iconPreferences.homeFadePercent
        )
        views.optionsFragmentDrawerBlurLabel.text = getString(
            R.string.options_fragment_drawer_blur,
            iconPreferences.drawerBlurPercent
        )
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
        views.optionsFragmentHomeFade.progressTintList = accent
        views.optionsFragmentHomeFade.thumbTintList = accent
        views.optionsFragmentDrawerBlur.progressTintList = accent
        views.optionsFragmentDrawerBlur.thumbTintList = accent
        tintSwitch(views.optionsFragmentOpenKeyboardSwitch, theme.accent, theme.onBackground)
    }

    private fun tintSwitch(switch: Switch, accent: Int, onBackground: Int) {
        val thumb = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(accent, onBackground)
        )
        val track = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                (accent and 0x00FFFFFF) or 0x66000000,
                (onBackground and 0x00FFFFFF) or 0x33000000
            )
        )
        switch.thumbTintList = thumb
        switch.trackTintList = track
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
