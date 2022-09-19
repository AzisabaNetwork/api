package net.azisaba.api.spigot

import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.spigot.tasks.UploadLifeAuctionsFileTask
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

        if (PluginConfig.instance.paths.lifeAuctions != null) {
            UploadLifeAuctionsFileTask.schedule(20 * 10, 20 * 60)
        }
        //logger.info("Connecting to database")
        //DatabaseManager
    }
}
