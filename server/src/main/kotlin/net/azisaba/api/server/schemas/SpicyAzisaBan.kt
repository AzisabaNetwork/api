package net.azisaba.api.server.schemas

import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

object SpicyAzisaBan {
    object PlayersTable : IdTable<String>("players") {
        override val id: Column<EntityID<String>> = varchar("uuid", 255).entityId()
        val name = varchar("name", 255)
        val ip = varchar("ip", 255).nullable()
        val lastSeen = long("last_seen")
        val firstLogin = long("first_login")
        val firstLoginAttempt = long("first_login_attempt")
        val lastLogin = long("last_login")
        val lastLoginAttempt = long("last_login_attempt")
    }

    class Players(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, Players>(PlayersTable) {
            val getUsernameById: (id: UUID) -> String? = Util.memoize(1000 * 60 * 10) { id ->
                Players.findById(id.toString())?.name
            }
        }

        var name by PlayersTable.name
        var ip by PlayersTable.ip
        var lastSeen by PlayersTable.lastSeen
        var firstLogin by PlayersTable.firstLogin
        var firstLoginAttempt by PlayersTable.firstLoginAttempt
        var lastLogin by PlayersTable.lastLogin
        var lastLoginAttempt by PlayersTable.lastLoginAttempt
    }
}
