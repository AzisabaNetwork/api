package net.azisaba.api.server.resources.interchat

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.interchat.InterChatApi
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson

@Suppress("unused")
@Serializable
@Resource("/interchat/guilds")
class Guilds {
    @Serializable
    @Resource("list")
    data class List(val parent: Guilds) : RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val uuid = call.authentication.principal<APIKeyPrincipal>()?.player ?: return run {
                call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
            }
            call.respondJson(
                InterChatApi.guildManager.getGuildsOf(uuid).join()
                    .map { guild ->
                        mapOf(
                            "id" to guild.id(),
                            "name" to guild.name(),
                            "format" to guild.format(),
                            "capacity" to guild.capacity(),
                            "deleted" to guild.deleted(),
                            "open" to guild.open(),
                        )
                    }
            )
        }
    }
}
