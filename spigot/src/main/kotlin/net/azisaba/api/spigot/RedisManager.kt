package net.azisaba.api.spigot

import net.azisaba.api.data.AuctionInfo
import net.azisaba.api.util.JSON
import redis.clients.jedis.JedisPool

object RedisManager {
    private val pool: JedisPool = PluginConfig.instance.redis.createPool()

    fun uploadAuctionData(vararg data: AuctionInfo) {
        val list = mutableListOf<String>()
        data.forEach { auction ->
            list.add("azisaba-api:auction:${auction.storeId}")
            list.add(JSON.encodeToString(AuctionInfo.serializer(), auction))
        }
        pool.resource.use { jedis ->
            val removeList = mutableListOf<String>()
            jedis.mget(*jedis.keys("azisaba-api:auction:*").toTypedArray())
                .map { JSON.decodeFromString(AuctionInfo.serializer(), it) }
                .filter { it.expiresAt > 9000 }
                .filter { a1 -> data.all { a2 -> a1.storeId != a2.storeId  } }
                .forEach { auction ->
                    removeList.add("azisaba-api:auction:${auction.storeId}")
                    removeList.add(JSON.encodeToString(AuctionInfo.serializer(), auction.copy(expiresAt = 1L)))
                }
            if (list.isEmpty() && removeList.isEmpty()) return@use
            jedis.mset(*(list + removeList).toTypedArray())
        }
    }
}
