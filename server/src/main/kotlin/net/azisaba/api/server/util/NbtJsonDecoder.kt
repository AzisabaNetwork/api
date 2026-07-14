package net.azisaba.api.server.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.NbtType
import org.cloudburstmc.nbt.NbtUtils
import java.io.ByteArrayInputStream
import java.util.Base64

object NbtJsonDecoder {
    const val MAX_NBT_BYTES = 50L * 1024L * 1024L
    const val MAX_DEPTH = 64

    fun decode(encoded: String): JsonElement = decode(encoded, MAX_NBT_BYTES)

    internal fun decode(encoded: String, maxNbtBytes: Long): JsonElement {
        val normalized = encoded.filterNot(Char::isWhitespace)
        val nbt = Base64.getDecoder().decode(normalized)
        return NbtUtils.createReader(ByteArrayInputStream(nbt), maxNbtBytes).use {
            toTypedJson(it.readTag(MAX_DEPTH))
        }
    }

    internal fun toTypedJson(value: Any): JsonElement = typed(typeName(value), payload(value))

    private fun payload(value: Any): JsonElement = when (value) {
        is Byte -> JsonPrimitive(value)
        is Short -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value.toString())
        is Float -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is ByteArray -> JsonArray(value.map(::JsonPrimitive))
        is IntArray -> JsonArray(value.map(::JsonPrimitive))
        is LongArray -> JsonArray(value.map { JsonPrimitive(it.toString()) })
        is NbtMap -> JsonObject(value.entries.associate { (name, child) -> name to toTypedJson(child) })
        is NbtList<*> -> JsonObject(
            mapOf(
                "type" to JsonPrimitive(typeName(value.type)),
                "value" to JsonArray(value.map { payload(requireNotNull(it)) }),
            )
        )
        else -> throw IllegalArgumentException("Unsupported NBT value type: ${value.javaClass.name}")
    }

    private fun typed(type: String, value: JsonElement): JsonObject = JsonObject(
        mapOf(
            "type" to JsonPrimitive(type),
            "value" to value,
        )
    )

    private fun typeName(value: Any): String = when (value) {
        is Byte -> "byte"
        is Short -> "short"
        is Int -> "int"
        is Long -> "long"
        is Float -> "float"
        is Double -> "double"
        is ByteArray -> "byteArray"
        is String -> "string"
        is NbtList<*> -> "list"
        is NbtMap -> "compound"
        is IntArray -> "intArray"
        is LongArray -> "longArray"
        else -> throw IllegalArgumentException("Unsupported NBT value type: ${value.javaClass.name}")
    }

    private fun typeName(type: NbtType<*>): String = when (type) {
        NbtType.BYTE -> "byte"
        NbtType.SHORT -> "short"
        NbtType.INT -> "int"
        NbtType.LONG -> "long"
        NbtType.FLOAT -> "float"
        NbtType.DOUBLE -> "double"
        NbtType.BYTE_ARRAY -> "byteArray"
        NbtType.STRING -> "string"
        NbtType.LIST -> "list"
        NbtType.COMPOUND -> "compound"
        NbtType.INT_ARRAY -> "intArray"
        NbtType.LONG_ARRAY -> "longArray"
        else -> throw IllegalArgumentException("Unsupported NBT tag type: ${type.typeName}")
    }
}
