package com.eventum.bdui.sdk

import android.net.Uri

internal sealed interface NavigationCommand {
    data class Push(val widgetUrl: String) : NavigationCommand
    data class Replace(val widgetUrl: String) : NavigationCommand
    data class Pop(val count: Int = 1) : NavigationCommand
}

internal interface ScreenOpener {
    fun open(widgetUrl: String)
    fun replace(widgetUrl: String)
    fun pop(count: Int = 1): Boolean
}

internal interface NavigationCommandParser {
    fun parse(rawUrl: String): NavigationCommand?
}

internal class DefaultNavigationCommandParser(
    private val allowHosts: Set<String>,
    private val navPrefix: String
) : NavigationCommandParser {

    override fun parse(rawUrl: String): NavigationCommand? {
        val uri = rawUrl.toUriOrNull() ?: return null
        if (rawUrl.startsWith("/")) return NavigationCommand.Push(rawUrl)

        val host = uri.host.orEmpty()
        val isHttp = uri.scheme == "http" || uri.scheme == "https"
        val isAllowedHttp = isHttp && (allowHosts.isEmpty() || allowHosts.contains(host))
        if (isAllowedHttp) return NavigationCommand.Push(rawUrl)

        if (!rawUrl.startsWith(navPrefix)) return null
        val command = uri.pathSegments.lastOrNull() ?: return null
        return when (command) {
            "push" -> uri.getQueryParameter("url")?.let { NavigationCommand.Push(it) }
            "replace" -> uri.getQueryParameter("url")?.let { NavigationCommand.Replace(it) }
            "pop" -> NavigationCommand.Pop(uri.getQueryParameter("count")?.toIntOrNull() ?: 1)
            else -> null
        }
    }
}

private fun String.toUriOrNull(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
