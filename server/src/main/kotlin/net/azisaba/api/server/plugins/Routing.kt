package net.azisaba.api.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.WebSocketRequestHandler
import net.azisaba.api.server.resources.handle
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.players.ExposedPlayerRepository
import net.azisaba.api.server.players.PlayerService
import net.azisaba.api.server.players.registerPlayerPunishmentRoutes
import net.azisaba.api.server.players.registerPlayerRoutes
import net.azisaba.api.server.store.registerStoreInternalRoutes
import net.azisaba.api.server.store.registerStoreAdminRoutes

fun Application.configureRouting() {
    val playerService = PlayerService(ExposedPlayerRepository())
    routing {
        get("/") {
            call.respondJson(
                mapOf(
                    "routes" to this@routing.getAllPaths(),
                )
            )
        }

        // websocket does not support authentication
        webSocket<net.azisaba.api.server.resources.interchat.RouteStream>("/interchat/stream")
        webSocket<net.azisaba.api.server.resources.interchat.RouteStreamWeb>("/interchat/stream/web")

        get<net.azisaba.api.server.resources.Store.Products>() // /store/products
        get<net.azisaba.api.server.resources.Store.HighestSara>() // /store/players/{name}/highest_sara
        registerStoreInternalRoutes()
        registerStoreAdminRoutes()
        get<net.azisaba.api.server.resources.interchat.RouteImage>()

        authenticate("api-key") {
            get<net.azisaba.api.server.resources.RouteCounts>() // /counts
            registerPlayerRoutes(playerService)
            get<net.azisaba.api.server.resources.servers.life.RouteAuctions>()
            get<net.azisaba.api.server.resources.servers.life.RouteAuctions.Id>()
            get<net.azisaba.api.server.resources.servers.life.RouteSpawners>()
            get<net.azisaba.api.server.resources.interchat.RouteGuilds.List>()
            get<net.azisaba.api.server.resources.interchat.RouteIdentifiedGuilds.Members>()
            get<net.azisaba.api.server.resources.interchat.RouteUserData>()
            post<net.azisaba.api.server.resources.interchat.RouteUploadImage>()
        }

        authenticate("punishments") {
            registerPlayerPunishmentRoutes(playerService)
            get<net.azisaba.api.server.resources.punishments.RouteId>() // /punishments/{id}
            get<net.azisaba.api.server.resources.punishments.RouteSearch>() // /punishments/search
        }
    }
}

inline fun <reified T : RequestHandler> Route.get() = this.get<T> { this.handle(it) }
inline fun <reified T : RequestHandler> Route.post() = this.post<T> { this.handle(it) }
inline fun <reified T : WebSocketRequestHandler> Route.webSocket(path: String) = this.webSocket(path) { this.handle(T::class.java.getConstructor().newInstance()) }

fun Route.getAllRoutes(): List<Route> {
    val routes = mutableListOf(this)
    this.children.forEach {
        routes.addAll(it.getAllRoutes())
    }
    return routes
}

fun Route.getAllPaths(): Map<String, MutableList<String>> {
    val paths = mutableMapOf<String, MutableList<String>>()
    this.getAllRoutes().forEach {
        var string = it.toString()
        val method = "/\\(method:([A-Z]+)\\)".toRegex().find(string)?.groupValues?.getOrNull(1)
        if (method != null) {
            string = string.replace("/\\(.*?\\)".toRegex(), "")
            if (string.isBlank()) {
                string = "/"
            }
            paths.computeIfAbsent(method) { mutableListOf() }.add(string)
        }
    }
    return paths
}
