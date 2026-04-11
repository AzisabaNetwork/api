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

fun Application.configureRouting() {
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
        post<net.azisaba.api.server.resources.Store.Pay>() // /store/pay
        post<net.azisaba.api.server.resources.Store.Webhook>() // /store/webhook
        get<net.azisaba.api.server.resources.Store.HighestSara>() // /store/players/{name}/highest_sara
        get<net.azisaba.api.server.resources.interchat.RouteImage>()

        authenticate("api-key") {
            get<net.azisaba.api.server.resources.RouteCounts>() // /counts
            get<net.azisaba.api.server.resources.RoutePlayers.Me>() // /players/me
            get<net.azisaba.api.server.resources.RoutePlayers.Id>() // /players/{uuid}
            get<net.azisaba.api.server.resources.RoutePlayers.ByName>() // /players/by-name/{uuid}
            get<net.azisaba.api.server.resources.servers.life.RouteAuctions>()
            get<net.azisaba.api.server.resources.servers.life.RouteAuctions.Id>()
            get<net.azisaba.api.server.resources.servers.life.RouteSpawners>()
            get<net.azisaba.api.server.resources.interchat.RouteGuilds.List>()
            get<net.azisaba.api.server.resources.interchat.RouteIdentifiedGuilds.Members>()
            get<net.azisaba.api.server.resources.interchat.RouteUserData>()
            post<net.azisaba.api.server.resources.interchat.RouteUploadImage>()
        }

        authenticate("punishments") {
            get<net.azisaba.api.server.resources.RoutePlayers.Id.Punishments>() // /players/{uuid}/punishments
            get<net.azisaba.api.server.resources.RoutePlayers.ByName.Punishments>() // /players/by-name/{uuid}/punishments
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
