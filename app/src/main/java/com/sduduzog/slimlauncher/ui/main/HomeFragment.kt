package com.sduduzog.slimlauncher.ui.main

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.adapters.HomeAdapter
import com.sduduzog.slimlauncher.databinding.HomeFragmentBinding
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.MainViewModel
import com.sduduzog.slimlauncher.ui.home.AppDrawerOverlay
import com.sduduzog.slimlauncher.ui.home.AppDrawerViewModel
import com.sduduzog.slimlauncher.ui.icons.ForgeTheme
import com.sduduzog.slimlauncher.wallpaper.WallpaperThemeLogic
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener
import com.sduduzog.slimlauncher.data.model.App
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment : BaseFragment(), OnLaunchAppListener {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding
    private val viewModel: MainViewModel by viewModels()
    private val drawerViewModel: AppDrawerViewModel by viewModels()
    private lateinit var adapter: HomeAdapter
    private var drawerOpen by mutableStateOf(false)
    private var homeApps: List<HomeApp> = emptyList()

    @Inject
    lateinit var iconResolver: AppIconResolver

    @Inject
    lateinit var themeController: PackThemeController

    private lateinit var receiver: BroadcastReceiver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        adapter = HomeAdapter(this, iconResolver, onLongPress = ::showHomeAppActions)
        binding!!.homeFragmentList.adapter = adapter
        viewModel.apps.observe(viewLifecycleOwner) { list ->
            homeApps = list.orEmpty()
            adapter.setItems(homeApps)
            binding!!.homeFragmentEmpty.isVisible = homeApps.isEmpty()
        }
        viewModel.seedDefaultsIfNeeded()
        setEventListeners()
        binding!!.homeFragmentOverflow.setOnClickListener { showOverflowMenu(it) }
        binding!!.homeFragmentDrawerHandle.setOnClickListener { openDrawer() }
        setupGestures()
        setupDrawer()
        observeIconRevision()
        applyLauncherTheme()
        return binding!!.root
    }

    private fun setupDrawer() {
        binding!!.homeFragmentDrawer.setContent {
            val state by drawerViewModel.ui.collectAsStateWithLifecycle()
            ForgeTheme(theme = state.theme, translucent = true) {
                AppDrawerOverlay(
                    visible = drawerOpen,
                    state = state,
                    onQueryChange = drawerViewModel::onQueryChange,
                    onLaunch = { app ->
                        drawerViewModel.recordLaunch(app)
                        launchInstalled(app)
                        closeDrawer()
                    },
                    onPin = { viewModel.pin(it) },
                    onUnpin = { viewModel.unpin(it.packageName, it.userSerial) },
                    onAppInfo = ::openAppInfo,
                    onRegenerate = drawerViewModel::regenerate,
                    onIconPacks = {
                        closeDrawer()
                        openIconPacks()
                    },
                    onWallpaper = {
                        closeDrawer()
                        openWallpaper()
                    },
                    onSettings = {
                        closeDrawer()
                        openSettings()
                    },
                    onClose = { closeDrawer() }
                )
            }
        }
    }

    private fun setupGestures() {
        val fling = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || drawerOpen) return false
                val dy = e1.y - e2.y
                if (dy > 80 && abs(velocityY) > abs(velocityX)) {
                    openDrawer()
                    return true
                }
                return false
            }
        }
        val flingAndMenu = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean = fling.onFling(e1, e2, velocityX, velocityY)

            override fun onLongPress(e: MotionEvent) {
                if (!drawerOpen) showHomeMenu()
            }
        }
        val menuDetector = GestureDetectorCompat(requireContext(), flingAndMenu)
        val flingDetector = GestureDetectorCompat(requireContext(), fling)
        binding!!.root.setOnTouchListener { _, event ->
            menuDetector.onTouchEvent(event)
            false
        }
        binding!!.homeFragmentList.setOnTouchListener { _, event ->
            flingDetector.onTouchEvent(event)
            false
        }
    }

    private fun observeIconRevision() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.iconRevision.collect {
                    applyLauncherTheme()
                    adapter.notifyDataSetChanged()
                    drawerViewModel.refreshInstalled()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        receiver = ClockReceiver()
        ContextCompat.registerReceiver(
            requireContext(),
            receiver,
            IntentFilter(Intent.ACTION_TIME_TICK),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun getFragmentView(): ViewGroup = binding!!.root

    override fun onResume() {
        super.onResume()
        updateClock()
        applyLauncherTheme()
        drawerViewModel.refreshInstalled()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(receiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setEventListeners() {
        binding!!.homeFragmentTime.setOnClickListener {
            try {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                launchActivity(it, intent)
            } catch (_: ActivityNotFoundException) {
            }
        }
        binding!!.homeFragmentDate.setOnClickListener {
            try {
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                val intent = Intent(Intent.ACTION_VIEW, builder.build())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                launchActivity(it, intent)
            } catch (_: ActivityNotFoundException) {
            }
        }
        binding!!.homeFragmentCall.setOnClickListener { view ->
            try {
                val pm = context?.packageManager ?: return@setOnClickListener
                val intent = Intent(Intent.ACTION_DIAL)
                val componentName = intent.resolveActivity(pm)
                if (componentName == null) launchActivity(view, intent) else {
                    pm.getLaunchIntentForPackage(componentName.packageName)?.let {
                        launchActivity(view, it)
                    } ?: launchActivity(view, intent)
                }
            } catch (_: Exception) {
            }
        }
        binding!!.homeFragmentCamera.setOnClickListener {
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                launchActivity(it, intent)
            } catch (_: Exception) {
            }
        }
    }

    private fun updateClock() {
        val active = context?.getSharedPreferences(
            getString(R.string.prefs_settings),
            Context.MODE_PRIVATE
        )?.getInt(getString(R.string.prefs_settings_key_time_format), 0)
        val date = Date()
        val fWatchTime = when (active) {
            1 -> SimpleDateFormat("H:mm", Locale.ROOT)
            2 -> SimpleDateFormat("h:mm aa", Locale.ROOT)
            else -> DateFormat.getTimeInstance(DateFormat.SHORT)
        }
        binding?.homeFragmentTime?.text = fWatchTime.format(date)
        binding?.homeFragmentDate?.text = HomeDateFormat.format(date)
    }

    override fun onLaunch(app: HomeApp, view: View) {
        drawerViewModel.recordLaunch(App.from(app))
        launchComponent(app.packageName, app.activityName, app.userSerial, view)
    }

    private fun launchInstalled(app: App) {
        val view = binding?.root ?: return
        launchComponent(app.packageName, app.activityName, app.userSerial, view)
    }

    private fun launchComponent(
        packageName: String,
        activityName: String,
        userSerial: Long,
        view: View
    ) {
        try {
            val manager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager
            val launcher = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val componentName = ComponentName(packageName, activityName)
            val userHandle = manager.getUserForSerialNumber(userSerial)
            launcher.startMainActivity(componentName, userHandle, view.clipBounds, null)
        } catch (_: Exception) {
        }
    }

    private fun openAppInfo(app: App) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", app.packageName, null)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun showHomeAppActions(app: HomeApp) {
        val canRegen = drawerViewModel.ui.value.canRegenerate
        val options = buildList {
            add(getString(R.string.home_action_remove))
            add(getString(R.string.home_action_app_info))
            if (canRegen) add(getString(R.string.home_action_regenerate))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(app.appNickname ?: app.appName)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.home_action_remove) -> viewModel.unpin(app)
                    getString(R.string.home_action_app_info) -> openAppInfo(App.from(app))
                    getString(R.string.home_action_regenerate) -> drawerViewModel.regenerate(App.from(app))
                }
            }
            .show()
    }

    private fun showHomeMenu() {
        val anchor = binding?.homeFragmentOverflow ?: binding?.root ?: return
        showOverflowMenu(anchor)
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, MENU_ICON_PACKS, 0, R.string.menu_icon_packs)
        popup.menu.add(0, MENU_WALLPAPER, 1, R.string.menu_wallpaper_theme)
        popup.menu.add(0, MENU_SETTINGS, 2, R.string.menu_launcher_settings)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ICON_PACKS -> openIconPacks()
                MENU_WALLPAPER -> openWallpaper()
                MENU_SETTINGS -> openSettings()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.show()
    }

    private fun applyLauncherTheme() {
        val theme = themeController.active()
        val root = binding?.homeFragment ?: return
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                WallpaperThemeLogic.scrim(theme.background),
                WallpaperThemeLogic.scrim(theme.backgroundEnd, WallpaperThemeLogic.SCRIM_END_ALPHA)
            )
        )
        binding?.homeFragmentTime?.setTextColor(theme.onBackground)
        binding?.homeFragmentDate?.setTextColor(theme.onBackground)
        binding?.homeFragmentEmpty?.setTextColor(theme.onBackground)
        binding?.homeFragmentOverflow?.imageTintList = ColorStateList.valueOf(theme.onBackground)
        adapter.setLabelColor(theme.onBackground)
        val accent = ColorStateList.valueOf(theme.accent)
        binding?.homeFragmentCall?.imageTintList = accent
        binding?.homeFragmentCamera?.imageTintList = accent
    }

    private fun openIconPacks() {
        Navigation.findNavController(binding!!.root)
            .navigate(R.id.action_homeFragment_to_iconPacksFragment)
    }

    private fun openWallpaper() {
        Navigation.findNavController(binding!!.root)
            .navigate(R.id.action_homeFragment_to_wallpaperThemeFragment)
    }

    private fun openSettings() {
        Navigation.findNavController(binding!!.root)
            .navigate(R.id.action_homeFragment_to_optionsFragment)
    }

    private fun openDrawer() {
        drawerOpen = true
        binding?.homeFragmentDrawer?.isVisible = true
        drawerViewModel.refreshInstalled()
    }

    private fun closeDrawer() {
        drawerOpen = false
        binding?.homeFragmentDrawer?.isVisible = false
        drawerViewModel.onQueryChange("")
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding?.root?.windowToken, 0)
    }

    override fun onBack(): Boolean {
        if (drawerOpen) {
            closeDrawer()
            return true
        }
        return true
    }

    override fun onHome() {
        closeDrawer()
    }

    inner class ClockReceiver : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            updateClock()
        }
    }

    companion object {
        private const val MENU_ICON_PACKS = 1
        private const val MENU_WALLPAPER = 2
        private const val MENU_SETTINGS = 3
    }
}
