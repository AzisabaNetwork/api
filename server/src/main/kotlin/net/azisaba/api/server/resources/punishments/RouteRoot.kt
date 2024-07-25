package net.azisaba.api.server.resources.punishments

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.resources.RequestHandler

@Serializable
@Resource("/punishments")
class RouteRoot : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        TODO("Not yet implemented")
    }
}
