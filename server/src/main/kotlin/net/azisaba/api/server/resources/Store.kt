package net.azisaba.api.server.resources

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.util.JSON
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

@Serializable
@Resource("/store")
class Store {
    companion object {
        private val ranks = listOf("100000", "50000", "10000", "5000", "2000", "1000", "500", "100")

        fun getAllProducts() = transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.Product.all().filter { !it.hidden }.map {
                mapOf(
                    "id" to it.id.value,
                    "name" to it.name,
                    "description" to it.description,
                    "price" to it.price,
                    "image_url" to it.imageUrl,
                    "tags" to it.tags,
                    "hidden" to it.hidden,
                )
            } to
                    AzisabaAPI.SaraProduct.all().filter { !it.hidden }.map {
                        mapOf(
                            "id" to it.id.value,
                            "name" to it.name,
                            "description" to it.description,
                            "price" to it.price,
                            "image_url" to it.imageUrl,
                            "hidden" to it.hidden,
                        )
                    }
        }

        fun getHighestSara(name: String): Int? {
            val uuid = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.Players.getIdByUsername(name) ?: UUID(0, 0)
            }
            return transaction(DatabaseManager.luckPerms) {
                val groups = LuckPerms.UserPermissions.getGroupsForPlayer(uuid).map {
                    it.permission.substringAfter("group.").substringAfter("hide").substringBefore("yen")
                }
                if (groups.isEmpty()) return@transaction null
                groups.firstOrNull { it in ranks }?.toInt() ?: return@transaction 0
            }
        }
    }

    @Serializable
    @Resource("products")
    data class Products(
        @Suppress("unused")
        val parent: Store,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val (products, saraProducts) = getAllProducts()
            call.respondJson(
                mapOf(
                    "products" to products,
                    "sara_products" to saraProducts,
                )
            )
        }
    }

    @Serializable
    @Resource("pay")
    data class Pay(
        @Suppress("unused")
        val parent: Store,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val body = JSON.parseToJsonElement(call.receiveText()).jsonObject
            val name = body["name"]!!.jsonPrimitive.content
            val highestSara = getHighestSara(name) ?: return call.respondJson(mapOf("error" to "name_not_found"), status = HttpStatusCode.BadRequest)
            val (products, saraProducts) = getAllProducts()
            val receivedProductIds = body["products"]!!.jsonArray.map { it.jsonPrimitive.long }
            val receivedSaraProductIds = body["sara_products"]!!.jsonArray.map { it.jsonPrimitive.long }
            val productsToBuy = products.filter { it["id"] in receivedProductIds }
            val saraProductsToBuy = saraProducts.filter { it["id"] in receivedSaraProductIds }
            val validSaraProducts = saraProductsToBuy.filter { (it["price"] as Int) > highestSara }
            if (receivedSaraProductIds.size != validSaraProducts.size) {
                return call.respondJson(mapOf("error" to "invalid_sara"), status = HttpStatusCode.BadRequest)
            }
            if (validSaraProducts.size > 1) {
                return call.respondJson(mapOf("error" to "too_many_sara"), status = HttpStatusCode.BadRequest)
            }
            var totalPrice = productsToBuy.sumOf { it["price"] as Int }
            if (validSaraProducts.isNotEmpty()) {
                totalPrice += validSaraProducts.first()["price"] as Int - highestSara
            }
            /*
            val url = PayPayUtil.createQRCode(totalPrice, null) {
                Util.sendDiscordWebhookAsync(
                    ServerConfig.instance.paypay.discordNotifyUrl,
                    null,
                    "Minecraft ID `$name`の決済が完了しました。\n金額: $totalPrice\n商品:\n${productsToBuy.joinToString("\n") { "- ${it["name"]} (${it["price"]} JPY)" }}\n${validSaraProducts.joinToString("\n") { "- ${it["name"]} (${it["price"]} JPY)" }}",
                )
            }
            */
            call.respondJson(mapOf("url" to "https://google.com"))
        }
    }

    @Serializable
    @Resource("players/{name}/highest_sara")
    data class HighestSara(
        @Suppress("unused")
        val parent: Store,
        val name: String,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            call.respondJson(mapOf("highest_sara" to getHighestSara(name)))
        }
    }
}
