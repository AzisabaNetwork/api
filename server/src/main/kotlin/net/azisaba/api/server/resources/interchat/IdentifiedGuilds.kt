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

@Serializable
@Resource("/interchat/guilds/{id}")
data class IdentifiedGuilds(val id: Long) {
    @Serializable
    @Resource("members")
    data class Members(val parent: IdentifiedGuilds): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val uuid = call.authentication.principal<APIKeyPrincipal>()?.player ?: return run {
                call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
            }
            try {
                val members = InterChatApi.guildManager.getMembers(parent.id).join()
                if (members.none { it.uuid() == uuid }) {
                    return call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
                }
                call.respondJson(members.map {
                    mapOf(
                        "guild_id" to it.guildId(),
                        "uuid" to it.uuid().toString(),
                        "role" to it.role().name,
                        "nickname" to it.nickname(),
                    )
                })
            } catch (e: Exception) {
                return call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
            }
        }
    }
}
