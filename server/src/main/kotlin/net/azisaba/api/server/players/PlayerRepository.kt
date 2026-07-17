package net.azisaba.api.server.players

import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.LifeMpdb
import net.azisaba.api.server.schemas.LifeStatz
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.schemas.SpicyAzisaBan
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class PlayerGroupPermission(
    val permission: String,
    val server: String,
    val world: String,
)

data class PlayerEconomy(
    val money: Double,
    val offlineMoney: Double,
)

data class StoredPlayerInventory(
    val playerName: String,
    val inventory: String,
    val armor: String,
    val hotbarSlot: Int,
    val gamemode: Int,
    val syncComplete: String,
    val lastSeen: String,
)

data class StoredPlayerEnderChest(
    val playerName: String,
    val enderChest: String,
    val syncComplete: String,
    val lastSeen: String,
)

enum class SharedPlayerData {
    INVENTORY,
    ENDER_CHEST,
}

interface PlayerRepository {
    fun findUsername(uuid: UUID): String?

    fun findUuid(username: String): UUID?

    fun findPunishments(uuid: UUID): List<Map<String, Any?>>

    fun findGroupPermissions(uuid: UUID): List<PlayerGroupPermission>

    fun findLifeEconomy(uuid: UUID): PlayerEconomy?

    fun findLifePlayTime(uuid: UUID): Long

    fun isPlayerDataShared(uuid: UUID, data: SharedPlayerData): Boolean

    fun findInventory(uuid: UUID): StoredPlayerInventory?

    fun findEnderChest(uuid: UUID): StoredPlayerEnderChest?
}

class ExposedPlayerRepository : PlayerRepository {
    override fun findUsername(uuid: UUID): String? =
        SpicyAzisaBan.Players.getUsernameById(uuid)

    override fun findUuid(username: String): UUID? =
        SpicyAzisaBan.Players.getIdByUsername(username)

    override fun findPunishments(uuid: UUID): List<Map<String, Any?>> =
        transaction(DatabaseManager.spicyAzisaBan) {
            SpicyAzisaBan.PunishmentHistory
                .find(SpicyAzisaBan.PunishmentHistoryTable.target eq uuid.toString())
                .map { it.toMap() }
        }

    override fun findGroupPermissions(uuid: UUID): List<PlayerGroupPermission> =
        transaction(DatabaseManager.luckPerms) {
            LuckPerms.UserPermissions.getGroupsForPlayer(uuid).map {
                PlayerGroupPermission(
                    permission = it.permission,
                    server = it.server,
                    world = it.world,
                )
            }
        }

    override fun findLifeEconomy(uuid: UUID): PlayerEconomy? =
        transaction(DatabaseManager.lifeMpdb) {
            LifeMpdb.Economy
                .find(LifeMpdb.EconomyTable.playerUUID eq uuid.toString())
                .firstOrNull()
                ?.let { PlayerEconomy(it.money, it.offlineMoney) }
        }

    override fun findLifePlayTime(uuid: UUID): Long =
        transaction(DatabaseManager.lifeStatz) {
            LifeStatz.StatzTimePlayed.getPlayTime(uuid)
        }

    override fun isPlayerDataShared(uuid: UUID, data: SharedPlayerData): Boolean =
        transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.PlayerDataPrivacyTable
                .select { AzisabaAPI.PlayerDataPrivacyTable.playerUUID eq uuid.toString() }
                .firstOrNull()
                ?.let {
                    when (data) {
                        SharedPlayerData.INVENTORY -> it[AzisabaAPI.PlayerDataPrivacyTable.shareInventory]
                        SharedPlayerData.ENDER_CHEST -> it[AzisabaAPI.PlayerDataPrivacyTable.shareEnderChest]
                    }
                }
                ?: false
        }

    override fun findInventory(uuid: UUID): StoredPlayerInventory? =
        transaction(DatabaseManager.lifeMpdb) {
            LifeMpdb.Inventory
                .find(LifeMpdb.InventoryTable.playerUUID eq uuid.toString())
                .firstOrNull()
                ?.let {
                    StoredPlayerInventory(
                        playerName = it.playerName,
                        inventory = it.inventory,
                        armor = it.armor,
                        hotbarSlot = it.hotbarSlot,
                        gamemode = it.gamemode,
                        syncComplete = it.syncComplete,
                        lastSeen = it.lastSeen,
                    )
                }
        }

    override fun findEnderChest(uuid: UUID): StoredPlayerEnderChest? =
        transaction(DatabaseManager.lifeMpdb) {
            LifeMpdb.EnderChest
                .find(LifeMpdb.EnderChestTable.playerUUID eq uuid.toString())
                .firstOrNull()
                ?.let {
                    StoredPlayerEnderChest(
                        playerName = it.playerName,
                        enderChest = it.enderChest,
                        syncComplete = it.syncComplete,
                        lastSeen = it.lastSeen,
                    )
                }
        }
}
