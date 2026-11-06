package net.azisaba.api.spigot.common.mythic

import org.bukkit.Bukkit

interface MythicAPI {
    companion object {
        fun getMythicMobsVersion(): String {
            val plugin =
                Bukkit.getPluginManager().getPlugin("MythicMobs")
                    ?: throw RuntimeException("MythicMobs is not installed")
            return plugin.description.version
        }

        fun getImplVersion(version: String = getMythicMobsVersion()): String {
            val (major, minor) =
                Regex("""^(\d+)\.(\d+)""").find(version)?.destructured?.let { (major, minor) ->
                    major.toInt() to minor.toInt()
                } ?: throw UnsupportedOperationException("Unsupported version: $version")
            return when {
                major == 4 && minor == 12 -> "v4_12"
                // The 5.12 implementation only uses APIs that are unchanged in later 5.x releases.
                major == 5 && minor >= 12 -> "v5_12"
                else -> throw UnsupportedOperationException("Unsupported version: $version")
            }
        }

        var unavailableCause: Throwable? = null
            private set

        val instance: MythicAPI? =
            try {
                Class.forName("net.azisaba.api.spigot.mythic.${getImplVersion()}.MythicAPIImpl").getConstructor().newInstance() as MythicAPI
            } catch (e: Exception) {
                unavailableCause = e
                null
            }
    }

    fun getSpawner(name: String): MythicSpawner?

    fun getItemNames(): List<String>

    fun getItem(name: String): MythicItem?

    fun getAllItems(): List<MythicItem> = getItemNames().mapNotNull { getItem(it) }
}
