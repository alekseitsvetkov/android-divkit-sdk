package com.eventum.sample

import android.app.Application
import com.eventum.bdui.sdk.BduiSDK
import com.eventum.bdui.sdk.BduiSDKAndroid

class SampleApp : Application() {
    lateinit var bduiSdk: BduiSDK

    override fun onCreate() {
        super.onCreate()
        bduiSdk = BduiSDKAndroid().apply { initialize(this@SampleApp) }
    }
}
