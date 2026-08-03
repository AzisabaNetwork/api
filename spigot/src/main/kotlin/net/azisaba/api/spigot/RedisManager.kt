package net.azisaba.api.spigot

import net.azisaba.api.Logger
import net.azisaba.api.data.AuctionInfo
import net.azisaba.api.data.IProduct
import net.azisaba.api.data.Product
import net.azisaba.api.data.PurchaseData
import net.azisaba.api.data.SaraProduct
import net.azisaba.api.util.JSON
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.exceptions.JedisConnectionException
import java.util.concurrent.Executors

object RedisManager : Listener {
    var listener: JedisPubSub? = null
    val pool: JedisPool = PluginConfig.instance.redis.createPool()
    val subscriberThread = Executors.newFixedThreadPool(1) {r -> Thread(r, "AzisabaAPI PubSub Subscriber Thread").apply { isDaemon = true } }!!
    val pingThread = Executors.newFixedThreadPool(1) {r -> Thread(r, "AzisabaAPI PubSub Ping Thread").apply { isDaemon = true } }!!
    private val pendingPurchases = PendingPurchaseStore(
        SpigotPlugin.instance.dataFolder.toPath().resolve("pending-purchases.yml"),
    )

    init {
        pool.resource.use { jedis -> jedis.ping() }
        subscriberThread.submit(this::loop)
        pingThread.submit {
            while (true) {
                try {
                    Thread.sleep(1000 * 30)
                    listener?.ping()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    Logger.currentLogger.warn("Failed to ping Redis", e)
                }
            }
        }
    }

    fun loop() {
        try {
            try {
                pool.resource.use { jedis ->
                    listener = object : JedisPubSub() {
                        override fun onMessage(channel: String, message: String) {
                            if (channel == "azisaba-api:store:purchase") {
                                val data = JSON.decodeFromString(PurchaseData.serializer(), message)
                                val player = Bukkit.getOfflinePlayer(data.uuid)
                                Bukkit.broadcastMessage("§a§l${player.name}さんが§d§l${data.amount}円§a§l寄付しました！")
                                Bukkit.broadcastMessage("§a§l${player.name}さんありがとうございます！")
                                val text =
                                    TextComponent("§6§l${player.name}さんのように寄付するにはこのメッセージをクリック！")
                                text.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://newstore.azisaba.net")
                                Bukkit.spigot().broadcast(text)
                            } else if (channel == "azisaba-api:store:purchase-item") {
                                val data = JSON.decodeFromString<IProduct>(message)
                                queuePurchase(data)
                            }
                        }
                    }
                    jedis.subscribe(listener, "azisaba-api:store:purchase", "azisaba-api:store:purchase-item")
                }
            } catch (e: JedisConnectionException) {
                Logger.currentLogger.warn("Could not subscribe", e)
            }
        } catch (e: Exception) {
            Logger.currentLogger.warn("Failed to get Jedis resource", e)
        } finally {
            subscriberThread.submit {
                try {
                    Thread.sleep(3000)
                    loop()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        processPendingPurchases(event.player)
    }

    private fun queuePurchase(product: IProduct) {
        try {
            pendingPurchases.add(product)
        } catch (e: Exception) {
            Logger.currentLogger.error("Failed to save a pending purchase for ${product.uuid}", e)
            return
        }

        Bukkit.getScheduler().runTask(SpigotPlugin.instance, Runnable {
            Bukkit.getPlayer(product.uuid)?.takeIf { it.isOnline }?.let(::processPendingPurchases)
        })
    }

    private fun processPendingPurchases(player: Player) {
        val products = try {
            pendingPurchases.get(player.uniqueId)
        } catch (e: Exception) {
            Logger.currentLogger.error("Failed to load pending purchases for ${player.uniqueId}", e)
            return
        }
        if (products.isEmpty()) return

        val config = try {
            PluginConfig.readCurrentConfig(SpigotPlugin.instance.dataFolder.toPath())
        } catch (e: Exception) {
            Logger.currentLogger.error("Failed to reload config.yml before granting purchases", e)
            return
        }

        val processed = mutableListOf<IProduct>()
        products.forEach { product ->
            val purchaseCommand = when (product) {
                is Product -> config.purchaseCommands.find { it.productId == product.id }
                is SaraProduct -> config.purchaseCommands.find { it.saraProductPrice == product.amount }
            }
            if (purchaseCommand == null) {
                Logger.currentLogger.warn("No purchase command is configured for pending purchase $product")
                return@forEach
            }

            try {
                purchaseCommand.command.forEach { command ->
                    Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        command.replace("<player>", player.name)
                            .replace("<uuid>", player.uniqueId.toString()),
                    )
                }
                processed += product
            } catch (e: Exception) {
                Logger.currentLogger.error("Failed to grant pending purchase $product", e)
            }
        }

        try {
            pendingPurchases.remove(processed)
        } catch (e: Exception) {
            Logger.currentLogger.error("Failed to remove granted purchases for ${player.uniqueId}", e)
        }
    }

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
