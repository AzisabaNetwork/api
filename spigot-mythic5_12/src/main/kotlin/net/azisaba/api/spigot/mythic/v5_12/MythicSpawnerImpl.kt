package net.azisaba.api.spigot.mythic.v5_12

import io.lumine.mythic.bukkit.MythicBukkit
import net.azisaba.api.spigot.common.mythic.MythicMob
import net.azisaba.api.spigot.common.mythic.MythicSpawner
import io.lumine.mythic.core.spawning.spawners.MythicSpawner as MMythicSpawner

data class MythicSpawnerImpl(
    val handle: MMythicSpawner,
) : MythicSpawner {
    override fun getName(): String = handle.name

    override fun getMobType(): MythicMob =
        MythicMobImpl(
            MythicBukkit
                .inst()
                .mobManager
                .getMythicMob(handle.typeName)
                .get(),
        )

    override fun getCooldownSeconds(): Int = handle.cooldownSeconds

    override fun getRemainingCooldownSeconds(): Int = handle.remainingCooldownSeconds

    override fun getWarmupSeconds(): Int = handle.warmupSeconds

    override fun getRemainingWarmupSeconds(): Int = handle.remainingWarmupSeconds
}
