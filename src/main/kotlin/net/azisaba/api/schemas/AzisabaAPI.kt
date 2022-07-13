package net.azisaba.api.schemas

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object AzisabaAPI {
    class APIKey(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<APIKey>(APIKeyTable)

        var key by APIKeyTable.key
        var player by APIKeyTable.player
        var createdAt by APIKeyTable.createdAt
        var uses by APIKeyTable.uses
    }

    object APIKeyTable : LongIdTable("api_keys") {
        val key = varchar("key", 64)
        val player = varchar("player", 36) // uuid
        val createdAt = long("created_at")
        val uses = long("uses").default(0L)

        init {
            uniqueIndex("player_key", key, player)
        }
    }
}
