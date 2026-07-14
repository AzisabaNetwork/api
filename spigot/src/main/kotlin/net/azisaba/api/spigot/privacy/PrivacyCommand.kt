package net.azisaba.api.spigot.privacy

import net.azisaba.api.spigot.DatabaseManager
import net.azisaba.api.spigot.SpigotPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PrivacyCommand(private val plugin: SpigotPlugin) : CommandExecutor, Listener {
    companion object {
        private const val TITLE = "API プライバシー設定"
        private const val INVENTORY_SLOT = 2
        private const val ENDER_CHEST_SLOT = 6
    }

    private val saving = ConcurrentHashMap.newKeySet<UUID>()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。")
            return true
        }

        player.sendMessage("§7プライバシー設定を読み込んでいます…")
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val settings = DatabaseManager.getPrivacySettings(player.uniqueId)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (player.isOnline) openInventory(player, settings)
                })
            } catch (e: Exception) {
                plugin.logger.log(java.util.logging.Level.SEVERE, "Could not load API privacy settings for ${player.uniqueId}", e)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (player.isOnline) player.sendMessage("§c設定の読み込みに失敗しました。時間をおいて再度お試しください。")
                })
            }
        })
        return true
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? PrivacyInventoryHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (player.uniqueId != holder.playerUUID || event.rawSlot !in 0 until event.view.topInventory.size) return

        when (event.rawSlot) {
            INVENTORY_SLOT -> save(
                player,
                holder,
                holder.settings.copy(shareInventory = !holder.settings.shareInventory),
                true,
            )
            ENDER_CHEST_SLOT -> save(
                player,
                holder,
                holder.settings.copy(shareEnderChest = !holder.settings.shareEnderChest),
                false,
            )
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is PrivacyInventoryHolder) {
            event.isCancelled = true
        }
    }

    private fun save(
        player: Player,
        holder: PrivacyInventoryHolder,
        updated: PrivacySettings,
        inventory: Boolean,
    ) {
        if (!saving.add(player.uniqueId)) {
            player.sendMessage("§e設定を保存中です。少々お待ちください。")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                if (inventory) {
                    DatabaseManager.setShareInventory(player.uniqueId, updated.shareInventory)
                } else {
                    DatabaseManager.setShareEnderChest(player.uniqueId, updated.shareEnderChest)
                }
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    holder.settings = updated
                    if (player.isOnline && player.openInventory.topInventory.holder === holder) {
                        render(holder.backingInventory, updated)
                    }
                    player.sendMessage("§aAPI の共有設定を保存しました。")
                })
            } catch (e: Exception) {
                plugin.logger.log(java.util.logging.Level.SEVERE, "Could not save API privacy settings for ${player.uniqueId}", e)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (player.isOnline) player.sendMessage("§c設定を保存できませんでした。変更は反映されていません。")
                })
            } finally {
                saving.remove(player.uniqueId)
            }
        })
    }

    private fun openInventory(player: Player, settings: PrivacySettings) {
        val holder = PrivacyInventoryHolder(player.uniqueId, settings)
        val inventory = Bukkit.createInventory(holder, 9, TITLE)
        holder.backingInventory = inventory
        render(inventory, settings)
        player.openInventory(inventory)
    }

    private fun render(inventory: Inventory, settings: PrivacySettings) {
        inventory.setItem(
            INVENTORY_SLOT,
            button(Material.CHEST, "インベントリ・防具", settings.shareInventory),
        )
        inventory.setItem(
            ENDER_CHEST_SLOT,
            button(Material.ENDER_CHEST, "エンダーチェスト", settings.shareEnderChest),
        )
    }

    private fun button(material: Material, label: String, enabled: Boolean): ItemStack =
        ItemStack(material).apply {
            itemMeta = itemMeta.apply {
                setDisplayName("${if (enabled) "§a" else "§c"}$label の共有: ${if (enabled) "有効" else "無効"}")
                lore = listOf(
                    if (enabled) "§7他の API 利用者へ公開されます。" else "§7他の API 利用者には公開されません。",
                    "§eクリックして切り替え",
                )
            }
        }

    private class PrivacyInventoryHolder(
        val playerUUID: UUID,
        var settings: PrivacySettings,
    ) : InventoryHolder {
        lateinit var backingInventory: Inventory

        override fun getInventory(): Inventory = backingInventory
    }
}
