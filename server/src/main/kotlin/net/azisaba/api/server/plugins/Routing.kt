package net.azisaba.api.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import net.azisaba.api.server.resources.*

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
        webSocket<net.azisaba.api.server.resources.interchat.Stream>("/interchat/stream")

        authenticate("api-key") {
            get<Counts>() // /counts
            get<Players.Id>() // /players/{uuid}
            get<Players.ByName>() // /players/by-name/{uuid}
            get<net.azisaba.api.server.resources.servers.life.Auctions>()
            get<net.azisaba.api.server.resources.servers.life.Auctions.Id>()
            get<net.azisaba.api.server.resources.servers.life.Spawners>()
            get<net.azisaba.api.server.resources.interchat.Guilds.List>()
            get<net.azisaba.api.server.resources.interchat.IdentifiedGuilds.Members>()
            get<net.azisaba.api.server.resources.interchat.UserData>()
        }

        authenticate("punishments") {
            get<Players.Id.Punishments>() // /players/{uuid}/punishments
            get<Punishments.Id>() // /punishments/{id}
            get<Punishments.Search>() // /punishments/search
        }
    }
}

inline fun <reified T : RequestHandler> Route.get() = this.get<T> { this.handle(it) }
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
