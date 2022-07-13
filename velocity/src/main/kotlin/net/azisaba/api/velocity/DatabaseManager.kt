package net.azisaba.api.velocity

import net.azisaba.api.schemas.AzisabaAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {
    val azisabaApi: Database = Database.connect(PluginConfig.instance.database.createDataSource())

    init {
        transaction(azisabaApi) {
            addLogger(Slf4jSqlDebugLogger)

            SchemaUtils.create(AzisabaAPI.APIKeyTable)
        }
    }
}
