package net.azisaba.api.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.velocity.commands.APIKeyCommand
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(id = "azisaba-api", name = "Azisaba API", version = "dev",
    description = "Provides a command to generate API keys", authors = ["Azisaba Network"])
open class VelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    logger: Logger,
    @DataDirectory val dataDirectory: Path,
) {
    init {
        logger.registerLogger()
        logger.info("Loading config")
        PluginConfig.loadConfig(logger, dataDirectory)
        logger.info("Connecting to database")
        DatabaseManager
    }

    @Subscribe
    fun onProxyInitialization(e: ProxyInitializeEvent) {
        server.commandManager.register(APIKeyCommand.createCommand())
    }
}
