package com.eventum.bdui.sdk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BduiScreenViewModel(
    private val repository: WidgetRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val widgetUrl: String = checkNotNull(savedStateHandle[ARG_WIDGET_URL])
    private val _state = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val state: StateFlow<ScreenUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenUiState.Loading
            runCatching { repository.loadJsonByUrl(widgetUrl) }
                .onSuccess { _state.value = ScreenUiState.Content(it) }
                .onFailure { _state.value = ScreenUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun retry() = load()

    companion object {
        const val ARG_WIDGET_URL = "widget_url"
    }
}

internal class BduiScreenVmFactory(
    private val repository: WidgetRepository,
    private val extras: Map<String, Any?>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val handle = SavedStateHandle(extras)
        return BduiScreenViewModel(repository, handle) as T
    }
}
