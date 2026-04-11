package net.azisaba.api.server.resources.interchat

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.authentication
import io.ktor.server.request.receiveChannel
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.util.JSON
import java.util.UUID

private val client = HttpClient(CIO)
private val limiter = mutableMapOf<UUID, Long>() // <player, expire>

@Serializable
@Resource("/interchat/upload_image")
class RouteUploadImage : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val uuid = call.authentication.principal<APIKeyPrincipal>()?.player ?: return run {
            call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
        }
        if (limiter.containsKey(uuid) && limiter[uuid]!! > System.currentTimeMillis()) {
            return call.respondJson(mapOf("error" to "rate limited"), status = HttpStatusCode.TooManyRequests)
        }
        limiter[uuid] = System.currentTimeMillis() + 1000 * 10
        val response = client.post("https://worker-scripts.azisaba.workers.dev/interchat/upload_image") {
            header("Authorization", "Bearer ${ServerConfig.instance.workerScriptsApiKey}")
            header("Content-Type", "image/png")
            setBody(call.receiveChannel())
        }.bodyAsText().let { JSON.parseToJsonElement(it) as JsonObject }
        call.respondJson(response)
    }
}
