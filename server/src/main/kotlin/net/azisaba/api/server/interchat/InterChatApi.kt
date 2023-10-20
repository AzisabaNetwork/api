package net.azisaba.api.server.interchat

import net.azisaba.interchat.api.InterChat
import net.azisaba.interchat.api.Logger
import net.azisaba.interchat.api.guild.GuildManager
import net.azisaba.interchat.api.guild.SQLGuildManager
import net.azisaba.interchat.api.user.SQLUserManager
import net.azisaba.interchat.api.user.UserManager
import net.azisaba.interchat.api.util.QueryExecutor
import net.azisaba.interchat.api.util.SQLThrowableConsumer
import java.sql.PreparedStatement
import java.sql.Statement
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

    override fun getLogger(): Logger = Logger.getCurrentLogger()

    override fun getGuildManager(): GuildManager = guildManager

    override fun getUserManager(): UserManager = userManager

    override fun getAsyncExecutor(): Executor = asyncExecutor
}
