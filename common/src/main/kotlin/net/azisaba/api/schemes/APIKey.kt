package net.azisaba.api.schemes

import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class APIKey(
    val id: Long,
    val key: String,
    @Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val createdAt: Long,
    val uses: Long,
) {
    companion object : Table<APIKey>(APIKey::class)
}
