package net.azisaba.api.server.schemas

import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object LuckPerms {
    object PlayersTable : IdTable<String>(ServerConfig.instance.database.databaseNames.luckPermsTablePrefix + "players") {
        override val id = varchar("uuid", 36).entityId()
        val username = varchar("username", 16).uniqueIndex()
        val primaryGroup = varchar("primary_group", 36)
    }

    class Players(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, Players>(PlayersTable) {
            val getUsernameById: (id: UUID) -> String? = Util.memoize(1000 * 60 * 10) { id -> // 10 minutes
                transaction(DatabaseManager.luckPerms) {
                    Players.findById(id.toString())?.username
                }
            }
        }

        val username by PlayersTable.username
        val primaryGroup by PlayersTable.primaryGroup
    }

    object UserPermissionsTable : IntIdTable(ServerConfig.instance.database.databaseNames.luckPermsTablePrefix + "user_permissions") {
        val uuid = varchar("uuid", 36)
        val permission = varchar("permission", 200)
        val value = bool("value")
        val server = varchar("server", 36)
        val world = varchar("world", 36)
        val expiry = integer("expiry")
        val contexts = varchar("contexts", 200)
    }

    class UserPermissions(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<UserPermissions>(UserPermissionsTable) {
            val getGroupsForPlayer: (uuid: UUID) -> List<UserPermissions> = Util.memoize(1000 * 30) { // 30 seconds
                transaction(DatabaseManager.luckPerms) {
                    UserPermissions
                        .find {
                            (UserPermissionsTable.uuid eq it.toString()) and
                                    (UserPermissionsTable.permission like "group.%") and
                                    (UserPermissionsTable.value eq true)
                        }
                        .sortedByDescending { GroupPermissions.getGroupWeight(it.permission.removePrefix("group.")) }
                        .toList()
                }
            }

            fun hasAnyGroup(uuid: UUID, vararg groups: String, server: String = "global"): Boolean =
                getGroupsForPlayer(uuid).any {
                    it.server == server &&
                            it.world == "global" &&
                            groups.contains(it.permission.removePrefix("group."))
                }

            val getPermissions = Util.memoize<UUID, List<UserPermissions>>(1000 * 60) {
                transaction(DatabaseManager.luckPerms) {
                    UserPermissions.find { UserPermissionsTable.uuid eq it.toString() }.toList()
                }
            }
        }

        val uuid by UserPermissionsTable.uuid
        val permission by UserPermissionsTable.permission
        val value by UserPermissionsTable.value
        val server by UserPermissionsTable.server
        val world by UserPermissionsTable.world
        val expiry by UserPermissionsTable.expiry
        val contexts by UserPermissionsTable.contexts
    }

    object GroupPermissionsTable : IntIdTable(ServerConfig.instance.database.databaseNames.luckPermsTablePrefix + "group_permissions") {
        val name = varchar("name", 36)
        val permission = varchar("permission", 200)
        val value = bool("value")
        val server = varchar("server", 36)
        val world = varchar("world", 36)
        val expiry = integer("expiry")
        val contexts = varchar("contexts", 200)
    }

    class GroupPermissions(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<GroupPermissions>(GroupPermissionsTable) {
            val getGroupWeight: (name: String) -> Long = Util.memoize(1000 * 60 * 60 * 2) { // 2 hours
                transaction(DatabaseManager.luckPerms) {
                    GroupPermissions
                        .find {
                            (GroupPermissionsTable.name eq it) and
                                    (GroupPermissionsTable.permission like "weight.%") and
                                    (GroupPermissionsTable.value eq true)
                        }
                        .limit(1)
                        .firstOrNull()
                        ?.permission
                        ?.removePrefix("weight.")
                        ?.toLong()
                        ?: 0
                }
            }

            val getPermissions = Util.memoize<String, List<GroupPermissions>>(1000 * 60) {
                transaction(DatabaseManager.luckPerms) {
                    GroupPermissions.find { GroupPermissionsTable.name eq it }.toList()
                }
            }
        }

        val name by GroupPermissionsTable.name
        val permission by GroupPermissionsTable.permission
        val value by GroupPermissionsTable.value
        val server by GroupPermissionsTable.server
        val world by GroupPermissionsTable.world
        val expiry by GroupPermissionsTable.expiry
        val contexts by GroupPermissionsTable.contexts
    }
}
