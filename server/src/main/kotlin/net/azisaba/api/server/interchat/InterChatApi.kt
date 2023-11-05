@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.api.server.interchat

import net.azisaba.api.server.interchat.protocol.OutgoingErrorMessagePacket
import net.azisaba.interchat.api.InterChat
import net.azisaba.interchat.api.Logger
import net.azisaba.interchat.api.data.UserDataProvider
import net.azisaba.interchat.api.guild.GuildBan
import net.azisaba.interchat.api.guild.GuildManager
import net.azisaba.interchat.api.guild.SQLGuildManager
import net.azisaba.interchat.api.user.SQLUserManager
import net.azisaba.interchat.api.user.User
import net.azisaba.interchat.api.user.UserManager
import net.azisaba.interchat.api.util.QueryExecutor
import net.azisaba.interchat.api.util.SQLThrowableConsumer
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object InterChatApi : InterChat {
    val dataSource = InterChatConfig.instance.database.createDataSource()
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool()
    val queryExecutor = object : QueryExecutor {
        override fun query(sql: String, action: SQLThrowableConsumer<PreparedStatement>) {
            dataSource.connection.use { conn -> conn.prepareStatement(sql).use { action.accept(it) } }
        }

        override fun queryWithGeneratedKeys(sql: String, action: SQLThrowableConsumer<PreparedStatement>) {
            dataSource.connection.use { conn -> conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { action.accept(it) } }
        }
    }
    private val guildManager = SQLGuildManager(queryExecutor)
    private val userManager = SQLUserManager(queryExecutor)

    inline fun <R> querySql(@Language("SQL") sql: String, action: (PreparedStatement) -> R) =
        dataSource.connection.use { conn -> conn.prepareStatement(sql).use { action(it) } }

    override fun getLogger(): Logger = Logger.getCurrentLogger()

    override fun getGuildManager(): GuildManager = guildManager

    override fun getUserManager(): UserManager = userManager

    override fun getAsyncExecutor(): Executor = asyncExecutor

    override fun getUserDataProvider(): UserDataProvider = UserDataProviderImpl

    suspend fun getUserByName(connection: ConnectedSocket, name: String): User? {
        val users = userManager.fetchUserByUsername(name).join()
        if (users.isEmpty()) {
            connection.sendPacket(OutgoingErrorMessagePacket("No such player $name"))
            return null
        }
        if (users.size > 1) {
            connection.sendPacket(OutgoingErrorMessagePacket("Multiple users found for $name"))
            return null
        }
        return users[0]
    }

    suspend fun checkBan(connection: ConnectedSocket, guildId: Long, player: UUID): GuildBan? =
        guildManager.getBan(guildId, player).join().orElse(null)?.also { ban ->
            if (ban.reasonPublic()) {
                // if public / moderator or higher
                connection.sendPacket(OutgoingErrorMessagePacket("$player is banned from this guild for ${ban.reason()}"))
            } else {
                connection.sendPacket(OutgoingErrorMessagePacket("$player is banned from this guild"))
            }
        }

    fun submitLog(guildId: Long, actor: String?, actorName: String, description: String) {
        asyncExecutor.execute {
            try {
                queryExecutor.query("INSERT INTO `guild_logs` (`guild_id`, `actor`, `actor_name`, `time`, `description`) VALUES (?, ?, ?, ?, ?)") { statement ->
                    statement.setLong(1, guildId)
                    statement.setString(2, actor)
                    statement.setString(3, actorName)
                    statement.setLong(4, System.currentTimeMillis())
                    statement.setString(5, description)
                    statement.executeUpdate()
                }
            } catch (e: SQLException) {
                Logger.getCurrentLogger().error("Failed to submit log", e)
            }
        }
    }

    fun submitLog(guildId: Long, user: User, description: String) =
        submitLog(guildId, user.name(), user.id().toString(), description)
}
