package net.azisaba.api.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import io.ktor.server.websocket.*
import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.server.interchat.InterChatApi
import net.azisaba.api.server.interchat.InterChatConfig
import net.azisaba.api.server.interchat.JedisBoxProvider
import net.azisaba.api.server.plugins.configureRouting
import net.azisaba.api.server.plugins.configureSecurity
import net.azisaba.api.server.plugins.configureSerialization
import net.azisaba.api.server.storage.PersistentDataStore
import net.azisaba.interchat.api.InterChatProviderProvider
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    ServerConfig // load configuration
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        watchPaths = listOf("classes"),
        module = Application::appModule
    ).start(wait = true)

    InterChatApi.dataSource.close()
    JedisBoxProvider.get().close()
}

fun Application.appModule() {
    LoggerFactory.getLogger("api-ktor").registerLogger() // register logger

    // setup interchat
    InterChatConfig // load InterChat config
    JedisBoxProvider.get()
    InterChatProviderProvider.register(InterChatApi)

    TaskScheduler
    PersistentDataStore // Load persistent data
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(Resources)
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    configureSecurity()
    configureRouting()
    configureSerialization()
}
