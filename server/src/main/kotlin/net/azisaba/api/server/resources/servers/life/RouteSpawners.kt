package net.azisaba.api.server.resources.servers.life

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson

@Serializable
@Resource("/servers/life/spawners")
class RouteSpawners : RequestHandler() {
    override suspend fun RoutingContext.handleRequest() {
        val childServer = call.parameters["child_server"]
        call.respondJson(RedisManager.getSpawnerData("life", childServer).groupBy({ it.childServer }, { it.data }))
    }
}
