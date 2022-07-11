package net.azisaba.api.resources

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.DatabaseManager
import net.azisaba.api.schemes.LifeMpdb
import net.azisaba.api.schemes.LifeStatz
import net.azisaba.api.schemes.LuckPerms
import net.azisaba.api.schemes.SpicyAzisaBan
import net.azisaba.api.serializers.UUIDSerializer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
@Resource("/players")
class Players {
    @Serializable
    @Resource("{uuid}")
    data class Id(
        @Suppress("unused")
        val parent: Players,
        @Serializable(with = UUIDSerializer::class)
        val uuid: UUID,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            if (uuid.version() == 3) {
                call.respondJson(
                    mapOf(
                        "error" to "cannot get player from offline (version 3) UUID",
                    ),
                    status = HttpStatusCode.BadRequest,
                )
                return
            }
            val username = transaction(DatabaseManager.spicyAzisaBan) { SpicyAzisaBan.Players.getUsernameById(uuid) }
            if (username == null) {
                call.respondJson(
                    mapOf(
                        "error" to "player not found",
                    ),
                    status = HttpStatusCode.NotFound,
                )
                return
            }
            call.respondJson(
                mapOf(
                    "uuid" to (uuid.toString() as Any).toString(),
                    "name" to username,
                    "groups" to getGroups(uuid, "global"),
                    "servers" to mapOf(
                        "life" to getLife(uuid),
                        "tsl" to getTSL(uuid),
                        "lgw" to getLGW(uuid),
                        "despawn" to getDespawn(uuid),
                        "diverse" to getDiverse(uuid),
                    )
                )
            )
        }

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
                .firstOrNull()
                ?.toInt()
                ?: 1
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

        private fun getDespawn(uuid: UUID): Map<String, Any> {
            return getServerTemplate(uuid, "despawn")
        }

        private fun getDiverse(uuid: UUID): Map<String, Any> {
            return getServerTemplate(uuid, "diverse")
            // exp, money, veteranpoint, title might be added in the future
        }

        private fun getGroups(uuid: UUID, server: String) = transaction(DatabaseManager.luckPerms) {
            LuckPerms.UserPermissions.getGroupsForPlayer(uuid)
                .filter { it.server == server && it.world == "global" }
                .map { it.permission.removePrefix("group.") }
                .mapNotNull { filterGroup(it) }
        }

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
}
