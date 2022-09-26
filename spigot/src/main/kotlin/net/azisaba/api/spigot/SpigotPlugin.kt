package net.azisaba.api.spigot

import net.azisaba.api.Logger
import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.spigot.tasks.UploadLifeAuctionsFileTask
import net.azisaba.api.spigot.tasks.UploadMythicMobsTask
import org.bukkit.plugin.java.JavaPlugin

class SpigotPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: SpigotPlugin
            private set
    }

    init {
        instance = this
    }

    override fun onEnable() {
        logger.registerLogger()
        logger.info("Loading config")
        PluginConfig.loadConfig(dataFolder.toPath())
        logger.info("Connecting to Redis server")
        RedisManager
        //logger.info("Connecting to database")
        //DatabaseManager

        if (PluginConfig.instance.paths.lifeAuctions != null) {
            UploadLifeAuctionsFileTask.schedule(20 * 10, 20 * 60)
        }
        // check MythicMobs
        if (PluginConfig.instance.mythicMobs.enabled && MythicAPI.instance != null) {
            Logger.currentLogger.info(
                "Detected MythicMobs version: {} (Impl version: {})",
                MythicAPI.getMythicMobsVersion(),
                MythicAPI.getImplVersion(),
            )
            UploadMythicMobsTask.schedule(20)
        } else {
            logger.info("MythicMobs is not installed.")
            Logger.currentLogger.info("Unavailable cause:", MythicAPI.unavailableCause)
        }
    }

    override fun onDisable() {
        RedisManager.pool.destroy()
    }
}
