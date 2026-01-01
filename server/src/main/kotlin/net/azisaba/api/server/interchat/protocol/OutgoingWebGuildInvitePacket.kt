package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@SerialName("guild_invite")
@Serializable
data class OutgoingWebGuildInvitePacket(
    @SerialName("guild_id")
    val guildId: Long,
    @Serializable(with = UUIDSerializer::class)
    val from: UUID,
    @SerialName("from_name")
    val fromName: String,
    @Serializable(with = UUIDSerializer::class)
    val to: UUID,
    @SerialName("to_name")
    val toName: String,
    @SerialName("guild_name")
    val guildName: String,
) : OutgoingPacket
