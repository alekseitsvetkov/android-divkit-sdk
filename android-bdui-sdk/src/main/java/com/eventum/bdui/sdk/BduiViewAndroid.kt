package com.eventum.bdui.sdk

import android.content.Context
import android.view.View
import android.view.ContextThemeWrapper
import com.yandex.div.DivDataTag
import com.yandex.div.core.Div2Context
import com.yandex.div.core.DivConfiguration
import com.yandex.div.core.view2.Div2View
import com.yandex.div.data.DivParsingEnvironment
import com.yandex.div.json.ParsingErrorLogger
import com.yandex.div2.DivData
import org.json.JSONObject

class BduiViewAndroid(
    private val divConfiguration: DivConfiguration
) : BduiView {

    private var divView: Div2View? = null
    private var cachedJson: String? = null

    fun asView(context: Context): View {
        if (divView == null) {
            val divContext = Div2Context(ContextThemeWrapper(context, 0), divConfiguration)
            divView = Div2View(divContext)
            cachedJson?.let { applyJson(it) }
        }
        return checkNotNull(divView)
    }

    override fun setJson(json: String) {
        cachedJson = json
        applyJson(json)
    }

    private fun applyJson(json: String) {
        val view = divView ?: return
        val root = JSONObject(json)
        val card = checkNotNull(root.optJSONObject("card")) { "Invalid DivKit payload: 'card' is missing" }
        val templates = root.optJSONObject("templates")

        val environment = DivParsingEnvironment(ParsingErrorLogger.LOG)
        templates?.let(environment::parseTemplates)
        val divData = parseDivData(environment, card)

        view.setData(divData, DivDataTag("bdui"))
    }

    private fun parseDivData(environment: DivParsingEnvironment, card: JSONObject): DivData {
        val companion = DivData::class.java.getField("Companion").get(null)
        val parseMethod = companion.javaClass.methods.first { method ->
            method.name == "fromJson" && method.parameterTypes.size == 2
        }
        return parseMethod.invoke(companion, environment, card) as DivData
    }
}
