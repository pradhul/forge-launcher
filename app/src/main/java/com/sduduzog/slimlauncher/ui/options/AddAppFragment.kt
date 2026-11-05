package com.sduduzog.slimlauncher.ui.options

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.sduduzog.slimlauncher.adapters.AddAppAdapter
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.databinding.AddAppFragmentBinding
import com.sduduzog.slimlauncher.icons.AppIconResolver
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.models.AddAppViewModel
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.OnAppClickedListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddAppFragment : BaseFragment(), OnAppClickedListener {

    private var _binding: AddAppFragmentBinding? = null
    private val binding get() = _binding
    override fun getFragmentView(): ViewGroup = binding!!.root

    private  val viewModel: AddAppViewModel by viewModels()

    @Inject
    lateinit var iconResolver: AppIconResolver

    @Inject
    lateinit var installedAppsQuery: InstalledAppsQuery

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = AddAppFragmentBinding.inflate(inflater, container, false)
        val adapter = AddAppAdapter(this, iconResolver)

        binding!!.addAppFragmentList.adapter = adapter

        viewModel.apps.observe(viewLifecycleOwner) {
            it?.let { apps ->
                adapter.setItems(apps)
                binding!!.addAppFragmentProgressBar.visibility = View.GONE
            } ?: run {
               binding!!.addAppFragmentProgressBar.visibility = View.VISIBLE
            }
        }
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.setInstalledApps(installedAppsQuery.load())
        viewModel.filterApps("")
        binding!!.addAppFragmentEditText.addTextChangedListener(onTextChangeListener)
    }

    override fun onPause() {
        super.onPause()
        binding!!.addAppFragmentEditText.removeTextChangedListener(onTextChangeListener)
    }

    private val onTextChangeListener: TextWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            // Do nothing
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Do nothing
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            viewModel.filterApps(s.toString())
        }
    }

    override fun onAppClicked(app: App) {
        viewModel.addAppToHomeScreen(app)
        Navigation.findNavController(binding!!.root).popBackStack()
    }
}