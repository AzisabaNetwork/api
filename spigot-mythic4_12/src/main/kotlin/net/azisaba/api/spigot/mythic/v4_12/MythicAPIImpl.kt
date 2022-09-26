package net.azisaba.api.spigot.mythic.v4_12

import io.lumine.xikage.mythicmobs.MythicMobs
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.spigot.common.mythic.MythicSpawner

class MythicAPIImpl : MythicAPI {
    override fun getSpawner(name: String): MythicSpawner? =
        MythicMobs.inst().spawnerManager.getSpawnerByName(name)?.let { MythicSpawnerImpl(it) }
}
