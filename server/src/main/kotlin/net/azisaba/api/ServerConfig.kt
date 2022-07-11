package net.azisaba.api

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlComment
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mariadb.jdbc.Driver
import java.io.File

@Serializable
data class ServerConfig(val database: DatabaseConfig = DatabaseConfig()) {
    companion object {
        val instance: ServerConfig

        init {
            val configFile = File(System.getenv("net.azisaba.api.configurationFile") ?: "config.yml")
            println("Loading config from $configFile (absolute path: ${configFile.absolutePath}) (use -Dnet.azisaba.api.configurationFile=path to override)")
            if (!configFile.exists()) {
                println("Config file not found. Creating new one.")
                configFile.writeText(Yaml.default.encodeToString(serializer(), ServerConfig()) + "\n")
            }
            instance = Yaml.default.decodeFromStream(serializer(), configFile.inputStream())

            Driver() // register driver here, just in case it's not registered yet.
        }
    }
}

@SerialName("database")
@Serializable
data class DatabaseConfig(
    @YamlComment(
        "You may have noticed that there is multiple 'name' properties (except for username).",
        "This is because multiple databases are used to get various data.",
        "Note: Jdbc URL is created with: (scheme)://(host):(port)/(database)",
        "",
        "Driver class to use.",
    )
    val driver: String = "org.mariadb.jdbc.Driver",
    @YamlComment("Change to jdbc:mysql if you want to use MySQL instead of MariaDB")
    val scheme: String = "jdbc:mariadb",
    val hostname: String = "localhost",
    val port: Int = 3306,
    @YamlComment(
        "Make sure the user has the correct permissions to access all of the databases provided below!",
        "Using root is not recommended because it opens up a large security hole.",
        "Note that the user does NOT require the any mutation privileges on the databases unless explicitly noted.",
    )
    val username: String = "azisaba_api",
    val password: String = "",
    val properties: Map<String, String> = mapOf(
        "useSSL" to "false",
        "verifyServerCertificate" to "true",
        "prepStmtCacheSize" to "250",
        "prepStmtCacheSqlLimit" to "2048",
        "cachePrepStmts" to "true",
        "useServerPrepStmts" to "true",
        "socketTimeout" to "60000",
        "useLocalSessionState" to "true",
        "rewriteBatchedStatements" to "true",
        "maintainTimeStats" to "false",
    ),
    val databaseNames: DatabaseNames = DatabaseNames(),
) {
    fun createDataSource(name: String): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driver
        config.jdbcUrl = "$scheme://$hostname:$port/$name"
        config.username = username
        config.password = password
        config.dataSourceProperties = properties.toProperties()
        return HikariDataSource(config)
    }
}

@Serializable
data class DatabaseNames(
    @YamlComment("Stores API Keys. Requires the SELECT, CREATE, and ALTER privileges.")
    val azisabaApi: String = "azisaba_api",
    @YamlComment("Database of SpicyAzisaBan. Requires the SELECT privilege on players table.")
    val spicyAzisaBan: String = "spicyazisaban",
    @YamlComment(
        "Database of LuckPerms.",
        "Requires the SELECT privilege on:",
        " - (prefix)_group_permissions",
        " - (prefix)_user_permissions",
        " - (prefix)_players",
    )
    val luckPerms: String = "luckperms",
    @YamlComment("Table prefix configured in LuckPerms.")
    val luckPermsTablePrefix: String = "luckperms_",
    val lifeStatz: String = "life_statz",
    @YamlComment("Database of MPDB. Requires the SELECT privilege on mpdb_economy table.")
    val lifeMpdb: String = "life_mpdb",
)
