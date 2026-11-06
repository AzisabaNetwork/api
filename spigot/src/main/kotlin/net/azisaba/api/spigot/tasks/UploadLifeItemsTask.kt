package net.azisaba.api.spigot.tasks

import kotlinx.serialization.encodeToString
import net.azisaba.api.data.ServerItemData
import net.azisaba.api.spigot.PluginConfig
import net.azisaba.api.spigot.RedisManager
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.util.JSON

object UploadLifeItemsTask : AbstractTask() {
    override fun run() {
        val mythicApi = MythicAPI.instance ?: return
        val items = mythicApi.getAllItems().map { it.toServerItemData() }
        if (items.isNotEmpty()) {
            val servers = PluginConfig.instance.mythicMobs.itemServers.ifEmpty { listOf("life") }
            val childServers = PluginConfig.instance.mythicMobs.lifeServers.keys.filter { it != "anyServerName" }.ifEmpty { listOf("life") }
            val list = mutableListOf<String>()
            val expiresAt = System.currentTimeMillis() + 1000L * 60 * 10 // 10 minutes

            for (server in servers) {
                for (childServer in childServers) {
                    for (item in items) {
                        val data = "$expiresAt\u0000$childServer\u0000" + JSON.encodeToString(ServerItemData.serializer(), item)
                        list.add("azisaba-api:$server:item:$childServer:${item.name}")
                        list.add(data)
                    }
                    val listData = "$expiresAt\u0000$childServer\u0000" + JSON.encodeToString(items)
                    list.add("azisaba-api:$server:items:$childServer")
                    list.add(listData)
                }
            }
            if (list.isNotEmpty()) {
                RedisManager.pool.resource.use { jedis ->
                    jedis.mset(*list.toTypedArray())
                }
            }
        }

        // schedule next task (every 60 seconds)
        schedule(20 * 60)
    }
}
