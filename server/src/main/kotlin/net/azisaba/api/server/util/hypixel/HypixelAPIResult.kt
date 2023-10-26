package net.azisaba.api.server.util.hypixel

import kotlinx.serialization.Serializable

@Serializable
data class HypixelAPIResult<T>(
    val success: Boolean,
    val player: T? = null,
)
