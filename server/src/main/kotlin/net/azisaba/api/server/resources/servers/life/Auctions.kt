package net.azisaba.api.server.resources.servers.life

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.util.JSON

@Serializable
@Resource("/servers/life/auctions")
class Auctions : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        call.respondJson(JSON.encodeToJsonElement(RedisManager.getAuctions()))
    }

    @Serializable
    @Resource("{id}")
    data class Id(val parent: Auctions, val id: Long) : RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            call.respondJson(JSON.encodeToJsonElement(RedisManager.getAuction(id)))
        }
    }
}
