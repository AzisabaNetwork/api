package net.azisaba.api.spigot

import net.azisaba.api.Logger
import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.spigot.common.scheduler.PlatformScheduler
import net.azisaba.api.spigot.tasks.UploadLifeAuctionsFileTask
import net.azisaba.api.spigot.tasks.UploadLifeItemsTask
import net.azisaba.api.spigot.tasks.UploadMythicMobsTask
import net.azisaba.api.spigot.privacy.PrivacyCommand
import org.bukkit.plugin.java.JavaPlugin

class SpigotPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: SpigotPlugin
            private set
    }

    lateinit var scheduler: PlatformScheduler
        private set

    init {
        instance = this
    }

    override fun onEnable() {
        logger.registerLogger()
        scheduler = PlatformScheduler.create(this)
        if (PlatformScheduler.isFolia()) logger.info("Detected Folia; using region-aware schedulers")
        logger.info("Loading config")
        PluginConfig.loadConfig(dataFolder.toPath())
        logger.info("Connecting to Redis server")
        RedisManager
        server.pluginManager.registerEvents(RedisManager, this)
        //logger.info("Connecting to database")
        val privacyCommand = PrivacyCommand(this)
        getCommand("api-privacy")?.setExecutor(privacyCommand)
            ?: error("api-privacy command is not defined in plugin.yml")
        server.pluginManager.registerEvents(privacyCommand, this)

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
            if (PluginConfig.instance.mythicMobs.uploadItems) {
                UploadLifeItemsTask.schedule(20 * 5)
            }
        } else {
            logger.info("MythicMobs is not installed.")
            Logger.currentLogger.info("Unavailable cause:", MythicAPI.unavailableCause)
        }
    }

    override fun onDisable() {
        if (::scheduler.isInitialized) scheduler.cancelTasks()
        RedisManager.subscriberThread.shutdownNow()
        RedisManager.pingThread.shutdownNow()
        RedisManager.pool.destroy()
    }
}
