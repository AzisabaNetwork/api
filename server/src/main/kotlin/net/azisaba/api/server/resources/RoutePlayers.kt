package net.azisaba.api.server.resources

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.authentication
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.LifeMpdb
import net.azisaba.api.server.schemas.LifeStatz
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.serializers.UUIDSerializer
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.resources.RoutePlayers.Id.Companion.toMap
import net.azisaba.api.server.util.NbtJsonDecoder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
@Resource("/players")
class RoutePlayers {
    @Serializable
    @Resource("{uuid}")
    data class Id(
        @Suppress("unused")
        val parent: RoutePlayers,
        @Serializable(with = UUIDSerializer::class)
        val uuid: UUID,
    ): RequestHandler() {
        companion object {
            fun toMap(uuid: UUID, username: String): Map<String, Any?> =
                mapOf(
                    "uuid" to (uuid.toString() as Any).toString(),
                    "name" to username,
                    "groups" to getGroups(uuid, "global"),
                    "servers" to mapOf(
                        "life" to getLife(uuid),
                        "tsl" to getTSL(uuid),
                        "lgw" to getLGW(uuid),
                        "lgw2" to getLGW2(uuid),
                        "sclat" to getSclat(uuid),
                        "despawn" to getDespawn(uuid),
                        "diverse" to getDiverse(uuid),
                        "vanilife" to getVanilife(uuid),
                        "afk" to getAfk(uuid),
                        "lobby" to getLobby(uuid),
                        "jg" to getJG(uuid),
                        "afnw2" to getAfnw2(uuid),
                    )
                )

            private fun getServerTemplate(uuid: UUID, server: String): Map<String, Any> {
                val groups = getGroups(uuid, server)
                return getServerTemplate(groups)
            }

            private fun getServerTemplate(groups: List<String>): Map<String, Any> {
                return mapOf(
                    "admin" to groups.contains("admin"),
                    "moderator" to groups.contains("moderator"),
                    "builder" to groups.contains("builder"),
                )
            }

            private fun getLife(uuid: UUID): Map<String, Any> {
                val groups = getGroups(uuid, "life")
                val rank = groups
                    .filter { it.startsWith("rank") }
                    .map { it.removePrefix("rank") }
                    .mapNotNull { it.toIntOrNull() }
                    .maxOrNull()
                    ?: 0
                val eco = transaction(DatabaseManager.lifeMpdb) {
                    LifeMpdb.Economy.find(LifeMpdb.EconomyTable.playerUUID eq uuid.toString()).firstOrNull()
                }
                return getServerTemplate(uuid, "life") + mapOf(
                    "rank" to rank,
                    "balance" to (eco?.money?.plus(eco.offlineMoney) ?: 0),
                    "raw_balance" to (eco?.money ?: 0),
                    "raw_offline_balance" to (eco?.offlineMoney ?: 0),
                    "total_play_time" to transaction(DatabaseManager.lifeStatz) { LifeStatz.StatzTimePlayed.getPlayTime(uuid) },
                )
            }

            private fun getTSL(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "tsl") + mapOf(
                    "total_play_time" to transaction(DatabaseManager.lifeStatz) { LifeStatz.StatzTimePlayed.getPlayTime(uuid) },
                )
            }

            private fun getLGW(uuid: UUID): Map<String, Any> {
                val groups = getGroups(uuid, "lgw")
                return getServerTemplate(groups) + mapOf(
                    "vip" to groups.contains("vip"),
                )
            }

            private fun getLGW2(uuid: UUID): Map<String, Any> {
                val groups = getGroups(uuid, "lgw2")
                return getServerTemplate(groups) + mapOf(
                    "vip" to groups.contains("vip"),
                )
            }

            private fun getSclat(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "sclat")
            }

            private fun getDespawn(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "despawn")
            }

            private fun getDiverse(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "diverse")
            }

            private fun getVanilife(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "vanilife")
            }

            private fun getJG(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "jg")
            }

            private fun getAfnw2(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "afnw2")
            }

            private fun getAfk(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "afk")
            }

            private fun getLobby(uuid: UUID): Map<String, Any> {
                return getServerTemplate(uuid, "lobby")
            }

            private fun getGroups(uuid: UUID, server: String) =
                LuckPerms.UserPermissions.getGroupsForPlayer(uuid)
                    .filter { it.server == server && it.world == "global" }
                    .map { it.permission.removePrefix("group.") }
                    .mapNotNull { filterGroup(it) }
                    .distinct()

            private fun filterGroup(rawGroup: String): String? {
                val group = rawGroup
                    .removePrefix("switch") // /switch'd admin/moderator/builder groups
                    .removePrefix("change") // sara
                    .removePrefix("show") // sara

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
                when (group) {
                    "developermember",
                    "alladminmember",
                    "adminmember",
                    "moderatormember",
                    "police",
                    "member",
                    "default",
                    -> return null
                }
                return group
            }
        }

        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val username = transaction(DatabaseManager.spicyAzisaBan) { SpicyAzisaBan.Players.getUsernameById(uuid) }
                ?: return call.respondJson(
                    mapOf("error" to "player not found"),
                    status = HttpStatusCode.NotFound
                )
            call.respondJson(toMap(uuid, username))
        }

        @Serializable
        @Resource("punishments")
        data class Punishments(val parent: Id): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                val map = transaction(DatabaseManager.spicyAzisaBan) {
                    SpicyAzisaBan.PunishmentHistory.find(SpicyAzisaBan.PunishmentHistoryTable.target eq parent.uuid.toString())
                        .map { it.toMap() }
                }
                call.respondJson(map)
            }
        }

        @Serializable
        @Resource("inventory")
        data class Inventory(val parent: Id): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                respondPlayerInventory(parent.uuid)
            }
        }

        @Serializable
        @Resource("enderchest")
        data class EnderChest(val parent: Id): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                respondPlayerEnderChest(parent.uuid)
            }
        }
    }

    @Serializable
    @Resource("by-name/{name}")
    data class ByName(val parent: RoutePlayers, val name: String): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val id = SpicyAzisaBan.Players.getIdByUsername(name)
                ?: return call.respondJson(
                    mapOf("error" to "player not found"),
                    status = HttpStatusCode.NotFound,
                )
            val realName = transaction(DatabaseManager.spicyAzisaBan) { SpicyAzisaBan.Players.getUsernameById(id) }
                ?: return call.respondJson(
                    mapOf("error" to "internal error"),
                    status = HttpStatusCode.NotFound
                )
            call.respondJson(Id.toMap(id, realName))
        }

        @Serializable
        @Resource("punishments")
        data class Punishments(val parent: ByName): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                val id = SpicyAzisaBan.Players.getIdByUsername(parent.name)
                    ?: return call.respondJson(
                        mapOf("error" to "player not found"),
                        status = HttpStatusCode.NotFound,
                    )
                val map = transaction(DatabaseManager.spicyAzisaBan) {
                    SpicyAzisaBan.PunishmentHistory.find(SpicyAzisaBan.PunishmentHistoryTable.target eq id.toString())
                        .map { it.toMap() }
                }
                call.respondJson(map)
            }
        }

        @Serializable
        @Resource("inventory")
        data class Inventory(val parent: ByName): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                val uuid = SpicyAzisaBan.Players.getIdByUsername(parent.name)
                    ?: return call.respondJson(
                        mapOf("error" to "player_not_found"),
                        status = HttpStatusCode.NotFound,
                    )
                respondPlayerInventory(uuid)
            }
        }

        @Serializable
        @Resource("enderchest")
        data class EnderChest(val parent: ByName): RequestHandler() {
            override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                val uuid = SpicyAzisaBan.Players.getIdByUsername(parent.name)
                    ?: return call.respondJson(
                        mapOf("error" to "player_not_found"),
                        status = HttpStatusCode.NotFound,
                    )
                respondPlayerEnderChest(uuid)
            }
        }
    }

    @Serializable
    @Resource("me")
    data class Me(val parent: RoutePlayers): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            this.context.authentication.principal<APIKeyPrincipal>()?.let { principal ->
                val username = transaction(DatabaseManager.spicyAzisaBan) { SpicyAzisaBan.Players.getUsernameById(principal.player) }
                    ?: return call.respondJson(
                        mapOf("error" to "player not found"),
                        status = HttpStatusCode.NotFound
                    )
                call.respondJson(toMap(principal.player, username))            }
        }
    }
}

private enum class SharedPlayerData {
    INVENTORY,
    ENDER_CHEST,
}

internal fun canAccessPlayerData(caller: UUID, target: UUID, shared: Boolean?): Boolean =
    caller == target || shared == true

private fun PipelineContext<Unit, ApplicationCall>.mayAccessPlayerData(
    target: UUID,
    data: SharedPlayerData,
): Boolean {
    val caller = context.authentication.principal<APIKeyPrincipal>()?.player ?: return false
    if (caller == target) return true

    val shared = transaction(DatabaseManager.azisabaApi) {
        AzisabaAPI.PlayerDataPrivacyTable
            .select { AzisabaAPI.PlayerDataPrivacyTable.playerUUID eq target.toString() }
            .firstOrNull()
            ?.let {
                when (data) {
                    SharedPlayerData.INVENTORY -> it[AzisabaAPI.PlayerDataPrivacyTable.shareInventory]
                    SharedPlayerData.ENDER_CHEST -> it[AzisabaAPI.PlayerDataPrivacyTable.shareEnderChest]
                }
            }
            ?: false
    }
    return canAccessPlayerData(caller, target, shared)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.respondPlayerInventory(uuid: UUID) {
    if (!mayAccessPlayerData(uuid, SharedPlayerData.INVENTORY)) {
        return call.respondJson(
            mapOf("error" to "data_sharing_disabled"),
            status = HttpStatusCode.Forbidden,
        )
    }

    val stored = transaction(DatabaseManager.lifeMpdb) {
        LifeMpdb.Inventory.find(LifeMpdb.InventoryTable.playerUUID eq uuid.toString()).firstOrNull()
    } ?: return call.respondJson(
        mapOf("error" to "inventory_data_not_found"),
        status = HttpStatusCode.NotFound,
    )

    val inventory = try {
        NbtJsonDecoder.decode(stored.inventory)
    } catch (e: Exception) {
        call.application.log.error("Could not decode MPDB inventory for player {}", uuid, e)
        return call.respondJson(
            mapOf("error" to "invalid_stored_player_data"),
            status = HttpStatusCode.InternalServerError,
        )
    }
    val armor = try {
        NbtJsonDecoder.decode(stored.armor)
    } catch (e: Exception) {
        call.application.log.error("Could not decode MPDB armor for player {}", uuid, e)
        return call.respondJson(
            mapOf("error" to "invalid_stored_player_data"),
            status = HttpStatusCode.InternalServerError,
        )
    }

    call.respondJson(
        mapOf(
            "uuid" to uuid.toString(),
            "name" to stored.playerName,
            "inventory" to inventory,
            "armor" to armor,
            "hotbar_slot" to stored.hotbarSlot,
            "gamemode" to stored.gamemode,
            "sync_complete" to stored.syncComplete,
            "last_seen" to stored.lastSeen,
        )
    )
}

private suspend fun PipelineContext<Unit, ApplicationCall>.respondPlayerEnderChest(uuid: UUID) {
    if (!mayAccessPlayerData(uuid, SharedPlayerData.ENDER_CHEST)) {
        return call.respondJson(
            mapOf("error" to "data_sharing_disabled"),
            status = HttpStatusCode.Forbidden,
        )
    }

    val stored = transaction(DatabaseManager.lifeMpdb) {
        LifeMpdb.EnderChest.find(LifeMpdb.EnderChestTable.playerUUID eq uuid.toString()).firstOrNull()
    } ?: return call.respondJson(
        mapOf("error" to "enderchest_data_not_found"),
        status = HttpStatusCode.NotFound,
    )

    val enderChest = try {
        NbtJsonDecoder.decode(stored.enderChest)
    } catch (e: Exception) {
        call.application.log.error("Could not decode MPDB ender chest for player {}", uuid, e)
        return call.respondJson(
            mapOf("error" to "invalid_stored_player_data"),
            status = HttpStatusCode.InternalServerError,
        )
    }

    call.respondJson(
        mapOf(
            "uuid" to uuid.toString(),
            "name" to stored.playerName,
            "enderchest" to enderChest,
            "sync_complete" to stored.syncComplete,
            "last_seen" to stored.lastSeen,
        )
    )
}
