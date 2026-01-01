package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@SerialName("guild_message")
@Serializable
data class OutgoingWebGuildMessagePacket(
    @SerialName("guild_id")
    val guildId: Long,
    val server: String,
    @Serializable(with = UUIDSerializer::class)
    val sender: UUID,
    val message: String,
    @SerialName("transliterated_message")
    val transliteratedMessage: String?,
) : OutgoingPacket
