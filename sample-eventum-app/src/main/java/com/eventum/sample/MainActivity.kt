package com.eventum.sample

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eventum.bdui.sdk.BduiHostActivity
import com.eventum.bdui.sdk.BduiViewAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sdk = (application as SampleApp).bduiSdk
        val bduiView = sdk.createView() as BduiViewAndroid
        findViewById<ViewGroup>(R.id.embeddedContainer).addView(bduiView.asView(this))

        lifecycleScope.launch {
            val mainJson = withContext(Dispatchers.IO) {
                assets.open("eventum-jsons/eventum-news-main.json")
                    .bufferedReader()
                    .use { it.readText() }
            }
            bduiView.setJson(mainJson)
        }

        findViewById<Button>(R.id.openStandaloneButton).setOnClickListener {
            startActivity(BduiHostActivity.newIntent(this, "/feed"))
        }
    }
}
