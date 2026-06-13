package net.azisaba.api.spigot.mythic.v5_12

import io.lumine.mythic.bukkit.MythicBukkit
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.spigot.common.mythic.MythicSpawner

class MythicAPIImpl : MythicAPI {
    override fun getSpawner(name: String): MythicSpawner? =
        MythicBukkit
            .inst()
            .spawnerManager
            .getSpawnerByName(name)
            ?.let { MythicSpawnerImpl(it) }
}
