package net.azisaba.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import net.azisaba.api.plugins.configureRouting
import net.azisaba.api.plugins.configureSecurity
import net.azisaba.api.plugins.configureSerialization

fun main() {
    ServerConfig // load configuration
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", watchPaths = listOf("classes")) {
        install(Resources)
        install(CallLogging)
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }

        configureDatabase()
        configureRouting()
        configureSecurity()
        configureSerialization()
    }.start(wait = true)
}
