package net.azisaba.api.server.schemas

import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
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
                transaction(DatabaseManager.spicyAzisaBan) {
                    Players.findById(id.toString())?.name
                }
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

    // all punishments, including the "unpunished" one
    object PunishmentHistoryTable : LongIdTable("punishmentHistory") {
        val name = varchar("name", 255)
        val target = varchar("target", 255)
        val reason = varchar("reason", 255)
        val operator = varchar("operator", 255)
        val type = varchar("type", 255)
        val start = long("start")
        val end = long("end")
        val server = varchar("server", 255)
        val extra = varchar("extra", 255)
    }

    class PunishmentHistory(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<PunishmentHistory>(PunishmentHistoryTable)

        var name by PunishmentHistoryTable.name
        var target by PunishmentHistoryTable.target
        var reason by PunishmentHistoryTable.reason
        var operator by PunishmentHistoryTable.operator
        var type by PunishmentHistoryTable.type
        var start by PunishmentHistoryTable.start
        var end by PunishmentHistoryTable.end
        var server by PunishmentHistoryTable.server
        var extra by PunishmentHistoryTable.extra

        fun toMap(): Map<String, Any?> = transaction(DatabaseManager.spicyAzisaBan) {
            val unpunish = Unpunish.find(UnpunishTable.punishId eq this@PunishmentHistory.id.value).firstOrNull()
            val inTable = Punishments.findById(this@PunishmentHistory.id)
            val expired = end > 0L && end < System.currentTimeMillis()
            val active = unpunish == null && inTable != null && !expired
            val actorName = Players.getUsernameById(UUID.fromString(operator)) ?: "CONSOLE"
            val proofs = Proofs.find(ProofsTable.punishId eq this@PunishmentHistory.id.value).map { it.toMap() }
            return@transaction mapOf(
                "id" to this@PunishmentHistory.id.value,
                "type" to type,
                "target" to mapOf(
                    "uuid" to target,
                    "name" to name,
                ),
                "actor" to mapOf(
                    "uuid" to operator,
                    "name" to actorName,
                ),
                "reason" to reason,
                "proofs" to proofs,
                "start" to start,
                "end" to end,
                "server" to server,
                "active" to active,
                "extra" to extra,
                "unpunish" to unpunish?.toMap(),
                "unpunished" to (inTable == null),
            )
        }
    }

    // active punishments
    object PunishmentsTable : LongIdTable("punishments") {
        val name = varchar("name", 255)
        val target = varchar("target", 255)
        val reason = varchar("reason", 255)
        val operator = varchar("operator", 255)
        val type = varchar("type", 255)
        val start = long("start")
        val end = long("end")
        val server = varchar("server", 255)
        val extra = varchar("extra", 255)
    }

    class Punishments(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Punishments>(PunishmentsTable)

        var name by PunishmentsTable.name
        var target by PunishmentsTable.target
        var reason by PunishmentsTable.reason
        var operator by PunishmentsTable.operator
        var type by PunishmentsTable.type
        var start by PunishmentsTable.start
        var end by PunishmentsTable.end
        var server by PunishmentsTable.server
        var extra by PunishmentsTable.extra
    }

    object UnpunishTable : LongIdTable("unpunish") {
        val punishId = long("punish_id")
        val reason = varchar("reason", 255)
        val timestamp = long("timestamp")
        val operator = varchar("operator", 255)
    }

    class Unpunish(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Unpunish>(UnpunishTable)

        var punishId by UnpunishTable.punishId
        var reason by UnpunishTable.reason
        var timestamp by UnpunishTable.timestamp
        var operator by UnpunishTable.operator

        fun toMap(): Map<String, Any?> {
            val unpunishActorName = Players.getUsernameById(UUID.fromString(operator)) ?: "CONSOLE"
            return mapOf(
                "id" to id.value,
                "actor" to mapOf(
                    "uuid" to operator,
                    "name" to unpunishActorName,
                ),
                "reason" to reason,
                "timestamp" to timestamp,
            )
        }
    }

    object ProofsTable : LongIdTable("proofs") {
        val punishId = long("punish_id")
        val text = varchar("text", 255)
        val public = bool("public").default(false)
    }

    class Proofs(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Proofs>(ProofsTable)

        var punishId by ProofsTable.punishId
        var text by ProofsTable.text
        var public by ProofsTable.public

        fun toMap(): Map<String, Any?> =
            mapOf(
                "id" to id.value,
                "text" to text,
                "public" to public,
            )
    }
}
