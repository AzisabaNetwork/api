package net.azisaba.api.server.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.NbtType
import org.cloudburstmc.nbt.NbtUtils
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class NbtJsonDecoderTest {
    @Test
    fun `decodes multiline base64 nbt with unnamed list root`() {
        val item = NbtMap.builder()
            .putString("id", "minecraft:carrot")
            .putByte("Count", 3)
            .build()
        val root = NbtList(NbtType.COMPOUND, listOf(item, NbtMap.EMPTY))
        val encoded = encode(root).chunked(16).joinToString("\r\n")

        val decoded = NbtJsonDecoder.decode(encoded).jsonObject

        assertEquals("list", decoded.getValue("type").jsonPrimitive.content)
        val listValue = decoded.getValue("value").jsonObject
        assertEquals("compound", listValue.getValue("type").jsonPrimitive.content)
        val slots = listValue.getValue("value") as JsonArray
        assertEquals(2, slots.size)
        assertEquals("minecraft:carrot", slots[0].jsonObject.getValue("id").jsonObject.getValue("value").jsonPrimitive.content)
        assertEquals(JsonObject(emptyMap()), slots[1])
    }

    @Test
    fun `preserves all nbt types and long precision`() {
        val value = NbtMap.builder()
            .putByte("byte", 1)
            .putShort("short", 2)
            .putInt("int", 3)
            .putLong("long", Long.MAX_VALUE)
            .putFloat("float", 4.5f)
            .putDouble("double", 5.5)
            .putByteArray("byteArray", byteArrayOf(1, 2))
            .putString("string", "value")
            .putList("list", NbtType.INT, 6, 7)
            .putCompound("compound", NbtMap.EMPTY)
            .putIntArray("intArray", intArrayOf(8, 9))
            .putLongArray("longArray", longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE))
            .build()

        val decoded = NbtJsonDecoder.toTypedJson(value).jsonObject.getValue("value").jsonObject

        assertEquals("9223372036854775807", decoded.getValue("long").jsonObject.getValue("value").jsonPrimitive.content)
        assertEquals("-9223372036854775808", (decoded.getValue("longArray").jsonObject.getValue("value") as JsonArray)[0].jsonPrimitive.content)
        assertEquals("int", decoded.getValue("list").jsonObject.getValue("value").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(
            setOf("byte", "short", "int", "long", "float", "double", "byteArray", "string", "list", "compound", "intArray", "longArray"),
            decoded.keys,
        )
    }

    @Test
    fun `rejects invalid base64 and invalid nbt`() {
        assertFails { NbtJsonDecoder.decode("not base64") }
        assertFails { NbtJsonDecoder.decode(Base64.getEncoder().encodeToString("not nbt".toByteArray())) }
    }

    @Test
    fun `uses fifty mebibyte nbt limit`() {
        assertEquals(52_428_800L, NbtJsonDecoder.MAX_NBT_BYTES)

        val encoded = encode(ByteArray(1024))
        assertFails { NbtJsonDecoder.decode(encoded, 1024) }
        NbtJsonDecoder.decode(encoded, 2048)
    }

    private fun encode(value: Any): String {
        val output = ByteArrayOutputStream()
        NbtUtils.createWriter(output).use { it.writeTag(value) }
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }
}
