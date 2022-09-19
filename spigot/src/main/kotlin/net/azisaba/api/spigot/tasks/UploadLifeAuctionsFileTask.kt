package net.azisaba.api.spigot.tasks

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import net.azisaba.api.spigot.PluginConfig
import net.azisaba.api.spigot.RedisManager
import net.azisaba.api.spigot.data.CAData
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Base64

object UploadLifeAuctionsFileTask : AbstractTask() {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    override fun run() {
        val file = PluginConfig.instance.paths.lifeAuctions?.let { File(it) } ?: return
        if (!file.exists()) return
        val data = yaml.decodeFromStream(CAData.serializer(), file.inputStream())
        RedisManager.uploadAuctionData(*data.items.values.map {
            val stack = ItemStack.deserializeBytes(Base64.getDecoder().decode(it.itemBytes))
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
            it.toAuctionInfo(displayName, lore)
        }.toTypedArray())
    }
}
