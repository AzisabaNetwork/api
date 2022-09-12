package net.azisaba.api.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import net.azisaba.api.server.resources.Counts
import net.azisaba.api.server.resources.Players
import net.azisaba.api.server.resources.Punishments
import net.azisaba.api.server.resources.RequestHandler
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

        authenticate("api-key") {
            get<Counts>() // /counts
            get<Players.Id>() // /players/{uuid}
            get<Players.ByName>() // /players/by-name/{uuid}
        }

        authenticate("punishments") {
            get<Players.Id.Punishments>() // /players/{uuid}/punishments
            get<Punishments.Id>() // /punishments/{id}
        }
    }
}

inline fun <reified T : RequestHandler> Route.get() = this.get<T> { this.handle(it) }

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
