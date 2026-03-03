package com.eventum.bdui.sdk

import android.content.Context
import android.content.Intent

internal class AndroidScreenOpener(
    private val appContext: Context,
    private val hostActivity: BduiHostActivity?
) : ScreenOpener {

    override fun open(widgetUrl: String) {
        if (hostActivity == null) {
            val intent = BduiHostActivity.newIntent(appContext, widgetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return
        }
        hostActivity.pushScreen(widgetUrl)
    }

    override fun replace(widgetUrl: String) {
        if (hostActivity == null) {
            val intent = BduiHostActivity.newIntent(appContext, widgetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return
        }
        hostActivity.replaceTopScreen(widgetUrl)
    }

    override fun pop(count: Int): Boolean {
        val host = hostActivity ?: return false
        return host.popScreens(count)
    }
}
