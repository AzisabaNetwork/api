package net.azisaba.api.spigot.mythic.v4_12

import io.lumine.xikage.mythicmobs.MythicMobs
import net.azisaba.api.spigot.common.mythic.MythicMob
import net.azisaba.api.spigot.common.mythic.MythicSpawner
import io.lumine.xikage.mythicmobs.spawning.spawners.MythicSpawner as MMythicSpawner

data class MythicSpawnerImpl(val handle: MMythicSpawner) : MythicSpawner {
    override fun getName(): String = handle.name

    override fun getMobType(): MythicMob = MythicMobImpl(MythicMobs.inst().mobManager.getMythicMob(handle.typeName)!!)

    override fun getCooldownSeconds(): Int = handle.cooldownSeconds

    override fun getRemainingCooldownSeconds(): Int = handle.remainingCooldownSeconds

    override fun getWarmupSeconds(): Int = handle.warmupSeconds

    override fun getRemainingWarmupSeconds(): Int = handle.remainingWarmupSeconds
}
