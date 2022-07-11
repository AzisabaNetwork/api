package net.azisaba.api.schemes

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object AzisabaAPI {
    class APIKey(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<APIKey>(APIKeyTable)

        val key by APIKeyTable.key
        val player by APIKeyTable.player
    }

    object APIKeyTable : LongIdTable("api_keys") {
        val key = varchar("key", 64)
        val player = varchar("player", 36) // uuid

        init {
            uniqueIndex("player_key", key, player)
        }
    }
}
