package net.azisaba.api.server.resources.interchat

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import net.azisaba.api.server.interchat.UserDataProviderImpl
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.util.hypixel.HypixelAPI
import net.azisaba.api.server.util.hypixel.HypixelPlayerData
import net.azisaba.api.util.JSON
import net.azisaba.interchat.api.data.ChatMetaNodeData
import java.util.*

private val empty: Map<String, Map<String, String>> = mapOf("prefix" to emptyMap(), "suffix" to emptyMap())

@Serializable
@Resource("/interchat/userdata")
class UserData : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val playerString = call.request.queryParameters["uuid"] ?: run {
            return call.respondJson(mapOf("error" to "uuid parameter must be specified"))
        }
        val uuid = try {
            UUID.fromString(playerString)
        } catch (e: IllegalArgumentException) {
            return call.respondJson(mapOf("error" to "invalid uuid"))
        }
        val server = call.request.queryParameters["server"] ?: run {
            return call.respondJson(empty)
        }
        if (server == "hypixel.net" || server.endsWith(".hypixel.net")) {
            val data = HypixelAPI.get<HypixelPlayerData>("https://api.hypixel.net/player?uuid=$uuid")
            return call.respondJson(mapOf(
                "prefix" to mapOf(server to data.player?.getPrefix()),
                "suffix" to emptyMap<String, String>(),
//                "raw" to JSON.encodeToJsonElement(data),
            ))
        }
        // results may be cached up to an hour
        return call.respondJson(
            mapOf(
                "prefix" to ChatMetaNodeData.toMap(UserDataProviderImpl.getChatMetaNodeDataList(uuid, "prefix")),
                "suffix" to ChatMetaNodeData.toMap(UserDataProviderImpl.getChatMetaNodeDataList(uuid, "suffix")),
            )
        )
    }
}
