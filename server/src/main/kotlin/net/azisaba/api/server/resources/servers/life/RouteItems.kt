package net.azisaba.api.server.resources.servers.life

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson

@Serializable
@Resource("/servers/life/items")
class RouteItems : RequestHandler() {
    override suspend fun RoutingContext.handleRequest() {
        val childServer = call.parameters["child_server"]
        call.respondJson(RedisManager.getItemData("life", childServer).groupBy({ it.childServer }, { it.data }))
    }

    @Serializable
    @Resource("{name}")
    data class Name(val parent: RouteItems, val name: String) : RequestHandler() {
        override suspend fun RoutingContext.handleRequest() {
            val childServer = call.parameters["child_server"]
            val item = RedisManager.getItem("life", childServer, name)
            if (item != null) {
                call.respondJson(item.data)
            } else {
                call.respondJson(mapOf("error" to "Item not found"), status = HttpStatusCode.NotFound)
            }
        }
    }
}
