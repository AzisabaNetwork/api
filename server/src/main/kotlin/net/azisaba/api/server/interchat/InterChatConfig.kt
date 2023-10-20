package net.azisaba.api.server.interchat

import com.charleskorn.kaml.Yaml
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import net.azisaba.interchat.api.Logger
import net.azisaba.interchat.api.network.JedisBox
import net.azisaba.interchat.api.network.Side
import org.mariadb.jdbc.Driver
import java.io.File

@Serializable
data class InterChatConfig(
    val redis: RedisConfig = RedisConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
) {
    companion object {
        val instance: InterChatConfig

        init {
            val configFile = File(System.getenv("net.azisaba.api.interchatConfigurationFile") ?: "interchat.yml")
            println("Loading config from $configFile (absolute path: ${configFile.absolutePath}) (use -Dnet.azisaba.api.interchatConfigurationFile=path to override)")
            if (!configFile.exists()) {
                println("Config file not found. Creating new one.")
                configFile.writeText(Yaml.default.encodeToString(serializer(), InterChatConfig()) + "\n")
            }
            instance = Yaml.default.decodeFromStream(serializer(), configFile.inputStream())
            if (java.lang.Boolean.getBoolean("net.azisaba.api.saveConfig")) {
                println("Saving config to $configFile (absolute path: ${configFile.absolutePath})")
                configFile.writeText(Yaml.default.encodeToString(serializer(), instance) + "\n")
            }

            Driver() // register driver here, just in case it's not registered yet.
        }
    }

    @Serializable
    data class RedisConfig(
        val hostname: String = "localhost",
        val port: Int = 6379,
        val username: String? = null,
        val password: String? = null,
    ) {
        fun createJedisBox() =
            JedisBox(
                Side.PROXY,
                Logger.getCurrentLogger(),
                InterChatPacketListener,
                hostname,
                port,
                username,
                password,
            )
    }

    @Serializable
    data class DatabaseConfig(
        val driver: String = "org.mariadb.jdbc.Driver",
        val scheme: String = "jdbc:mariadb",
        val hostname: String = "localhost",
        val port: Int = 3306,
        val username: String = "interchat",
        val password: String = "interchat",
        val name: String = "interchat",
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
        fun createDataSource(): HikariDataSource {
            val config = HikariConfig()
            val actualDriver = try {
                Class.forName(driver)
                driver
            } catch (e: ClassNotFoundException) {
                Logger.getCurrentLogger().warn("Failed to load driver class $driver. Falling back to org.mariadb.jdbc.Driver")
                "org.mariadb.jdbc.Driver"
            }
            config.driverClassName = actualDriver
            config.jdbcUrl = "$scheme://$hostname:$port/$name"
            config.username = username
            config.password = password
            config.dataSourceProperties = properties.toProperties()
            return HikariDataSource(config)
        }
    }
}
