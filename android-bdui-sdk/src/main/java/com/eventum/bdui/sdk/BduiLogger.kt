package com.eventum.bdui.sdk

import android.util.Log

internal interface BduiLogger {
    fun warn(tag: String, details: Map<String, String> = emptyMap())
}

internal class AndroidBduiLogger : BduiLogger {
    override fun warn(tag: String, details: Map<String, String>) {
        Log.w("BDUI", "$tag: $details")
    }
}
