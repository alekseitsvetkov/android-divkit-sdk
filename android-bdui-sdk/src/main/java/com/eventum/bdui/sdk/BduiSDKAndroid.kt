package com.eventum.bdui.sdk

import android.content.Context
import com.yandex.div.core.DivConfiguration
import com.yandex.div.picasso.PicassoDivImageLoader

class BduiSDKAndroid(
    private val config: BduiConfig = BduiConfig()
) : BduiSDK {

    private lateinit var appContext: Context
    private lateinit var divConfiguration: DivConfiguration

    override fun initialize(context: BduiContext) {
        appContext = (context as Context).applicationContext

        val parser = DefaultNavigationCommandParser(
            allowHosts = config.allowedWidgetHosts,
            navPrefix = config.navigationPrefix
        )

        BduiRuntime.repository = config.repositoryFactory(appContext, config.routeMapper)
        BduiRuntime.routeMapper = config.routeMapper

        val actionHandler = BduiDivActionHandler(
            parser = parser,
            openerProvider = {
                AndroidScreenOpener(appContext, BduiHostActivity.current())
            },
            logger = AndroidBduiLogger()
        )

        divConfiguration = DivConfiguration.Builder(PicassoDivImageLoader(appContext))
            .actionHandler(actionHandler)
            .build()
        BduiRuntime.viewFactory = { BduiViewAndroid(divConfiguration) }
    }

    override fun createView(): BduiView {
        check(::divConfiguration.isInitialized) { "Call initialize(context) before createView()" }
        return BduiViewAndroid(divConfiguration)
    }
}

internal object BduiRuntime {
    lateinit var repository: WidgetRepository
    lateinit var routeMapper: WidgetRouteMapper
    lateinit var viewFactory: () -> BduiViewAndroid
}
