package net.azisaba.api.server.resources.punishments

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import net.azisaba.api.server.resources.RequestHandler

@Serializable
@Resource("/punishments")
class RouteRoot : RequestHandler() {
    override suspend fun RoutingContext.handleRequest() {
        TODO("Not yet implemented")
    }
}
