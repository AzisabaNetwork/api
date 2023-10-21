package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("message")
@Serializable
data class OutgoingMessagePacket(val message: String) : OutgoingPacket
