package net.azisaba.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MythicSpawnerData(
    @SerialName("spawner_name")
    val spawnerName: String,
    @SerialName("mob_type")
    val mobType: String,
    @SerialName("mob_name")
    val mobName: String,
    @SerialName("cooldown_seconds")
    val cooldownSeconds: Int,
    @SerialName("remaining_cooldown_seconds")
    val remainingCooldownSeconds: Int,
    @SerialName("warmup_seconds")
    val warmupSeconds: Int,
    @SerialName("remaining_warmup_seconds")
    val remainingWarmupSeconds: Int,
)
