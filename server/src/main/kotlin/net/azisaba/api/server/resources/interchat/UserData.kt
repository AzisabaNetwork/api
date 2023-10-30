package net.azisaba.api.server.resources.interchat

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.interchat.UserDataProviderImpl
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
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
        // results may be cached up to an hour
        return call.respondJson(
            mapOf(
                "prefix" to ChatMetaNodeData.toMap(UserDataProviderImpl.getChatMetaNodeDataList(uuid, "prefix")),
                "suffix" to ChatMetaNodeData.toMap(UserDataProviderImpl.getChatMetaNodeDataList(uuid, "suffix")),
            )
        )
    }
}
