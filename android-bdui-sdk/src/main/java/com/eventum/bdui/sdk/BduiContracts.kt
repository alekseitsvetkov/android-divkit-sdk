package com.eventum.bdui.sdk

typealias BduiContext = Any

interface BduiSDK {
    fun initialize(context: BduiContext)
    fun createView(): BduiView
}

interface BduiView {
    fun setJson(json: String)
}

sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Content(val json: String) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}
