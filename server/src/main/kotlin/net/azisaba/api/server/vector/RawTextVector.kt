package net.azisaba.api.server.vector

import kotlinx.serialization.Serializable

@Serializable
data class RawTextVector(
    val id: String,
    val text: String,
    val metadata: Map<String, String> = emptyMap()
)
