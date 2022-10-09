package net.azisaba.api.server

import kotlinx.serialization.Serializable
import net.azisaba.api.data.AuctionInfo
import net.azisaba.api.data.MythicSpawnerData
import net.azisaba.api.serializers.UUIDSerializer
import net.azisaba.api.server.storage.PersistentDataStore
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON
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

    val getAuctions: () -> List<AuctionInfo> = Util.memoizeSupplier(1000 * 60) {
        val container = PersistentDataStore.getListContainer<AuctionInfo>()
        val list = (fetchAuctions() + (container["auctions"] ?: emptyList())).distinctBy { it.storeId }
        container["auctions"] = list
        PersistentDataStore.dirty = true
        list
    }

    val getAuction: (storeId: Long) -> AuctionInfo? = Util.memoize(1000 * 60) { id -> fetchAuction(id) }

    val getSpawnerData: (server: String, childServer: String?) -> List<MythicSpawnerDataEx> =
        Util.memoize2(1000) { server, childServer -> fetchSpawnerData(server, childServer) }

    /**
     * Returns a list of all players. This method fetches the list from Redis and is not cached.
     */
    private fun fetchPlayers(): List<PlayerInfo> {
        val id = "velocity-redis-bridge:player:*"
        return pool.resource.use { jedis ->
            val keys = jedis.keys(id)
            if (keys.isEmpty()) {
                return emptyList()
            }
            jedis.mget(*keys.toTypedArray()).mapNotNull { data ->
                try {
                    if (data == null) {
                        null
                    } else {
                        JSON.decodeFromString(PlayerInfo.serializer(), data)
                    }
                } catch (e: Exception) {
                    log.error("Failed to deserialize PlayerInfo", e)
                    null
                }
            }
        }
    }

    private fun fetchAuctions(): List<AuctionInfo> {
        val id = "azisaba-api:auction:*"
        return pool.resource.use { jedis ->
            val keys = jedis.keys(id)
            if (keys.isEmpty()) {
                return emptyList()
            }
            val toDelete = mutableListOf<String>()
            jedis.mget(*keys.toTypedArray()).mapNotNull { data ->
                try {
                    if (data == null) {
                        null
                    } else {
                        val ai = JSON.decodeFromString(AuctionInfo.serializer(), data)
                        if (ai.timeTillDataRemoval < System.currentTimeMillis()) {
                            toDelete.add("azisaba-api:auction:${ai.storeId}")
                        }
                        ai
                    }
                } catch (e: Exception) {
                    log.error("Failed to deserialize AuctionInfo", e)
                    null
                }
            }.also {
                if (toDelete.isNotEmpty()) {
                    jedis.del(*toDelete.toTypedArray())
                }
            }
        }
    }

    private fun fetchAuction(id: Long): AuctionInfo? {
        val key = "azisaba-api:auction:$id"
        return pool.resource.use { jedis ->
            val data = jedis.get(key)
            try {
                if (data == null) {
                    null
                } else {
                    JSON.decodeFromString(AuctionInfo.serializer(), data)
                }
            } catch (e: Exception) {
                log.error("Failed to deserialize AuctionInfo", e)
                null
            }
        }
    }

    private fun fetchSpawnerData(server: String, childServer: String?): List<MythicSpawnerDataEx> {
        val key = "azisaba-api:$server:mythicmobs-spawner:${childServer ?: "*"}:*"
        return pool.resource.use { jedis ->
            val keys = jedis.keys(key)
            if (keys.isEmpty()) {
                return emptyList()
            }
            jedis.mget(*keys.toTypedArray()).mapNotNull { data ->
                try {
                    if (data == null) {
                        null
                    } else {
                        val split = data.split('\u0000')
                        val expiresAt = split[0].toLong()
                        if (expiresAt < System.currentTimeMillis()) {
                            null
                        } else {
                            val actualChildServer = split[1]
                            val json = split.drop(2).joinToString("\u0000")
                            val spawnerData = JSON.decodeFromString(MythicSpawnerData.serializer(), json)
                            MythicSpawnerDataEx(spawnerData, server, actualChildServer)
                        }
                    }
                } catch (e: Exception) {
                    log.error("Failed to deserialize MythicSpawnerData", e)
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

@Serializable
data class MythicSpawnerDataEx(
    val data: MythicSpawnerData,
    val server: String,
    val childServer: String?,
)
