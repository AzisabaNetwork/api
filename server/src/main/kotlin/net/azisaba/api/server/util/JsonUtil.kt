package net.azisaba.api.server.util

import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.azisaba.api.util.JSON

object JsonUtil {
    val prettyJson = Json {
        prettyPrint = true
        serializersModule = JSON.serializersModule
    }

    fun ApplicationCall.getJson() =
        if (request.queryParameters.contains("pretty")) {
            prettyJson
        } else {
            JSON
        }
}

// hacks (workaround) for kotlinx.serialization messing with map of <String, Any?>
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

