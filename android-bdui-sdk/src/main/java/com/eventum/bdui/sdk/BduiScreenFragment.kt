package com.eventum.bdui.sdk

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BduiScreenFragment : Fragment(R.layout.bdui_screen_fragment) {
    private lateinit var vm: BduiScreenViewModel
    private lateinit var bduiView: BduiViewAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(
            this,
            BduiScreenVmFactory(
                repository = BduiRuntime.repository,
                extras = mapOf(BduiScreenViewModel.ARG_WIDGET_URL to requireArguments().getString(ARG_WIDGET_URL))
            )
        )[BduiScreenViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<ViewGroup>(R.id.contentContainer)
        val loading = view.findViewById<ProgressBar>(R.id.loadingView)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val errorMessage = view.findViewById<TextView>(R.id.errorMessage)
        val retryButton = view.findViewById<Button>(R.id.retryButton)

        bduiView = BduiRuntime.viewFactory.invoke()
        container.addView(bduiView.asView(requireContext()))
        retryButton.setOnClickListener { vm.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                when (state) {
                    ScreenUiState.Loading -> {
                        loading.isVisible = true
                        errorContainer.isGone = true
                        container.isGone = true
                    }
                    is ScreenUiState.Content -> {
                        loading.isGone = true
                        errorContainer.isGone = true
                        container.isVisible = true
                        bduiView.setJson(state.json)
                    }
                    is ScreenUiState.Error -> {
                        loading.isGone = true
                        container.isGone = true
                        errorContainer.isVisible = true
                        errorMessage.text = state.message
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_WIDGET_URL = "widget_url"

        fun newInstance(widgetUrl: String): BduiScreenFragment {
            return BduiScreenFragment().apply {
                arguments = Bundle().apply { putString(ARG_WIDGET_URL, widgetUrl) }
            }
        }
    }
}
