package net.azisaba.api.server.schemas

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object LifeMpdb {
    object EconomyTable : IntIdTable("mpdb_economy") { // id is unsigned!!! (but not yet supported as of 2022: https://github.com/JetBrains/Exposed/issues/199)
        val playerUUID = char("player_uuid", 36).uniqueIndex("player_uuid")
        val playerName = varchar("player_name", 16)
        val money = double("money")
        val offlineMoney = double("offline_money")
        val syncComplete = varchar("sync_complete", 5) // for some reason this is varchar rather than bool
        val lastSeen = char("last_seen", 13) // for some reason this is char rather than long...
    }

    class Economy(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Economy>(EconomyTable)

        var playerUUID by EconomyTable.playerUUID
        var playerName by EconomyTable.playerName
        var money by EconomyTable.money
        var offlineMoney by EconomyTable.offlineMoney
        var syncComplete by EconomyTable.syncComplete
        var lastSeen by EconomyTable.lastSeen
    }

    object InventoryTable : IntIdTable("mpdb_inventory") { // id is unsigned in MariaDB
        val playerUUID = char("player_uuid", 36).uniqueIndex("player_uuid")
        val playerName = varchar("player_name", 16)
        val inventory = text("inventory")
        val armor = text("armor")
        val hotbarSlot = integer("hotbar_slot")
        val gamemode = integer("gamemode")
        val syncComplete = varchar("sync_complete", 5)
        val lastSeen = char("last_seen", 13)
    }

    class Inventory(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Inventory>(InventoryTable)

        var playerUUID by InventoryTable.playerUUID
        var playerName by InventoryTable.playerName
        var inventory by InventoryTable.inventory
        var armor by InventoryTable.armor
        var hotbarSlot by InventoryTable.hotbarSlot
        var gamemode by InventoryTable.gamemode
        var syncComplete by InventoryTable.syncComplete
        var lastSeen by InventoryTable.lastSeen
    }

    object EnderChestTable : IntIdTable("mpdb_enderchest") { // id is unsigned in MariaDB
        val playerUUID = char("player_uuid", 36).uniqueIndex("player_uuid")
        val playerName = varchar("player_name", 16)
        val enderChest = text("enderchest")
        val syncComplete = varchar("sync_complete", 5)
        val lastSeen = char("last_seen", 13)
    }

    class EnderChest(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<EnderChest>(EnderChestTable)

        var playerUUID by EnderChestTable.playerUUID
        var playerName by EnderChestTable.playerName
        var enderChest by EnderChestTable.enderChest
        var syncComplete by EnderChestTable.syncComplete
        var lastSeen by EnderChestTable.lastSeen
    }
}
