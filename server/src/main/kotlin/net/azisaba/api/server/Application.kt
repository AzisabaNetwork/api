package net.azisaba.api.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import net.azisaba.api.server.plugins.configureRouting
import net.azisaba.api.server.plugins.configureSecurity
import net.azisaba.api.server.plugins.configureSerialization

fun main() {
    ServerConfig // load configuration
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", watchPaths = listOf("classes")) {
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
    }.start(wait = true)
}
