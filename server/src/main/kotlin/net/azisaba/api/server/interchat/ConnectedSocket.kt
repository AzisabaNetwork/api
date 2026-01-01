package net.azisaba.api.server.interchat

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.interchat.protocol.OutgoingComponentPacket
import net.azisaba.api.server.interchat.protocol.OutgoingFeedbackPacket
import net.azisaba.api.server.interchat.protocol.OutgoingMessagePacket
import net.azisaba.api.server.interchat.protocol.OutgoingPacket
import net.azisaba.api.util.JSON
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.UUID

@Suppress("SqlResolve", "SqlNoDataSourceInspection")
data class ConnectedSocket(
    var uuid: UUID?,
    var server: String,
    val connection: DefaultWebSocketSession,
    val plainText: Boolean = false,
) {
    /**
     * Sends packet to the connected socket
     * @param packet Packet to send
     * @return If the packet was sent successfully
     */
    suspend fun sendPacket(packet: OutgoingPacket): Boolean =
        try {
            val normalizedPacket = if (!plainText) {
                packet
            } else {
                when (packet) {
                    is OutgoingComponentPacket -> OutgoingMessagePacket(componentJsonToPlainText(packet.message))
                    is OutgoingFeedbackPacket -> OutgoingMessagePacket(componentJsonToPlainText(packet.json))
                    else -> packet
                }
            }
            connection.send(JSON.encodeToString(normalizedPacket))
            true
        } catch (e: Exception) {
            false
        }

    suspend fun sendMessage(message: String) = sendPacket(OutgoingMessagePacket(message))

    suspend fun sendMessage(component: Component) = sendPacket(OutgoingComponentPacket(component))

    suspend fun sendFeedback(component: Component) = sendPacket(OutgoingFeedbackPacket(component))

    fun getSelectedGuildId(): Long =
        InterChatApi.userManager.fetchUser(uuid ?: error("uuid is not set")).join().selectedGuild()

    private fun componentJsonToPlainText(json: String): String =
        try {
            val component = GsonComponentSerializer.gson().deserialize(json)
            PlainTextComponentSerializer.plainText().serialize(component)
        } catch (_: Exception) {
            json
        }
}
