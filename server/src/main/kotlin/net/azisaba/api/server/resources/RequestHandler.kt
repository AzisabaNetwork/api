package net.azisaba.api.server.resources

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.util.JsonUtil.getJson
import net.azisaba.api.server.util.toJsonElement

sealed class RequestHandler {
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
    noinline configure: OutgoingContent.() -> Unit = {}
) {
    respondText(this.getJson().encodeToString(value.toJsonElement()), contentType, status, configure)
}
