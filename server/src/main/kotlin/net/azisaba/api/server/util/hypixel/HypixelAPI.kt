package net.azisaba.api.server.util.hypixel

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON

object HypixelAPI {
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 1000 * 30
        }
    }

    // cached for up to 1 week
    val requestWithCache = Util.memoize<String, String>(1000 * 60 * 60 * 24 * 7) {
        runBlocking {
            client.get(it) {
                header("API-Key", ServerConfig.instance.hypixelApiKey)
            }.bodyAsText()
        }
    }

    suspend fun getRaw(url: String, cacheable: Boolean = true): String =
        if (cacheable) {
            requestWithCache(url)
        } else {
            client.get(url) {
                header("API-Key", ServerConfig.instance.hypixelApiKey)
            }.bodyAsText()
        }

    suspend inline fun <reified T> get(url: String, cacheable: Boolean = true): HypixelAPIResult<T> =
        JSON.decodeFromString<HypixelAPIResult<T>>(getRaw(url, cacheable))
}
