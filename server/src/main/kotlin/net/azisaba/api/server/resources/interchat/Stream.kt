@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.api.server.resources.interchat

import io.ktor.resources.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.server.interchat.ConnectedSocket
import net.azisaba.api.server.interchat.InterChatApi
import net.azisaba.api.server.interchat.InterChatPacketListener
import net.azisaba.api.server.interchat.JedisBoxProvider
import net.azisaba.api.server.plugins.authSimple
import net.azisaba.api.server.resources.WebSocketRequestHandler
import net.azisaba.api.util.JSON
import net.azisaba.interchat.api.network.Protocol
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket

@Serializable
@Resource("/interchat/stream")
class Stream : WebSocketRequestHandler() {
    override suspend fun DefaultWebSocketServerSession.handleRequest() {
        val server = call.request.queryParameters["server"] ?: run {
            return close(CloseReason(CloseReason.Codes.GOING_AWAY, "server parameter must be specified"))
        }
        val conn = ConnectedSocket(null, server, this)
        InterChatPacketListener.sockets += conn
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val packet = JSON.decodeFromString<Packet>(frame.readText())
                if (packet is AuthPacket && conn.uuid == null) {
                    val principal = authSimple(packet.key) ?: run {
                        return this.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Failed to authenticate you."))
                    }
                    conn.uuid = principal.player
                }
                if (conn.uuid != null) {
                    when (packet) {
                        is AuthPacket -> {}
                        is MessagePacket -> {
                            val guildId = packet.guildId ?: InterChatApi.userManager.fetchUser(conn.uuid!!).join().focusedGuild()
                            InterChatApi.guildManager.getMember(guildId, conn.uuid!!).exceptionally { null }.join() ?: continue
                            val guildMessagePacket =
                                GuildMessagePacket(guildId, conn.server, conn.uuid!!, packet.message, null)
                            Protocol.GUILD_MESSAGE.send(JedisBoxProvider.get().pubSubHandler, guildMessagePacket)
                        }
                        is FocusPacket -> {
                            InterChatApi.queryExecutor.query("UPDATE `players` SET `focused_guild` = ? WHERE `id` = ?") { ps ->
                                ps.setLong(1, packet.guildId)
                                ps.setString(2, conn.uuid.toString())
                                ps.executeUpdate()
                            }
                        }
                        is SwitchServerPacket -> {
                            conn.server = packet.server
                        }
                    }
                }
            }
        } finally {
            InterChatPacketListener.sockets -= conn
        }
    }

    @Serializable
    sealed interface Packet

    @SerialName("auth")
    @Serializable
    data class AuthPacket(val key: String) : Packet

    @SerialName("message")
    @Serializable
    data class MessagePacket(val guildId: Long?, val message: String) : Packet

    @SerialName("focus")
    @Serializable
    data class FocusPacket(val guildId: Long) : Packet

    @SerialName("switch_server")
    @Serializable
    data class SwitchServerPacket(val server: String) : Packet
}
