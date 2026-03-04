package com.eventum.sample

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eventum.bdui.sdk.BduiViewAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sdk = (application as SampleApp).bduiSdk
        val embeddedContainer = findViewById<ViewGroup>(R.id.embeddedContainer)
        val topBduiView = sdk.createView() as BduiViewAndroid
        val mainBduiView = sdk.createView() as BduiViewAndroid
        embeddedContainer.addView(topBduiView.asView(this))
        embeddedContainer.addView(mainBduiView.asView(this))

        lifecycleScope.launch {
            val (topJson, mainJson) = withContext(Dispatchers.IO) {
                val top = assets.open("eventum-jsons/eventum-news-top.json")
                    .bufferedReader()
                    .use { it.readText() }
                val main = assets.open("eventum-jsons/eventum-news-main.json")
                    .bufferedReader()
                    .use { it.readText() }
                top to main
            }
            topBduiView.setJson(topJson)
            mainBduiView.setJson(mainJson)
        }

    }
}
