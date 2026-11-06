package net.azisaba.api.server.resources.servers.life

import kotlinx.serialization.encodeToString
import net.azisaba.api.data.ServerItemData
import net.azisaba.api.server.ServerItemDataEx
import net.azisaba.api.util.JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RouteItemsTest {
    @Test
    fun `serializes and deserializes ServerItemData correctly`() {
        val item = ServerItemData(
            name = "EXCALIBUR",
            material = "minecraft:diamond_sword",
            displayName = "§6Excalibur",
            lore = listOf("§7A legendary sword", "§eAttack: +10"),
            customModelData = 1001,
            amount = 1,
            unbreakable = true,
            hideFlags = listOf("HIDE_ENCHANTS"),
            enchants = mapOf("minecraft:sharpness" to 5),
            customNbt = mapOf("custom_tag" to "value"),
        )

        val json = JSON.encodeToString(ServerItemData.serializer(), item)
        val decoded = JSON.decodeFromString(ServerItemData.serializer(), json)

        assertEquals("EXCALIBUR", decoded.name)
        assertEquals("minecraft:diamond_sword", decoded.material)
        assertEquals("§6Excalibur", decoded.displayName)
        assertEquals(listOf("§7A legendary sword", "§eAttack: +10"), decoded.lore)
        assertEquals(1001, decoded.customModelData)
        assertEquals(1, decoded.amount)
        assertEquals(true, decoded.unbreakable)
        assertEquals(listOf("HIDE_ENCHANTS"), decoded.hideFlags)
        assertEquals(mapOf("minecraft:sharpness" to 5), decoded.enchants)
        assertEquals(mapOf("custom_tag" to "value"), decoded.customNbt)
    }

    @Test
    fun `groups ServerItemDataEx by child server`() {
        val item1 = ServerItemData(name = "ITEM_1", material = "minecraft:diamond")
        val item2 = ServerItemData(name = "ITEM_2", material = "minecraft:emerald")

        val list = listOf(
            ServerItemDataEx(item1, "life", "life-1"),
            ServerItemDataEx(item2, "life", "life-2"),
        )

        val grouped = list.groupBy({ it.childServer }, { it.data })
        assertEquals(2, grouped.size)
        assertEquals(listOf(item1), grouped["life-1"])
        assertEquals(listOf(item2), grouped["life-2"])
    }
}
