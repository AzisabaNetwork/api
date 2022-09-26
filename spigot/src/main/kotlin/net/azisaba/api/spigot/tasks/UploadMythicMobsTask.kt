package net.azisaba.api.spigot.tasks

import kotlinx.serialization.encodeToString
import net.azisaba.api.data.MythicSpawnerData
import net.azisaba.api.spigot.PluginConfig
import net.azisaba.api.spigot.RedisManager
import net.azisaba.api.spigot.common.mythic.MythicAPI
import net.azisaba.api.util.JSON

object UploadMythicMobsTask : AbstractTask() {
    override fun run() {
        val list =
            PluginConfig.instance
                .mythicMobs
                .lifeServers
                .map { (server, value) -> process(server, value) }
                .flatten()
        RedisManager.pool.resource.use { jedis ->
            jedis.mset(*list.toTypedArray())
        }

        // schedule next task
        schedule(20)
    }

    private fun process(childServer: String, data: List<String>, server: String = "life"): List<String> =
        data.map { processSingle(server, childServer, it) }.flatten()

    private fun processSingle(server: String, childServer: String, spawnerName: String): List<String> {
        if (spawnerName == "spawnerName") return emptyList()
        val mythicSpawner = MythicAPI.instance?.getSpawner(spawnerName) ?: return emptyList()
        val mobType = mythicSpawner.getMobType()
        val spawnerData = MythicSpawnerData(
            mythicSpawner.getName(),
            mobType.getTypeName(),
            mobType.getDisplayName(),
            mythicSpawner.getCooldownSeconds(),
            mythicSpawner.getRemainingCooldownSeconds(),
            mythicSpawner.getWarmupSeconds(),
            mythicSpawner.getRemainingWarmupSeconds(),
        )
        val expiresAt = System.currentTimeMillis() + 60000L // 1 minute
        val data = "$expiresAt\u0000$childServer\u0000" + JSON.encodeToString(spawnerData)
        return listOf("azisaba-api:$server:mythicmobs-spawner:$childServer:${spawnerData.spawnerName}", data)
    }
}
