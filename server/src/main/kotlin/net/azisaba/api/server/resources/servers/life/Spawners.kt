package net.azisaba.api.server.resources.servers.life

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson

@Serializable
@Resource("/servers/life/spawners")
class Spawners : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val childServer = call.parameters["child_server"]
        call.respondJson(RedisManager.getSpawnerData("life", childServer).groupBy({ it.childServer }, { it.data }))
    }
}
