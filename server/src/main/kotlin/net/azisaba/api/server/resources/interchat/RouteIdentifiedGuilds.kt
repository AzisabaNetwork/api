package net.azisaba.api.server.resources.interchat

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.interchat.InterChatApi
import net.azisaba.api.server.interchat.JedisBoxProvider
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.interchat.api.data.PlayerPresenceData
import net.azisaba.interchat.api.network.RedisKeys

@Serializable
@Resource("/interchat/guilds/{id}")
data class RouteIdentifiedGuilds(val id: Long) {
    @Serializable
    @Resource("members")
    data class Members(val parent: RouteIdentifiedGuilds): RequestHandler() {
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
                    val presenceData = JedisBoxProvider.get().getOptional(RedisKeys.playerPresence(it.uuid().toString()), PlayerPresenceData.CODEC, 100, true)
                    val extraData = mutableMapOf<String, JsonElement>()
                    if (presenceData.isPresent) {
                        extraData["presence"] = JsonObject(mapOf(
                            "server" to JsonPrimitive(presenceData.get().server()),
                            "last_seen" to JsonPrimitive(presenceData.get().lastSeen()),
                            "cause" to JsonPrimitive(presenceData.get().cause().name),
                        ))
                    }
                    JsonObject(mapOf(
                        "guild_id" to JsonPrimitive(it.guildId()),
                        "uuid" to JsonPrimitive(it.uuid().toString()),
                        "name" to JsonPrimitive(SpicyAzisaBan.Players.getUsernameById(it.uuid())),
                        "role" to JsonPrimitive(it.role().name),
                        "nickname" to JsonPrimitive(it.nickname()),
                    )) + JsonObject(extraData)
                })
            } catch (e: Exception) {
                e.printStackTrace()
                return call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
            }
        }
    }
}
