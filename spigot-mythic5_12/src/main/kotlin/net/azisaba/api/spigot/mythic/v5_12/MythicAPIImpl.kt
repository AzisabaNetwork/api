package net.azisaba.api.spigot.mythic.v5_12

import io.lumine.mythic.bukkit.MythicBukkit
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.spigot.common.mythic.MythicItem
import net.azisaba.api.spigot.common.mythic.MythicSpawner

class MythicAPIImpl : MythicAPI {
    override fun getSpawner(name: String): MythicSpawner? =
        MythicBukkit
            .inst()
            .spawnerManager
            .getSpawnerByName(name)
            ?.let { MythicSpawnerImpl(it) }

    override fun getItemNames(): List<String> =
        MythicBukkit
            .inst()
            .itemManager
            .itemNames
            ?.toList() ?: emptyList()

    override fun getItem(name: String): MythicItem? =
        MythicBukkit
            .inst()
            .itemManager
            .getItem(name)
            .orElse(null)
            ?.let { MythicItemImpl(it) }
}
