package net.azisaba.api.server

import kotlinx.serialization.Serializable
import net.azisaba.api.server.serializers.UUIDSerializer
import net.azisaba.api.server.util.JsonUtil
import net.azisaba.api.server.util.Util
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import java.util.UUID

object RedisManager {
    private val log = LoggerFactory.getLogger(RedisManager::class.java)
    private val pool: JedisPool = ServerConfig.instance.redis.createPool()

    /**
     * Returns a list of all players.
     * Similar to [fetchPlayers], but caches the result for 10 seconds.
     */
    val getPlayers: () -> List<PlayerInfo> = Util.memoizeSupplier(1000 * 10) { fetchPlayers() }

    /**
     * Returns a list of all players. This method fetches the list from Redis and is not cached.
     */
    private fun fetchPlayers(): List<PlayerInfo> {
        val id = "velocity-redis-bridge:player:*"
        return pool.resource.use { jedis ->
            jedis.mget(*jedis.keys(id).toTypedArray()).mapNotNull { data ->
                try {
                    if (data == null) {
                        null
                    } else {
                        JsonUtil.json.decodeFromString(PlayerInfo.serializer(), data)
                    }
                } catch (e: Exception) {
                    log.error("Failed to deserialize PlayerInfo", e)
                    null
                }
            }
        }
    }
}

@Serializable
data class PlayerInfo(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val username: String,
    val hostName: String,
    val port: Int,
    val proxyServer: String,
    val childServer: String?,
)
