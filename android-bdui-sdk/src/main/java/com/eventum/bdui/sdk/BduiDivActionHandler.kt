package com.eventum.bdui.sdk

import com.yandex.div.core.DivActionHandler
import com.yandex.div.core.DivViewFacade
import com.yandex.div.json.expressions.ExpressionResolver
import com.yandex.div2.DivAction

internal class BduiDivActionHandler(
    private val parser: NavigationCommandParser,
    private val openerProvider: () -> ScreenOpener,
    private val logger: BduiLogger
) : DivActionHandler() {

    override fun handleAction(
        action: DivAction,
        view: DivViewFacade,
        expressionResolver: ExpressionResolver
    ): Boolean {
        val rawUrl = action.url?.evaluate(expressionResolver)?.toString() ?: return false
        val command = parser.parse(rawUrl)
        if (command == null) {
            logger.warn("bdui_nav_invalid_action", mapOf("rawUrl" to rawUrl))
            return false
        }

        val opener = openerProvider()
        when (command) {
            is NavigationCommand.Push -> opener.open(command.widgetUrl)
            is NavigationCommand.Replace -> opener.replace(command.widgetUrl)
            is NavigationCommand.Pop -> opener.pop(command.count)
        }
        return true
    }
}
