package net.azisaba.api.server.resources

import com.stripe.model.Event
import com.stripe.model.checkout.Session
import com.stripe.net.ApiResource
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.azisaba.api.data.IProduct
import net.azisaba.api.data.Product
import net.azisaba.api.data.PurchaseData
import net.azisaba.api.data.SaraProduct
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.server.storage.PersistentDataStore
import net.azisaba.api.server.util.StripeUtil
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.collections.set

@Serializable
@Resource("/store")
class Store {
    companion object {
        private val ranks = listOf("100000000", "100000", "50000", "10000", "5000", "2000", "1000", "500", "100")

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
                    "price_id" to it.priceId,
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
                            "product_id" to it.productId,
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
            val referer = call.request.header("Referer")
            if (referer == null || (!referer.startsWith("https://newstore.azisaba.net") && !referer.startsWith("http://localhost:"))) {
                return call.respondJson(mapOf("error" to "invalid_referer"), status = HttpStatusCode.BadRequest)
            }
            val body = JSON.parseToJsonElement(call.receiveText()).jsonObject
            val name = body["name"]!!.jsonPrimitive.content
            val uuid = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.Players.getIdByUsername(name)
            }
            if (uuid == null) {
                return call.respondJson(mapOf("error" to "name_not_found"), status = HttpStatusCode.BadRequest)
            }
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
            // flatten products (merge multiple product A's into one)
            val productMap = productsToBuy.groupBy { it["price_id"] as String }.mapValues { it.value.size.toLong() }
            val saraPrice = validSaraProducts.first()["price"] as Int
            val session = StripeUtil.createCheckoutSession(referer, productMap, validSaraProducts.first()["product_id"] as String, saraPrice, highestSara)
            val persistentData = mutableListOf<IProduct>()
            persistentData += productsToBuy.map { Product(uuid, it["id"] as Long) }
            if (saraProductsToBuy.isNotEmpty()) {
                persistentData += SaraProduct(uuid, saraPrice)
            }
            PersistentDataStore.getListContainer<IProduct>().data["session_${session.id}"] = persistentData
            call.respondJson(mapOf("url" to session.url))
        }
    }

    @Serializable
    @Resource("webhook")
    data class Webhook(
        @Suppress("unused")
        val parent: Store,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val payload = call.receiveText()
            var event = ApiResource.GSON.fromJson(payload, Event::class.java)
            if (ServerConfig.instance.stripe.webhookSecret.isNotBlank()) {
                if (call.request.header("Stripe-Signature") != null) {
                    val signature = call.request.header("Stripe-Signature")!!
                    event = com.stripe.net.Webhook.constructEvent(payload, signature, ServerConfig.instance.stripe.webhookSecret)
                } else {
                    call.respondJson(mapOf("error" to "missing signature"), status = HttpStatusCode.BadRequest)
                    return
                }
            }
            val stripeObject = if (event.dataObjectDeserializer.`object`?.isPresent == true) {
                event.dataObjectDeserializer.`object`.get()
            } else {
                call.respondJson(mapOf("error" to "could not deserialize object"), status = HttpStatusCode.BadRequest)
                return
            }
            if (event.type == "checkout.session.completed") {
                stripeObject as Session
                Util.sendDiscordWebhookAsync(ServerConfig.instance.stripe.discordNotifyUrl, null, """
                    `${stripeObject.customerDetails.email}` が ${stripeObject.amountTotal} ${stripeObject.currency.uppercase()} の支払いが完了しました。
                    Checkout Session ID: `${stripeObject.id}`
                    Payment Intent ID: [`${stripeObject.paymentIntent}`](https://dashboard.stripe.com/payments/${stripeObject.paymentIntent})
                """.trimIndent())
                RedisManager.pool.resource.use { jedis ->
                    var uuid: UUID? = null
                    PersistentDataStore.getListContainer<IProduct>().data["session_${stripeObject.id}"]?.forEach { prod ->
                        uuid = prod.uuid
                        if (!ServerConfig.instance.stripe.testMode) {
                            jedis.publish(
                                "azisaba-api:store:purchase-item",
                                JSON.encodeToString(IProduct.serializer(), prod)
                            )
                        }
                    }
                    if (uuid != null) {
                        jedis.publish(
                            "azisaba-api:store:purchase",
                            JSON.encodeToString(PurchaseData(uuid, stripeObject.amountTotal))
                        )
                    }
                }
            }
            call.respondJson(mapOf("info" to "processed"))
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
