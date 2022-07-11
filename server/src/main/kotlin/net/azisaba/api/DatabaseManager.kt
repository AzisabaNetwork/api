package net.azisaba.api

import net.azisaba.api.schemes.AzisabaAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {
    val azisabaApi: Database
    val spicyAzisaBan: Database
    val luckPerms: Database
    val lifeStatz: Database
    val lifeMpdb: Database

    init {
        val config = ServerConfig.instance.database
        azisabaApi = Database.connect(config.createDataSource(config.databaseNames.azisabaApi))
        spicyAzisaBan = Database.connect(config.createDataSource(config.databaseNames.spicyAzisaBan))
        luckPerms = Database.connect(config.createDataSource(config.databaseNames.luckPerms))
        lifeStatz = Database.connect(config.createDataSource(config.databaseNames.lifeStatz))
        lifeMpdb = Database.connect(config.createDataSource(config.databaseNames.lifeMpdb))

        transaction(azisabaApi) {
            addLogger(Slf4jSqlDebugLogger)
            SchemaUtils.create(AzisabaAPI.APIKeyTable)
        }
        transaction(spicyAzisaBan) { addLogger(Slf4jSqlDebugLogger) }
        transaction(luckPerms) { addLogger(Slf4jSqlDebugLogger) }
        transaction(lifeStatz) { addLogger(Slf4jSqlDebugLogger) }
        transaction(lifeMpdb) { addLogger(Slf4jSqlDebugLogger) }
    }
}
