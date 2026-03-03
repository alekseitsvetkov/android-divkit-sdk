package com.eventum.bdui.sdk

data class BduiConfig(
    val allowedWidgetHosts: Set<String> = setOf("localhost", "widgets.example.com"),
    val navigationPrefix: String = "app://nav/v1",
    val routeMapper: WidgetRouteMapper = EventumRouteMapper(),
    val repositoryFactory: (android.content.Context, WidgetRouteMapper) -> WidgetRepository = { context, mapper ->
        HybridWidgetRepository(
            context = context,
            mapper = mapper
        )
    }
)
