package net.azisaba.api.server.resources

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.util.JsonUtil.getJson
import net.azisaba.api.server.util.toJsonElement

abstract class RequestHandler {
    abstract suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest()
}

suspend fun PipelineContext<Unit, ApplicationCall>.handle(handler: RequestHandler) {
    with(handler) {
        handleRequest()
    }
}

suspend inline fun <reified T> ApplicationCall.respondJson(
    value: T,
    contentType: ContentType? = ContentType.Application.Json,
    status: HttpStatusCode? = null,
    configure: OutgoingContent.() -> Unit = {},
) {
    try {
        val text = this.getJson().encodeToString(value.toJsonElement())
        val message = TextContent(text, defaultTextContentType(contentType), status).apply(configure)
        respond(message)
    } catch (e: SerializationException) {
        try {
            val text = this.getJson().encodeToString(value)
            val message = TextContent(text, defaultTextContentType(contentType), status).apply(configure)
            respond(message)
        } catch (e2: Exception) {
            e2.addSuppressed(e)
            throw e2
        }
    }
}
