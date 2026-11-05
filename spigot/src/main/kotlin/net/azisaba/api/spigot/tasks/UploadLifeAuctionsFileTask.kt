package net.azisaba.api.spigot.tasks

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import net.azisaba.api.Logger
import net.azisaba.api.spigot.PluginConfig
import net.azisaba.api.spigot.RedisManager
import net.azisaba.api.spigot.data.CAData
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Base64
import org.bukkit.configuration.file.YamlConfiguration as BukkitYamlConfiguration

object UploadLifeAuctionsFileTask : AbstractTask() {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    override fun run() {
        val file = PluginConfig.instance.paths.lifeAuctions?.let { File(it) } ?: return
        if (!file.exists()) return
        val start = System.currentTimeMillis()
        Logger.currentLogger.info("Updating life auctions data")
        val data = file.inputStream().use { yaml.decodeFromStream(CAData.serializer(), it) }
        if (data.items.isEmpty()) {
            Logger.currentLogger.info("No life auctions data found. Skipped updating.")
            return
        }
        // Loaded only when some ItemBytes cannot be deserialized.
        val bukkitYaml by lazy { BukkitYamlConfiguration.loadConfiguration(file) }
        RedisManager.uploadAuctionData(*data.items.mapNotNull { (key, info) ->
            var itemBytes = info.itemBytes
            val stack = try {
                ItemStack.deserializeBytes(Base64.getDecoder().decode(info.itemBytes))
            } catch (e: Exception) {
                // ItemBytes may contain data the current server version cannot parse (e.g. legacy
                // attribute ids such as "generic.movementSpeed"). CrazyAuctions also stores the item
                // via Bukkit's YAML serialization under "Items.<key>.Item", so fall back to it and
                // re-encode the bytes so API consumers get a readable item.
                val fallback = try {
                    bukkitYaml.getItemStack("Items.$key.Item")
                } catch (e2: Exception) {
                    e.addSuppressed(e2)
                    null
                }
                if (fallback == null) {
                    Logger.currentLogger.warn(
                        "Skipping life auction item (key: $key, StoreID: ${info.storeId}, Seller: ${info.seller}) because it could not be deserialized: ${e.message}",
                    )
                    return@mapNotNull null
                }
                itemBytes = Base64.getEncoder().encodeToString(fallback.serializeAsBytes())
                fallback
            }
            val displayName = if (stack.hasItemMeta() && stack.itemMeta.hasDisplayName()) {
                stack.itemMeta.displayName
            } else {
                stack.i18NDisplayName ?: stack.type.name
            }
            val lore = if (stack.hasItemMeta() && stack.itemMeta.hasLore()) {
                stack.itemMeta.lore!!.joinToString("\n")
            } else {
                null
            }
            info.toAuctionInfo(displayName, lore, itemBytes)
        }.toTypedArray())
        Logger.currentLogger.info("Updated life auctions data (took {} ms)", System.currentTimeMillis() - start)
    }
}
