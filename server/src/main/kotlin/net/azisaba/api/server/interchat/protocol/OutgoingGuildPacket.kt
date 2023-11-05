package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("guild")
@Serializable
data class OutgoingGuildPacket(val guildId: Long) : OutgoingPacket
