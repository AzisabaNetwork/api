package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName

@SerialName("guild")
data class OutgoingGuildPacket(val guildId: Long) : OutgoingPacket
