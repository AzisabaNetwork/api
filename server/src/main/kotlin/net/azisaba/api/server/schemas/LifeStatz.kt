package net.azisaba.api.server.schemas

import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

object LifeStatz {
    object StatzTimePlayedTable : LongIdTable("statz_time_played") {
        val uuid = varchar("uuid", 100)
        val value = long("value")
        val world = varchar("world", 100)

        init {
            uniqueIndex("uuid", uuid, world)
        }
    }

    class StatzTimePlayed(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<StatzTimePlayed>(StatzTimePlayedTable) {
            val getPlayTime: (uuid: UUID) -> Long = Util.memoize(1000 * 60) {
                StatzTimePlayedTable
                    .select(StatzTimePlayedTable.uuid eq it.toString())
                    .sumOf { rw -> rw[StatzTimePlayedTable.value] }
            }
        }

        var uuid by StatzTimePlayedTable.uuid
        var value by StatzTimePlayedTable.value
        var world by StatzTimePlayedTable.world
    }
}
