package net.azisaba.api.server.interchat

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.interchat.protocol.OutgoingComponentPacket
import net.azisaba.api.server.interchat.protocol.OutgoingWebGuildInvitePacket
import net.azisaba.api.server.interchat.protocol.OutgoingWebGuildMessagePacket
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON
import net.azisaba.interchat.api.data.PlayerPosData
import net.azisaba.interchat.api.data.SenderInfo
import net.azisaba.interchat.api.guild.GuildInviteResult
import net.azisaba.interchat.api.guild.GuildMember
import net.azisaba.interchat.api.network.PacketListener
import net.azisaba.interchat.api.network.RedisKeys
import net.azisaba.interchat.api.network.protocol.*
import net.azisaba.interchat.api.text.MessageFormatter
import net.azisaba.interchat.api.util.AsyncUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import java.util.stream.Collectors

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
object InterChatPacketListener : PacketListener {
    private val client = HttpClient(CIO)
    val sockets: MutableSet<ConnectedSocket> = Collections.synchronizedSet(LinkedHashSet())
    val getHideAllUntil = Util.memoize<UUID, Long>(10000) { uuid ->
        try {
            InterChatApi.querySql("SELECT `hide_all_until` FROM `players` WHERE `id` = ?") { ps ->
                ps.setString(1, uuid.toString())
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.getLong("hide_all_until")
                    } else {
                        0L
                    }
                }
            }
        } catch (_: Exception) {
            0L
        }
    }

    override fun handlePrivateMessage(packet: PrivateMessagePacket) {
        UserDataProviderImpl.requestDataAsync(packet.sender(), packet.server())
        UserDataProviderImpl.requestDataAsync(packet.receiver(), packet.server())
        val senderFuture = InterChatApi.userManager.fetchUser(packet.sender())
        val receiverFuture = InterChatApi.userManager.fetchUser(packet.receiver())
        AsyncUtil.collectAsync(senderFuture, receiverFuture) { sender, receiver ->
            if (sender == null || receiver == null) {
                return@collectAsync
            }
            val pos = try {
                JedisBoxProvider.get().get(RedisKeys.azisabaReportPlayerPos(sender.id()), PlayerPosData.NETWORK_CODEC)
                    .toWorldPos()
            } catch (_: Exception) {
                null
            }
            val info = SenderInfo(sender, packet.server(), null, pos)
            val formattedText = MessageFormatter.formatPrivateChat(
                PrivateMessagePacket.FORMAT,
                info,
                receiver,
                packet.message(),
                packet.transliteratedMessage(),
                emptyMap(),
            )
            val coloredText =
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(formattedText)
                    .let { LegacyComponentSerializer.legacySection().serialize(it) }
            val toRemove = sockets.parallelStream().filter { socket ->
                if (receiver.id() == socket.uuid) {
                    try {
                        if (getHideAllUntil(socket.uuid!!) > System.currentTimeMillis()) {
                            return@filter false // continue loop
                        }
                        if (InterChatApi.userManager.isBlocked(socket.uuid!!, sender.id()).join()) {
                            return@filter false // continue loop
                        }
                    } catch (_: Exception) {
                    }
                    runBlocking {
                        !socket.sendPacket(OutgoingComponentPacket(LegacyComponentSerializer.legacySection().deserialize(coloredText)
                            .hoverEvent(Component.text("クリックで返信", NamedTextColor.WHITE))
                            .clickEvent(ClickEvent.suggestCommand("/cguild tell ${sender.name()} "))))
                    }
                } else {
                    false
                }
            }.collect(Collectors.toSet())
            sockets -= toRemove
        }
    }

    override fun handleGuildMessage(packet: GuildMessagePacket) {
        UserDataProviderImpl.requestDataAsync(packet.sender(), packet.server())
        val guildFuture = InterChatApi.guildManager.fetchGuildById(packet.guildId())
        val userFuture = InterChatApi.userManager.fetchUser(packet.sender())
        AsyncUtil.collectAsync(guildFuture, userFuture) { guild, user ->
            if (guild == null || user == null || guild.deleted()) {
                return@collectAsync
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    client.post("https://worker-scripts.azisaba.workers.dev/interchat/add_guild_message") {
                        header("Authorization", "Bearer ${ServerConfig.instance.workerScriptsApiKey}")
                        header("Content-Type", "application/json")
                        setBody(JSON.encodeToString(JsonObject(mapOf(
                            "guild_id" to JsonPrimitive(packet.guildId()),
                            "server" to JsonPrimitive(packet.server()),
                            "sender" to JsonPrimitive(packet.sender().toString()),
                            "message" to JsonPrimitive(packet.message()),
                            "transliterated_message" to JsonPrimitive(packet.transliteratedMessage()),
                        ))))
                    }
                }
            }
            val members = guild.members.join()
            val nickname = members.stream().filter { it.uuid() == user.id() }.findAny().map(GuildMember::nickname)
            val pos = try {
                JedisBoxProvider.get().get(RedisKeys.azisabaReportPlayerPos(user.id()), PlayerPosData.NETWORK_CODEC)
                    .toWorldPos()
            } catch (_: Exception) {
                null
            }
            val info = SenderInfo(user, packet.server(), nickname.orElse(null), pos)
            val formattedText = MessageFormatter.format(
                guild.format(),
                guild,
                info,
                packet.message(),
                packet.transliteratedMessage(),
                emptyMap(),
            )
            val coloredText = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedText)
            val toRemove = sockets.parallelStream().filter { socket ->
                if (members.any { m -> !m.hiddenByMember() && m.uuid() == socket.uuid }) {
                    try {
                        if (getHideAllUntil(socket.uuid!!) > System.currentTimeMillis()) {
                            return@filter false // continue loop
                        }
                        if (InterChatApi.userManager.isBlocked(socket.uuid!!, packet.sender()).join()) {
                            return@filter false // continue loop
                        }
                    } catch (_: Exception) {
                    }
                    runBlocking {
                        val sent = if (socket.plainText) {
                            socket.sendPacket(
                                OutgoingWebGuildMessagePacket(
                                    packet.guildId(),
                                    packet.server(),
                                    packet.sender(),
                                    packet.message(),
                                    packet.transliteratedMessage(),
                                )
                            )
                        } else {
                            socket.sendPacket(OutgoingComponentPacket(coloredText))
                        }
                        !sent
                    }
                } else {
                    false
                }
            }.collect(Collectors.toSet())
            sockets -= toRemove
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
                    if (socket.plainText) {
                        if (socket.uuid == to.id() || socket.uuid in members) {
                            socket.sendPacket(
                                OutgoingWebGuildInvitePacket(
                                    guild.id(),
                                    from.id(),
                                    from.name(),
                                    to.id(),
                                    to.name(),
                                    guild.name(),
                                )
                            )
                        }
                        return@forEach
                    }
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
