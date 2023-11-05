package net.azisaba.api.server.interchat

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.interchat.protocol.OutgoingComponentPacket
import net.azisaba.api.server.interchat.protocol.OutgoingFeedbackPacket
import net.azisaba.api.server.interchat.protocol.OutgoingMessagePacket
import net.azisaba.api.server.interchat.protocol.OutgoingPacket
import net.azisaba.api.util.JSON
import net.kyori.adventure.text.Component
import java.util.UUID

@Suppress("SqlResolve", "SqlNoDataSourceInspection")
data class ConnectedSocket(var uuid: UUID?, var server: String, val connection: DefaultWebSocketSession) {
    suspend fun sendPacket(packet: OutgoingPacket): Boolean =
        try {
            connection.send(JSON.encodeToString(packet))
            true
        } catch (e: Exception) {
            false
        }

    suspend fun sendMessage(message: String) = sendPacket(OutgoingMessagePacket(message))

    suspend fun sendMessage(component: Component) = sendPacket(OutgoingComponentPacket(component))

    suspend fun sendFeedback(component: Component) = sendPacket(OutgoingFeedbackPacket(component))

    fun getSelectedGuildId(): Long =
        InterChatApi.userManager.fetchUser(uuid ?: error("uuid is not set")).join().selectedGuild()
}
