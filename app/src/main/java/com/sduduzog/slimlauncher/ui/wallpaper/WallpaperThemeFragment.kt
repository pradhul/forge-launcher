package com.sduduzog.slimlauncher.ui.wallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sduduzog.slimlauncher.ui.icons.ForgeTheme
import com.sduduzog.slimlauncher.utils.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WallpaperThemeFragment : BaseFragment() {

    private var composeView: ComposeView? = null
    private val viewModel: WallpaperThemeViewModel by viewModels()

    override fun getFragmentView(): ViewGroup = composeView!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).also { view ->
            composeView = view
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            view.setContent {
                val state by viewModel.ui.collectAsStateWithLifecycle()
                ForgeTheme(theme = state.theme) {
                    WallpaperThemeScreen(
                        state = state,
                        onBingEnabled = viewModel::setBingEnabled,
                        onUnmeteredOnly = viewModel::setUnmeteredOnly,
                        onStyledIcons = viewModel::setStyledIcons,
                        onStrength = viewModel::setStrength,
                        onRefreshNow = viewModel::refreshNow
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromSystem()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }
}
