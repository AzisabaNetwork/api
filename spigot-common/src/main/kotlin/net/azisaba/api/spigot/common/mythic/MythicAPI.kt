package net.azisaba.api.spigot.common.mythic

import org.bukkit.Bukkit

interface MythicAPI {
    companion object {
        fun getMythicMobsVersion(): String {
            val plugin = Bukkit.getPluginManager().getPlugin("MythicMobs")
                ?: throw RuntimeException("MythicMobs is not installed")
            return plugin.description.version
        }

        fun getImplVersion(version: String = getMythicMobsVersion()): String = when {
            version.startsWith("4.12.") -> "v4_12"
            else -> throw UnsupportedOperationException("Unsupported version: $version")
        }

        var unavailableCause: Throwable? = null
            private set

        val instance: MythicAPI? = try {
            Class.forName("net.azisaba.api.spigot.mythic.${getImplVersion()}.MythicAPIImpl").getConstructor().newInstance() as MythicAPI
        } catch (e: Exception) {
            unavailableCause = e
            null
        }
    }

    fun getSpawner(name: String): MythicSpawner?
}
