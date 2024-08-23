package net.azisaba.api.server.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.azisaba.api.Logger
import net.azisaba.api.server.TaskScheduler
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.schedule
import kotlin.io.readBytes

object Util {
    inline fun <T, R> memoize(expireAfter: Long = 0, crossinline fn: (T) -> R): (T) -> R {
        val cache = mutableMapOf<T, Pair<R, Long>>()

        return {
            val now = System.currentTimeMillis()
            val (value, lastUsed) = cache[it] ?: Pair(fn(it), now)
            if (lastUsed == now || (expireAfter > 0 && now - lastUsed > expireAfter)) {
                cache[it] = Pair(fn(it), now)
            }
            value
        }
    }

    inline fun <T, U, R> memoize2(expireAfter: Long = 0, crossinline fn: (T, U) -> R): (T, U) -> R {
        val cache = mutableMapOf<Pair<T, U>, Pair<R, Long>>()

        return { t, u ->
            val pair = Pair(t, u)
            val now = System.currentTimeMillis()
            val (value, lastUsed) = cache[pair] ?: Pair(fn(t, u), now)
            if (lastUsed == now || (expireAfter > 0 && now - lastUsed > expireAfter)) {
                if (lastUsed != now) {
                    cache[pair] = Pair(fn(t, u), now)
                } else {
                    cache[pair] = Pair(value, lastUsed)
                }
            }
            value
        }
    }

    fun <R> memoizeSupplier(expireAfter: Long = 0, fn: () -> R): () -> R {
        val updating = AtomicBoolean(false)
        var cache: Pair<R, Long>? = null

        return {
            if (updating.get()) {
                cache!!.first
            } else {
                val now = System.currentTimeMillis()
                val (value, lastFetched) = cache ?: Pair(fn(), 0L)
                if (lastFetched == 0L || (expireAfter > 0 && now - lastFetched > expireAfter)) {
                    updating.set(true)
                    fn().apply {
                        cache = Pair(this, now)
                        updating.set(false)
                    }
                } else {
                    value
                }
            }
        }
    }

    fun <R> runNoinline(fn: () -> R): R = fn()

    private val GSON = Gson()

    fun sendDiscordWebhookAsync(url: String, username: String?, content: String) {
        if (url.isBlank()) return
        TaskScheduler.schedule(1) {
            try {
                val con = URL(url).openConnection() as HttpsURLConnection
                con.setRequestMethod("POST")
                con.setRequestProperty("Content-Type", "application/json")
                con.setRequestProperty("User-Agent", "api - https://github.com/AzisabaNetwork/api")
                con.setRequestProperty("Accept", "application/json")
                con.setDoOutput(true)
                con.setConnectTimeout(5000)
                con.setReadTimeout(5000)
                val stream = con.outputStream
                val json = JsonObject()
                if (username != null) json.addProperty("username", username)
                json.addProperty("content", content)
                stream.write(GSON.toJson(json).toByteArray(StandardCharsets.UTF_8))
                stream.flush()
                stream.close()
                con.connect()
                val errorStream = con.errorStream
                if (errorStream != null) {
                    val err = String(errorStream.readBytes(), StandardCharsets.UTF_8)
                    Logger.currentLogger.warn("Discord webhook returned " + con.getResponseCode() + ": " + err)
                }
                con.inputStream.close()
                con.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
