package net.azisaba.api.spigot.common.mythic

interface MythicSpawner {
    fun getName(): String

    fun getMobType(): MythicMob

    fun getCooldownSeconds(): Int

    fun getRemainingCooldownSeconds(): Int

    fun getWarmupSeconds(): Int

    fun getRemainingWarmupSeconds(): Int
}
