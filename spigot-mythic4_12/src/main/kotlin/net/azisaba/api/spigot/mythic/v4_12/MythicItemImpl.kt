package net.azisaba.api.spigot.mythic.v4_12

import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.items.MythicItem as MMythicItem
import net.azisaba.api.data.ServerItemData
import net.azisaba.api.spigot.common.mythic.MythicItem
import org.bukkit.inventory.ItemStack

data class MythicItemImpl(val handle: MMythicItem) : MythicItem {
    override fun getInternalName(): String = handle.internalName

    override fun toServerItemData(): ServerItemData {
        val itemStack: ItemStack = try {
            BukkitAdapter.adapt(handle.generateItemStack(1))
        } catch (e: Throwable) {
            ItemStack(org.bukkit.Material.AIR)
        }

        val meta = itemStack.itemMeta
        val enchants = mutableMapOf<String, Int>()
        itemStack.enchantments.forEach { (enchant, level) ->
            val key = try {
                enchant.key.toString()
            } catch (e: Throwable) {
                enchant.name.lowercase()
            }
            enchants[key] = level
        }
        val customModelData = try {
            if (meta != null && meta.hasCustomModelData()) meta.customModelData else null
        } catch (e: Throwable) {
            null
        }
        val unbreakable = try {
            meta?.isUnbreakable ?: false
        } catch (e: Throwable) {
            false
        }
        val hideFlags = meta?.itemFlags?.map { it.name } ?: emptyList()
        val material = "minecraft:" + itemStack.type.name.lowercase()

        return ServerItemData(
            name = handle.internalName,
            material = material,
            displayName = if (meta?.hasDisplayName() == true) meta.displayName else null,
            lore = meta?.lore ?: emptyList(),
            customModelData = customModelData,
            amount = itemStack.amount,
            unbreakable = unbreakable,
            hideFlags = hideFlags,
            enchants = enchants,
        )
    }
}
