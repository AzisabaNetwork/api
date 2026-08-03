package net.azisaba.api.spigot

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.Logger
import org.mariadb.jdbc.Driver
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

@Serializable
data class PluginConfig(
    val paths: FilePaths = FilePaths(),
    val mythicMobs: MythicMobs = MythicMobs(),
    val redis: RedisConfig = RedisConfig(),
    val purchaseCommands: List<PurchaseCommand> = listOf(PurchaseCommand(saraProductPrice = Int.MAX_VALUE)),
    val database: SpigotDatabaseConfig = SpigotDatabaseConfig(),
) {
    companion object {
        lateinit var instance: PluginConfig

        fun loadConfig(dataDirectory: Path) {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory)
            }
            val configPath = dataDirectory.resolve("config.yml")
            Logger.currentLogger.info("Loading config from $configPath")
            val comment = """
                # This is the config file for the plugin.
                # Note: Configuration file is regenerated every time the plugin is loaded.
                #       Values will be kept but all non-default comments will be removed.
            """.trimIndent() + "\n"
            if (!Files.exists(configPath)) {
                Logger.currentLogger.warn("Config file not found. Creating new one.")
                configPath.writeText(comment + Yaml.default.encodeToString(serializer(), PluginConfig()) + "\n")
            }
            instance = readCurrentConfig(dataDirectory)
            configPath.writeText(comment + Yaml.default.encodeToString(serializer(), instance) + "\n")

            // initialize driver
            Driver()
        }

        fun readCurrentConfig(dataDirectory: Path): PluginConfig {
            val configPath = dataDirectory.resolve("config.yml")
            return configPath.inputStream().use { input ->
                Yaml.default.decodeFromStream(serializer(), input)
            }
        }
    }
}

@Serializable
data class FilePaths(
    @YamlComment("/path/to/plugins/CrazyAuctions/Data.yml")
    val lifeAuctions: String? = null,
)

@Serializable
data class MythicMobs(
    @YamlComment("If true, these settings will be uploaded to the Redis server.")
    val enabled: Boolean = false,
    val lifeServers: Map<String, List<String>> = mapOf(
        "anyServerName" to listOf("spawnerName"),
    ),
)

@SerialName("database")
@Serializable
data class SpigotDatabaseConfig(
    @YamlComment("Change to jdbc:mysql if you want to use MySQL instead of MariaDB")
    val scheme: String = "jdbc:mariadb",
    val hostname: String = "localhost",
    val port: Int = 3306,
    @YamlComment("Must match database.databaseNames.azisabaApi in api-ktor-server")
    val name: String = "azisaba_api",
    @YamlComment(
        "The user needs SELECT, INSERT, and UPDATE on player_data_privacy.",
        "Using root is not recommended because it opens up a large security hole.",
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
) {
    fun connectionProperties() = properties.toProperties().apply {
        setProperty("user", username)
        setProperty("password", password)
    }

    val jdbcUrl: String get() = "$scheme://$hostname:$port/$name"
}

@SerialName("redis")
@Serializable
data class RedisConfig(
    @YamlComment("Redis hostname.")
    val host: String = "localhost",
    @YamlComment("Port of Redis server. Default is 6379, change if you want to use a different port.")
    val port: Int = 6379,
    @YamlComment("Username for Redis.")
    val username: String? = null,
    @YamlComment("Password for Redis, if the server is password-protected.")
    val password: String? = null,
) {
    fun createPool(): JedisPool {
        return if (username != null && password != null) {
            JedisPool(host, port, username, password)
        } else if (password != null) {
            JedisPool(JedisPoolConfig(), host, port, 3000, password)
        } else if (username != null) {
            throw IllegalArgumentException("Password cannot be null if the username is provided")
        } else {
            JedisPool(JedisPoolConfig(), host, port)
        }
    }
}

@Serializable
data class PurchaseCommand(
    val command: List<String> = listOf("tell <player> Thank you for your purchase!"),
    val productId: Long? = Long.MAX_VALUE,
    val saraProductPrice: Int? = null,
)
