package com.eventum.bdui.sdk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import java.lang.ref.WeakReference

class BduiHostActivity : AppCompatActivity(R.layout.bdui_host_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentRef = WeakReference(this)

        if (savedInstanceState == null) {
            val firstUrl = checkNotNull(intent.getStringExtra(EXTRA_WIDGET_URL))
            supportFragmentManager.commit {
                replace(R.id.bduiHostContainer, BduiScreenFragment.newInstance(firstUrl))
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!popScreens(1)) finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentRef?.get() === this) currentRef = null
    }

    fun pushScreen(widgetUrl: String) {
        supportFragmentManager.commit {
            replace(R.id.bduiHostContainer, BduiScreenFragment.newInstance(widgetUrl))
            addToBackStack(widgetUrl)
        }
    }

    fun replaceTopScreen(widgetUrl: String) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        pushScreen(widgetUrl)
    }

    fun popScreens(count: Int): Boolean {
        var left = count.coerceAtLeast(1)
        var didPop = false
        while (left > 0 && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
            left--
            didPop = true
        }
        return didPop
    }

    companion object {
        private const val EXTRA_WIDGET_URL = "widget_url"
        private var currentRef: WeakReference<BduiHostActivity>? = null

        fun newIntent(context: Context, widgetUrl: String): Intent {
            return Intent(context, BduiHostActivity::class.java)
                .putExtra(EXTRA_WIDGET_URL, widgetUrl)
        }

        fun current(): BduiHostActivity? = currentRef?.get()
    }
}
