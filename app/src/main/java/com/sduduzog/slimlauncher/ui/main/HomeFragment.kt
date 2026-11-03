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
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.sduduzog.slimlauncher.diag.AboutDebugLog
import com.sduduzog.slimlauncher.ui.home.DockLogic
import com.sduduzog.slimlauncher.ui.home.DrawerGestureLogic
import com.sduduzog.slimlauncher.ui.home.DrawerIme
import com.sduduzog.slimlauncher.ui.home.DrawerWindowLogic
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.MainViewModel
import com.sduduzog.slimlauncher.ui.home.AppDrawerOverlay
import com.sduduzog.slimlauncher.ui.home.AppDrawerViewModel
import com.sduduzog.slimlauncher.ui.icons.ForgeTheme
import com.sduduzog.slimlauncher.wallpaper.DrawerBlurWindow
import com.sduduzog.slimlauncher.wallpaper.DrawerMotionLogic
import com.sduduzog.slimlauncher.wallpaper.WallpaperThemeLogic
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener
import com.sduduzog.slimlauncher.data.model.App
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
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
    private var closeDrawerAction: Runnable? = null

    @Inject
    lateinit var iconResolver: AppIconResolver

    @Inject
    lateinit var themeController: PackThemeController

    @Inject
    lateinit var iconPreferences: IconPreferences

    @Inject
    lateinit var wallpaperThemeMonitor: com.sduduzog.slimlauncher.wallpaper.WallpaperThemeMonitor

    private lateinit var receiver: BroadcastReceiver
    private val blurEnabledListener = java.util.function.Consumer<Boolean> { syncWindowBlur() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        adapter = HomeAdapter(this, iconResolver, onLongPress = ::showHomeAppActions)
        binding!!.homeFragmentList.setHasFixedSize(true)
        binding!!.homeFragmentList.setItemViewCacheSize(16)
        binding!!.homeFragmentList.adapter = adapter
        viewModel.apps.observe(viewLifecycleOwner) { list ->
            homeApps = list.orEmpty()
            adapter.setItems(DockLogic.slots(homeApps))
            if (!drawerOpen) {
                binding!!.homeFragmentEmpty.isVisible = homeApps.isEmpty()
            }
        }
        viewModel.seedDefaultsIfNeeded()
        setEventListeners()
        binding!!.homeFragmentOverflow.setOnClickListener { showOverflowMenu(it) }
        binding!!.homeFragmentDrawerHandle.setOnClickListener { openDrawer() }
        setupDrawerOverlay()
        setupGestures()
        observeIconRevision()
        applyLauncherTheme()
        return binding!!.root
    }

    private fun setupDrawerOverlay() {
        val overlay = binding!!.homeFragmentDrawer
        overlay.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        overlay.setContent {
            val state by drawerViewModel.ui.collectAsStateWithLifecycle()
            ForgeTheme(theme = state.theme, drawerScrim = true) {
                AppDrawerOverlay(
                    visible = drawerOpen,
                    state = state,
                    onQueryChange = drawerViewModel::onQueryChange,
                    onLaunch = { app ->
                        drawerViewModel.recordLaunch(app)
                        launchInstalled(app)
                        closeDrawer()
                    },
                    onPin = { app ->
                        if (DockLogic.canAdd(homeApps.size)) {
                            viewModel.pin(app)
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                R.string.home_dock_full,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onUnpin = { viewModel.unpin(it.packageName, it.userSerial) },
                    onAppInfo = ::openAppInfo,
                    onRegenerate = drawerViewModel::regenerate,
                    onClose = { closeDrawer() }
                )
            }
        }
        overlay.visibility = DrawerWindowLogic.overlayVisibility(false)
    }

    private fun setupGestures() {
        val density = resources.displayMetrics.density
        val longPress = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true
                override fun onLongPress(e: MotionEvent) {
                    if (!drawerOpen && !swipeCommitted) showHomeMenu()
                }
            }
        )
        val swipe = View.OnTouchListener { _, event ->
            handleHomeSwipe(event, density)
            false
        }
        binding!!.root.setOnTouchListener { v, event ->
            longPress.onTouchEvent(event)
            swipe.onTouch(v, event)
            true
        }
        binding!!.homeFragmentList.setOnTouchListener(swipe)
    }

    private var swipeDownX = 0f
    private var swipeDownY = 0f
    private var swipeCommitted = false
    private var swipeVelocity: android.view.VelocityTracker? = null

    /** Commits on movement (not fling end): one short swipe up opens the drawer. */
    private fun handleHomeSwipe(event: MotionEvent, density: Float) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeDownX = event.rawX
                swipeDownY = event.rawY
                swipeCommitted = false
                swipeVelocity?.recycle()
                swipeVelocity = android.view.VelocityTracker.obtain().also { it.addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> {
                swipeVelocity?.addMovement(event)
                if (swipeCommitted || drawerOpen) return
                val dx = event.rawX - swipeDownX
                val dy = event.rawY - swipeDownY
                if (DrawerGestureLogic.shouldOpenOnMove(dy, dx, density)) {
                    swipeCommitted = true
                    openDrawer()
                } else if (dy > DrawerGestureLogic.COMMIT_DP * 2 * density && DrawerGestureLogic.isVertical(dx, dy)) {
                    swipeCommitted = true
                    expandNotificationShade()
                }
            }
            MotionEvent.ACTION_UP -> {
                val tracker = swipeVelocity
                tracker?.addMovement(event)
                tracker?.computeCurrentVelocity(1000)
                if (!swipeCommitted && !drawerOpen && tracker != null &&
                    DrawerGestureLogic.shouldOpenOnRelease(
                        event.rawY - swipeDownY,
                        event.rawX - swipeDownX,
                        tracker.yVelocity,
                        density
                    )
                ) {
                    openDrawer()
                }
                tracker?.recycle()
                swipeVelocity = null
            }
            MotionEvent.ACTION_CANCEL -> {
                swipeVelocity?.recycle()
                swipeVelocity = null
            }
        }
    }


    private fun observeIconRevision() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.iconRevision.collect {
                    applyLauncherTheme()
                    adapter.rebindIcons()
                    if (!drawerOpen) drawerViewModel.refreshInstalled(force = true)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                iconPreferences.chrome.collect {
                    applyLauncherTheme()
                    syncWindowBlur()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addCrossWindowBlurEnabledListener(
                ContextCompat.getMainExecutor(requireContext()),
                blurEnabledListener
            )
        }
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
        wallpaperThemeMonitor.onHostResume()
        adapter.rebindIcons()
        if (!drawerOpen) {
            drawerViewModel.refreshBackdrop()
            drawerViewModel.refreshInstalled(force = false)
        }
        val launch = activity?.intent
        if (com.sduduzog.slimlauncher.BuildConfig.DEBUG &&
            launch?.getBooleanExtra(com.sduduzog.slimlauncher.MainActivity.EXTRA_OPEN_DRAWER, false) == true
        ) {
            launch.removeExtra(com.sduduzog.slimlauncher.MainActivity.EXTRA_OPEN_DRAWER)
            binding?.root?.post { openDrawer() }
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wm = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeCrossWindowBlurEnabledListener(blurEnabledListener)
        }
        activity?.unregisterReceiver(receiver)
    }

    override fun onDestroyView() {
        cancelCloseDrawer()
        DrawerIme.hide(binding?.homeFragmentDrawer)
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
    }

    private fun updateClock() {
        val date = Date()
        val fWatchTime = DateFormat.getTimeInstance(DateFormat.SHORT)
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
        val theme = themeController.active()
        val content = layoutInflater.inflate(R.layout.overflow_menu, null)
        val list = content.findViewById<LinearLayout>(R.id.overflow_menu_list)
        content.setBackgroundColor(theme.background)
        val popup = PopupWindow(
            content,
            (280 * resources.displayMetrics.density).toInt(),
            LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 12f
        val items = listOf(
            OverflowItem(
                MENU_APPLY_THEME,
                R.string.menu_apply_theme,
                R.string.menu_apply_theme_desc
            ),
            OverflowItem(MENU_ICON_PACKS, R.string.menu_icon_packs, R.string.menu_icon_packs_desc),
            OverflowItem(
                MENU_WALLPAPER,
                R.string.menu_wallpaper_theme,
                R.string.menu_wallpaper_theme_desc
            ),
            OverflowItem(
                MENU_SETTINGS,
                R.string.menu_launcher_settings,
                R.string.menu_launcher_settings_desc
            ),
            OverflowItem(MENU_ABOUT, R.string.menu_about, R.string.menu_about_desc)
        )
        items.forEach { item ->
            val row = layoutInflater.inflate(R.layout.overflow_menu_item, list, false)
            val title = row.findViewById<TextView>(R.id.overflow_item_title)
            val description = row.findViewById<TextView>(R.id.overflow_item_description)
            title.text = getString(item.title)
            description.text = getString(item.description)
            title.setTextColor(theme.onBackground)
            description.setTextColor((theme.onBackground and 0x00FFFFFF) or 0xB3000000.toInt())
            row.setOnClickListener {
                popup.dismiss()
                when (item.id) {
                    MENU_APPLY_THEME -> applyThemeFromWallpaper()
                    MENU_ICON_PACKS -> openIconPacks()
                    MENU_WALLPAPER -> openWallpaper()
                    MENU_SETTINGS -> openSettings()
                    MENU_ABOUT -> AboutDebugLog.show(this)
                }
            }
            list.addView(row)
        }
        popup.showAsDropDown(anchor)
    }

    private fun applyThemeFromWallpaper() {
        Toast.makeText(requireContext(), R.string.theme_applying, Toast.LENGTH_SHORT).show()
        wallpaperThemeMonitor.applyNow(
            onDone = {
                if (!isAdded) return@applyNow
                Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyLauncherTheme() {
        val theme = themeController.active()
        val root = binding?.homeFragment ?: return
        root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val seed = theme.seed
        val homeFade = iconPreferences.homeFadePercent
        binding?.homeFragmentTopScrim?.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(WallpaperThemeLogic.homeClockScrim(seed, homeFade), 0x00000000)
        )
        binding?.homeFragmentBottomScrim?.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x00000000, WallpaperThemeLogic.homeDockScrim(seed, homeFade))
        )
        binding?.homeFragmentTime?.setTextColor(theme.onBackground)
        binding?.homeFragmentDate?.setTextColor(theme.onBackground)
        binding?.homeFragmentEmpty?.setTextColor(theme.onBackground)
        binding?.homeFragmentOverflow?.imageTintList = ColorStateList.valueOf(theme.onBackground)
        adapter.setLabelColor(theme.onBackground)
        val accent = ColorStateList.valueOf(theme.accent)
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
        cancelCloseDrawer()
        val overlay = binding?.homeFragmentDrawer ?: return
        overlay.visibility = DrawerWindowLogic.overlayVisibility(true)
        setHomeChromeVisible(false)
        drawerOpen = true
        android.util.Log.i(
            DrawerIme.TAG,
            "open overlay cached keyboard=${iconPreferences.openKeyboardOnDrawer}"
        )
        syncWindowBlur()
    }

    private fun closeDrawer() {
        if (!drawerOpen) return
        drawerOpen = false
        syncWindowBlur()
        swallowOverflowClicks()
        drawerViewModel.onQueryChange("")
        val overlay = binding?.homeFragmentDrawer
        DrawerIme.hide(overlay)
        android.util.Log.i(DrawerIme.TAG, "close overlay")
        val host = overlay ?: binding?.root ?: return
        cancelCloseDrawer()
        val action = Runnable {
            if (!DrawerWindowLogic.overlayViewVisible(drawerOpen, closeAnimationPending = false)) {
                overlay?.visibility = DrawerWindowLogic.overlayVisibility(false)
                setHomeChromeVisible(true)
            }
        }
        closeDrawerAction = action
        host.postDelayed(action, DrawerMotionLogic.ANIM_MS.toLong())
    }

    private fun cancelCloseDrawer() {
        val action = closeDrawerAction ?: return
        val host = binding?.homeFragmentDrawer ?: binding?.root
        host?.removeCallbacks(action)
        closeDrawerAction = null
    }

    private fun setHomeChromeVisible(visible: Boolean) {
        val views = binding ?: return
        val vis = DrawerWindowLogic.homeChromeVisibility(visible)
        views.homeFragmentTopScrim.visibility = vis
        views.homeFragmentBottomScrim.visibility = vis
        views.homeFragmentTime.visibility = vis
        views.homeFragmentDate.visibility = vis
        views.homeFragmentList.visibility = vis
        views.homeFragmentDrawerHandle.visibility = vis
        views.homeFragmentOverflow.visibility = vis
        views.homeFragmentEmpty.visibility = if (visible && homeApps.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun swallowOverflowClicks() {
        val button = binding?.homeFragmentOverflow ?: return
        button.isClickable = false
        button.postDelayed({ button.isClickable = true }, DrawerMotionLogic.ANIM_MS + 80L)
    }

    private fun syncWindowBlur() {
        val wm = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            wm?.isCrossWindowBlurEnabled == true
        drawerViewModel.setCrossWindowBlur(enabled)
        val window = activity?.window ?: return
        val percent = if (DrawerWindowLogic.shouldApplyActivityBlur(
                drawerOpen,
                iconPreferences.drawerBlurPercent
            )
        ) {
            iconPreferences.drawerBlurPercent
        } else {
            0
        }
        DrawerBlurWindow.apply(
            window = window,
            percent = percent,
            density = resources.displayMetrics.density,
            crossWindowEnabled = enabled,
            sdkInt = Build.VERSION.SDK_INT
        )
    }

    private fun expandNotificationShade() {
        try {
            val service = requireContext().getSystemService("statusbar") ?: return
            val method = service.javaClass.getMethod("expandNotificationsPanel")
            method.invoke(service)
        } catch (_: Exception) {
        }
    }

    override fun onBack(): Boolean {
        if (DrawerMotionLogic.backClosesDrawerOnly(drawerOpen)) {
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

    private data class OverflowItem(val id: Int, val title: Int, val description: Int)

    companion object {
        private const val MENU_APPLY_THEME = 0
        private const val MENU_ICON_PACKS = 1
        private const val MENU_WALLPAPER = 2
        private const val MENU_SETTINGS = 3
        private const val MENU_ABOUT = 4
    }
}
