package net.azisaba.api.server.interchat

import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON
import net.azisaba.interchat.api.guild.GuildMember
import net.azisaba.interchat.api.network.PacketListener
import net.azisaba.interchat.api.network.protocol.*
import net.azisaba.interchat.api.text.MessageFormatter
import net.azisaba.interchat.api.util.AsyncUtil
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.LinkedHashSet

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
object InterChatPacketListener : PacketListener {
    val sockets: MutableSet<ConnectedSocket> = Collections.synchronizedSet(LinkedHashSet<ConnectedSocket>())
    val getHideAllUntil = Util.memoize<UUID, Long>(10000) { uuid ->
        val value = AtomicLong()
        try {
            InterChatApi.queryExecutor.query("SELECT `hide_all_until` FROM `players` WHERE `id` = ?") { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        value.set(rs.getLong("hide_all_until"))
                    }
                }
            }
        } catch (_: Exception) {}
        value.get()
    }

    override fun handleGuildMessage(packet: GuildMessagePacket) {
        val guildFuture = InterChatApi.guildManager.fetchGuildById(packet.guildId())
        val userFuture = InterChatApi.userManager.fetchUser(packet.sender())
        AsyncUtil.collectAsync(guildFuture, userFuture) { guild, user ->
            if (guild == null || user == null || guild.deleted()) {
                return@collectAsync
            }
            val members = guild.members.join()
            val nickname = members.stream().filter { it.uuid() == user.id() }.findAny().map(GuildMember::nickname)
            val formattedText = MessageFormatter.format(
                guild.format(),
                guild,
                packet.server(),
                user,
                nickname.orElse(null),
                packet.message(),
                packet.transliteratedMessage(),
                emptyMap(),
            )
            val coloredText =
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(formattedText)
                    .let { LegacyComponentSerializer.legacySection().serialize(it) }
            runBlocking {
                val toRemove = mutableListOf<ConnectedSocket>()
                sockets.forEach {
                    if (members.any { m -> !m.hiddenByMember() && m.uuid() == it.uuid }) {
                        if (getHideAllUntil(it.uuid!!) > System.currentTimeMillis()) {
                            return@forEach // continue loop
                        }
                        try {
                            it.connection.send(JSON.encodeToString(mapOf("message" to coloredText)))
                        } catch (e: Exception) {
                            toRemove += it
                        }
                    }
                }
                sockets -= toRemove.toSet()
            }
        }
    }
}
