package net.azisaba.api.server.resources.interchat

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentType
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import io.ktor.util.toByteArray
import kotlinx.serialization.Serializable
import net.azisaba.api.server.resources.RequestHandler

private val client = HttpClient(CIO)

@Serializable
@Resource("/interchat/image")
class RouteImage : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val key = call.request.queryParameters["key"]
        val response = client.get("https://interchat-userdata.azisaba.net/$key.png")
        return call.respondBytes(response.bodyAsChannel().toByteArray(), response.contentType())
    }
}
