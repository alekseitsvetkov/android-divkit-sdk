package com.eventum.bdui.sdk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

interface WidgetRouteMapper {
    fun resolve(input: String): WidgetLocation?
}

sealed interface WidgetLocation {
    data class Asset(val path: String) : WidgetLocation
    data class Remote(val url: String) : WidgetLocation
}

class EventumRouteMapper : WidgetRouteMapper {
    override fun resolve(input: String): WidgetLocation? {
        val clean = input.trim()
        return when {
            clean.endsWith("eventum-news-main.json") -> WidgetLocation.Asset("eventum-jsons/eventum-news-main.json")
            clean.endsWith("eventum-news-feed.json") || clean.endsWith("/feed") -> WidgetLocation.Asset("eventum-jsons/eventum-news-feed.json")
            clean.endsWith("eventum-news-top.json") || clean.endsWith("/top") -> WidgetLocation.Asset("eventum-jsons/eventum-news-top.json")
            clean.endsWith("eventum-news-comment.json") || clean.contains("/comment") -> WidgetLocation.Asset("eventum-jsons/eventum-news-comment.json")
            clean.endsWith("eventum-news-detail.json") || clean.contains("/feed/") -> WidgetLocation.Asset("eventum-jsons/eventum-news-detail.json")
            clean.startsWith("http://") || clean.startsWith("https://") -> WidgetLocation.Remote(clean)
            else -> null
        }
    }
}

interface WidgetRepository {
    suspend fun loadJsonByUrl(widgetUrl: String): String
}

class HybridWidgetRepository(
    private val context: Context,
    private val mapper: WidgetRouteMapper
) : WidgetRepository {

    override suspend fun loadJsonByUrl(widgetUrl: String): String = withContext(Dispatchers.IO) {
        when (val location = mapper.resolve(widgetUrl) ?: WidgetLocation.Asset(widgetUrl)) {
            is WidgetLocation.Asset -> loadFromAssets(location.path)
            is WidgetLocation.Remote -> loadFromUrl(location.url)
        }
    }

    private fun loadFromAssets(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun loadFromUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}
