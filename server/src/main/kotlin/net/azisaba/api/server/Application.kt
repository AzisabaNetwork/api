package net.azisaba.api.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import net.azisaba.api.Logger.Companion.registerLogger
import net.azisaba.api.server.plugins.configureRouting
import net.azisaba.api.server.plugins.configureSecurity
import net.azisaba.api.server.plugins.configureSerialization
import net.azisaba.api.server.storage.PersistentDataStore
import org.slf4j.LoggerFactory

fun main() {
    ServerConfig // load configuration
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        watchPaths = listOf("classes"),
        module = Application::appModule
    ).start(wait = true)
}

fun Application.appModule() {
    LoggerFactory.getLogger("api-ktor").registerLogger() // register logger
    TaskScheduler
    PersistentDataStore // Load persistent data
    install(Resources)
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    DatabaseManager // Make connections to database
    configureRouting()
    configureSecurity()
    configureSerialization()
}
