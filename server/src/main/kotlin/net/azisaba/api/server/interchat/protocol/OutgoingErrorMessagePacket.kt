package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("error_message")
@Serializable
data class OutgoingErrorMessagePacket(val message: String) : OutgoingPacket
