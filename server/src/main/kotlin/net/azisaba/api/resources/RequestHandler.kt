package net.azisaba.api.resources

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import net.azisaba.api.resources.RequestHandler.Companion.getJson
import net.azisaba.api.serializers.DynamicLookupSerializer
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

sealed class RequestHandler {
    companion object {
        @JvmStatic
        @OptIn(ExperimentalSerializationApi::class)
        protected val json = Json {
            serializersModule = SerializersModule {
                contextual(Any::class, DynamicLookupSerializer)
                contextual(UUID::class, UUIDSerializer)
            }
        }

        @JvmStatic
        protected val prettyJson = Json {
            prettyPrint = true
            serializersModule = json.serializersModule
        }

        fun ApplicationCall.getJson() =
            if (request.queryParameters.contains("pretty")) {
                prettyJson
            } else {
                json
            }

        fun PipelineContext<Unit, ApplicationCall>.getJson() = call.getJson()
    }

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

// https://github.com/Kotlin/kotlinx.serialization/issues/296#issuecomment-1132714147
// (related: https://youtrack.jetbrains.com/issue/KTOR-3063)

fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

fun Map<*, *>.toJsonElement(): JsonElement = JsonObject(
    mapNotNull {
        (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement()
    }.toMap(),
)

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Map<*, *> -> toJsonElement()
    is Collection<*> -> toJsonElement()
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(this.toString())
    else -> throw IllegalStateException("Can't serialize unknown type: $this (${this::class.java.typeName})")
}
