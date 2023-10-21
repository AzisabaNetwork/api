package net.azisaba.api.server.interchat

import kotlinx.coroutines.runBlocking
import net.azisaba.api.server.interchat.protocol.OutgoingMessagePacket
import net.azisaba.api.server.util.Util
import net.azisaba.interchat.api.guild.GuildInviteResult
import net.azisaba.interchat.api.guild.GuildMember
import net.azisaba.interchat.api.network.PacketListener
import net.azisaba.interchat.api.network.protocol.GuildInvitePacket
import net.azisaba.interchat.api.network.protocol.GuildInviteResultPacket
import net.azisaba.interchat.api.network.protocol.GuildJoinPacket
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket
import net.azisaba.interchat.api.text.MessageFormatter
import net.azisaba.interchat.api.util.AsyncUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
object InterChatPacketListener : PacketListener {
    val sockets: MutableSet<ConnectedSocket> = Collections.synchronizedSet(LinkedHashSet())
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
                        if (!it.sendPacket(OutgoingMessagePacket(coloredText))) {
                            toRemove += it
                        }
                    }
                }
                sockets -= toRemove.toSet()
            }
        }
    }

    override fun handleGuildInvite(packet: GuildInvitePacket) {
        val guildFuture = InterChatApi.guildManager.fetchGuildById(packet.guildId())
        val fromFuture = InterChatApi.userManager.fetchUser(packet.from())
        val toFuture = InterChatApi.userManager.fetchUser(packet.to())
        AsyncUtil.collectAsync(guildFuture, fromFuture, toFuture) { guild, from, to ->
            if (guild == null || from == null || to == null) {
                return@collectAsync
            }
            val members = guild.members.join().map { it.uuid() }
            val message = "§b${from.name()}§6が§b${to.name()}§6をギルド§b${guild.name()}§6に招待しました。招待は5分で期限切れになります。"
            runBlocking {
                sockets.forEach { socket ->
                    if (socket.uuid == to.id()) {
                        socket.sendMessage(Component.text("------------------------------", NamedTextColor.YELLOW))
                        socket.sendMessage("§b${from.name()}§6があなたをギルド§b${guild.name()}§6に招待しました。招待は5分で期限切れになります。")
                        socket.sendMessage(Component.join(JoinConfiguration.noSeparators(), listOf(
                            Component.text("[", NamedTextColor.GREEN)
                                .append(Component.text("承認(Accept)"))
                                .append(Component.text("]"))
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.suggestCommand("/cguild accept ${guild.name()}")),
                            Component.space(),
                            Component.text("[", NamedTextColor.RED)
                                .append(Component.text("拒否(Reject)"))
                                .append(Component.text("]"))
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.suggestCommand("/cguild reject ${guild.name()}"))
                        )))
                        socket.sendMessage(Component.text("------------------------------", NamedTextColor.YELLOW))
                    }
                    if (socket.uuid in members) {
                        socket.sendMessage(message)
                    }
                }
            }
        }
    }

    override fun handleGuildInviteResult(packet: GuildInviteResultPacket) {
        val guildFuture = InterChatApi.guildManager.fetchGuildById(packet.guildId())
        val userFuture = InterChatApi.userManager.fetchUser(packet.to())
        AsyncUtil.collectAsync(guildFuture, userFuture) { guild, user ->
            if (guild == null || user == null) {
                return@collectAsync
            }
            val members = guild.members.join().map { it.uuid() }
            val message = if (packet.result() == GuildInviteResult.ACCEPTED) {
                "§b${user.name()}§6がギルド§b${guild.name()}§6に参加しました。"
            } else {
                "§b${user.name()}§6がギルド§b${guild.name()}§6の招待を拒否しました。"
            }
            runBlocking {
                sockets.forEach { socket ->
                    if (socket.uuid in members) {
                        socket.sendMessage(message)
                    }
                }
            }
        }
    }

    override fun handleGuildJoin(packet: GuildJoinPacket) {
        val guildFuture = InterChatApi.guildManager.fetchGuildById(packet.guildId())
        val userFuture = InterChatApi.userManager.fetchUser(packet.player())
        AsyncUtil.collectAsync(guildFuture, userFuture) { guild, user ->
            if (guild == null || user == null) {
                return@collectAsync
            }
            val members = guild.members.join().map { it.uuid() }
            val message = "§b${user.name()}§6がギルド§b${guild.name()}§6に参加しました。"
            runBlocking {
                sockets.forEach { socket ->
                    if (socket.uuid in members) {
                        socket.sendMessage(message)
                    }
                }
            }
        }
    }
}
