package net.azisaba.api.server.players

import kotlinx.serialization.json.JsonElement
import net.azisaba.api.server.util.NbtJsonDecoder
import java.util.UUID

enum class PlayerResultStatus {
    NOT_FOUND,
    FORBIDDEN,
    INTERNAL_SERVER_ERROR,
}

sealed interface PlayerResult<out T> {
    data class Success<T>(val value: T) : PlayerResult<T>

    data class Failure(
        val error: String,
        val status: PlayerResultStatus,
        val logMessage: String? = null,
        val cause: Throwable? = null,
    ) : PlayerResult<Nothing>
}

class PlayerService(
    private val repository: PlayerRepository,
    private val decodeNbt: (String) -> JsonElement = NbtJsonDecoder::decode,
) {
    fun getPlayer(uuid: UUID): PlayerResult<Map<String, Any?>> {
        val username = repository.findUsername(uuid)
            ?: return notFound("player not found")
        return PlayerResult.Success(buildPlayer(uuid, username))
    }

    fun getPlayerByName(name: String): PlayerResult<Map<String, Any?>> {
        val uuid = repository.findUuid(name)
            ?: return notFound("player not found")
        val username = repository.findUsername(uuid)
            ?: return notFound("internal error")
        return PlayerResult.Success(buildPlayer(uuid, username))
    }

    fun getPunishments(uuid: UUID): PlayerResult<List<Map<String, Any?>>> =
        PlayerResult.Success(repository.findPunishments(uuid))

    fun getPunishmentsByName(name: String): PlayerResult<List<Map<String, Any?>>> {
        val uuid = repository.findUuid(name)
            ?: return notFound("player not found")
        return getPunishments(uuid)
    }

    fun getInventory(uuid: UUID, caller: UUID?): PlayerResult<Map<String, Any?>> {
        if (!mayAccessPlayerData(caller, uuid, SharedPlayerData.INVENTORY)) {
            return forbidden()
        }
        val stored = repository.findInventory(uuid)
            ?: return notFound("inventory_data_not_found")
        val inventory = decode(stored.inventory, uuid, "inventory")
        if (inventory is PlayerResult.Failure) return inventory
        val armor = decode(stored.armor, uuid, "armor")
        if (armor is PlayerResult.Failure) return armor

        return PlayerResult.Success(
            mapOf(
                "uuid" to uuid.toString(),
                "name" to stored.playerName,
                "inventory" to (inventory as PlayerResult.Success).value,
                "armor" to (armor as PlayerResult.Success).value,
                "hotbar_slot" to stored.hotbarSlot,
                "gamemode" to stored.gamemode,
                "sync_complete" to stored.syncComplete,
                "last_seen" to stored.lastSeen,
            )
        )
    }

    fun getInventoryByName(name: String, caller: UUID?): PlayerResult<Map<String, Any?>> {
        val uuid = repository.findUuid(name)
            ?: return notFound("player_not_found")
        return getInventory(uuid, caller)
    }

    fun getEnderChest(uuid: UUID, caller: UUID?): PlayerResult<Map<String, Any?>> {
        if (!mayAccessPlayerData(caller, uuid, SharedPlayerData.ENDER_CHEST)) {
            return forbidden()
        }
        val stored = repository.findEnderChest(uuid)
            ?: return notFound("enderchest_data_not_found")
        val enderChest = decode(stored.enderChest, uuid, "ender chest")
        if (enderChest is PlayerResult.Failure) return enderChest

        return PlayerResult.Success(
            mapOf(
                "uuid" to uuid.toString(),
                "name" to stored.playerName,
                "enderchest" to (enderChest as PlayerResult.Success).value,
                "sync_complete" to stored.syncComplete,
                "last_seen" to stored.lastSeen,
            )
        )
    }

    fun getEnderChestByName(name: String, caller: UUID?): PlayerResult<Map<String, Any?>> {
        val uuid = repository.findUuid(name)
            ?: return notFound("player_not_found")
        return getEnderChest(uuid, caller)
    }

    private fun buildPlayer(uuid: UUID, username: String): Map<String, Any?> {
        val permissions = repository.findGroupPermissions(uuid)
        fun groups(server: String): List<String> = permissions
            .asSequence()
            .filter { it.server == server && it.world == "global" }
            .map { it.permission.removePrefix("group.") }
            .mapNotNull(::filterGroup)
            .distinct()
            .toList()

        fun serverTemplate(serverGroups: List<String>): Map<String, Any> = mapOf(
            "admin" to serverGroups.contains("admin"),
            "moderator" to serverGroups.contains("moderator"),
            "builder" to serverGroups.contains("builder"),
        )

        val lifeGroups = groups("life")
        val lifeEconomy = repository.findLifeEconomy(uuid)
        val life = serverTemplate(lifeGroups) + mapOf(
            "rank" to (lifeGroups
                .filter { it.startsWith("rank") }
                .map { it.removePrefix("rank") }
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull() ?: 0),
            "balance" to (lifeEconomy?.let { it.money + it.offlineMoney } ?: 0),
            "raw_balance" to (lifeEconomy?.money ?: 0),
            "raw_offline_balance" to (lifeEconomy?.offlineMoney ?: 0),
            "total_play_time" to repository.findLifePlayTime(uuid),
        )

        fun standardServer(name: String) = serverTemplate(groups(name))
        fun vipServer(name: String): Map<String, Any> {
            val serverGroups = groups(name)
            return serverTemplate(serverGroups) + mapOf("vip" to serverGroups.contains("vip"))
        }

        return mapOf(
            "uuid" to uuid.toString(),
            "name" to username,
            "groups" to groups("global"),
            "servers" to mapOf(
                "life" to life,
                "tsl" to (standardServer("tsl") + mapOf("total_play_time" to repository.findLifePlayTime(uuid))),
                "lgw" to vipServer("lgw"),
                "lgw2" to vipServer("lgw2"),
                "sclat" to standardServer("sclat"),
                "despawn" to standardServer("despawn"),
                "diverse" to standardServer("diverse"),
                "vanilife" to standardServer("vanilife"),
                "afk" to standardServer("afk"),
                "lobby" to standardServer("lobby"),
                "jg" to standardServer("jg"),
                "afnw2" to standardServer("afnw2"),
            ),
        )
    }

    private fun mayAccessPlayerData(caller: UUID?, target: UUID, data: SharedPlayerData): Boolean {
        if (caller == null) return false
        val shared = if (caller == target) null else repository.isPlayerDataShared(target, data)
        return canAccessPlayerData(caller, target, shared)
    }

    private fun decode(value: String, uuid: UUID, label: String): PlayerResult<JsonElement> =
        try {
            PlayerResult.Success(decodeNbt(value))
        } catch (e: Exception) {
            PlayerResult.Failure(
                error = "invalid_stored_player_data",
                status = PlayerResultStatus.INTERNAL_SERVER_ERROR,
                logMessage = "Could not decode MPDB $label for player $uuid",
                cause = e,
            )
        }

    private fun notFound(error: String) = PlayerResult.Failure(error, PlayerResultStatus.NOT_FOUND)

    private fun forbidden() = PlayerResult.Failure("data_sharing_disabled", PlayerResultStatus.FORBIDDEN)
}

internal fun canAccessPlayerData(caller: UUID, target: UUID, shared: Boolean?): Boolean =
    caller == target || shared == true

private fun filterGroup(rawGroup: String): String? {
    val group = rawGroup
        .removePrefix("switch")
        .removePrefix("change")
        .removePrefix("show")

    if (group.startsWith("hide")) {
        return group.removePrefix("hide") + "yen"
    }
    if (group.startsWith("punish-") ||
        group.startsWith("coretol_") ||
        group.startsWith("kill") ||
        group.startsWith("wave")
    ) {
        return null
    }
    return group.takeUnless {
        it in setOf(
            "developermember",
            "alladminmember",
            "adminmember",
            "moderatormember",
            "police",
            "member",
            "default",
        )
    }
}
