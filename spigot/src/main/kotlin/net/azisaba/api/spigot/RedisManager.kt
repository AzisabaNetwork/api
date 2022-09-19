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
            jedis.mset(*list.toTypedArray())
        }
    }
}
