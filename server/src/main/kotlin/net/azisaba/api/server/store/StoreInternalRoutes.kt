package net.azisaba.api.server.store

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.azisaba.api.data.IProduct
import net.azisaba.api.data.Product
import net.azisaba.api.data.SaraProduct
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.resources.Store
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.server.storage.PersistentDataStore
import net.azisaba.api.util.JSON
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val SIGNATURE_TOLERANCE_SECONDS = 300L

@Serializable
private data class PrepareCheckoutRequest(
    @SerialName("order_id") val orderId: String,
    val name: String,
    val products: List<Long>,
    @SerialName("sara_products") val saraProducts: List<Long>,
)

@Serializable
private data class PreparedLineItem(
    val kind: String,
    @SerialName("price_id") val priceId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    val quantity: Int? = null,
    @SerialName("unit_amount") val unitAmount: Int? = null,
)

@Serializable
private data class PreparedProduct(
    val kind: String,
    val id: Long? = null,
    val amount: Int? = null,
)

@Serializable
private data class PreparedCheckout(
    @SerialName("player_uuid") val playerUuid: String,
    @SerialName("line_items") val lineItems: List<PreparedLineItem>,
    val products: List<PreparedProduct>,
)

@Serializable
data class FulfillmentProduct(
    val kind: String,
    @SerialName("delivery_id") val deliveryId: String,
    val id: Long? = null,
    val amount: Int? = null,
) {
    fun toProduct(uuid: UUID): IProduct = when (kind) {
        "product" -> Product(uuid, requireNotNull(id))
        "sara" -> SaraProduct(uuid, requireNotNull(amount))
        else -> error("Unknown product kind: $kind")
    }
}

@Serializable
data class StoreFulfillmentRequest(
    @SerialName("order_id") val orderId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("player_uuid") val playerUuid: String,
    @SerialName("amount_total") val amountTotal: Long,
    val currency: String,
    val mode: String,
    val products: List<FulfillmentProduct>,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null,
)

@Serializable
private data class LegacyFulfillmentRequest(
    @SerialName("event_id") val eventId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("amount_total") val amountTotal: Long,
    val currency: String,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null,
)

fun Route.registerStoreInternalRoutes() {
    post("/internal/store/checkout/prepare") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/checkout/prepare")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val request = runCatching { JSON.decodeFromString<PrepareCheckoutRequest>(body) }.getOrNull()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        if (request.products.isEmpty() && request.saraProducts.isEmpty()) {
            return@post call.respondJson(mapOf("error" to "invalid_products"), status = HttpStatusCode.BadRequest)
        }
        if (request.saraProducts.size > 1) {
            return@post call.respondJson(mapOf("error" to "too_many_sara"), status = HttpStatusCode.BadRequest)
        }
        val uuid = SpicyAzisaBan.Players.getIdByUsername(request.name)
            ?: return@post call.respondJson(mapOf("error" to "name_not_found"), status = HttpStatusCode.BadRequest)
        val highestSara = Store.getHighestSara(request.name)
            ?: return@post call.respondJson(mapOf("error" to "name_not_found"), status = HttpStatusCode.BadRequest)
        if (request.products.contains(ServerConfig.instance.store.gamingSaraId) && Store.hasGamingSara(request.name)) {
            return@post call.respondJson(mapOf("error" to "already_has_gaming_sara"), status = HttpStatusCode.BadRequest)
        }
        val (availableProducts, availableSaraProducts) = Store.getAllProducts()
        val productById = availableProducts.associateBy { it["id"] as Long }
        val products = request.products.mapNotNull(productById::get)
        if (products.size != request.products.size) {
            return@post call.respondJson(mapOf("error" to "invalid_products"), status = HttpStatusCode.BadRequest)
        }
        val saraById = availableSaraProducts.associateBy { it["id"] as Long }
        val saraProducts = request.saraProducts.mapNotNull(saraById::get)
        val sara = saraProducts.singleOrNull()
        if (saraProducts.size != request.saraProducts.size || (sara != null && (sara["price"] as Int) <= highestSara)) {
            return@post call.respondJson(mapOf("error" to "invalid_sara"), status = HttpStatusCode.BadRequest)
        }
        val lineItems = products.groupingBy { it["price_id"] as String }.eachCount().map { (priceId, quantity) ->
            PreparedLineItem(kind = "price", priceId = priceId, quantity = quantity)
        }.toMutableList()
        if (sara != null) {
            lineItems += PreparedLineItem(
                kind = "sara",
                productId = sara["product_id"] as String,
                unitAmount = (sara["price"] as Int) - highestSara,
            )
        }
        val fulfillment = products.map { PreparedProduct(kind = "product", id = it["id"] as Long) }.toMutableList()
        if (sara != null) fulfillment += PreparedProduct(kind = "sara", amount = sara["price"] as Int)
        call.respondJson(PreparedCheckout(uuid.toString(), lineItems, fulfillment))
    }

    post("/internal/store/fulfill") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/fulfill")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val fulfillment = runCatching { JSON.decodeFromString<StoreFulfillmentRequest>(body) }.getOrNull()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        if (runCatching { UUID.fromString(fulfillment.playerUuid) }.isFailure || fulfillment.products.isEmpty()) {
            return@post call.respondJson(mapOf("error" to "invalid_fulfillment"), status = HttpStatusCode.BadRequest)
        }
        when (StoreFulfillmentOutbox.accept(fulfillment, body)) {
            StoreFulfillmentOutbox.AcceptResult.ACCEPTED,
            StoreFulfillmentOutbox.AcceptResult.DUPLICATE -> call.respondJson(mapOf("status" to "accepted"))
            StoreFulfillmentOutbox.AcceptResult.CONFLICT ->
                call.respondJson(mapOf("error" to "order_conflict"), status = HttpStatusCode.Conflict)
        }
    }

    post("/internal/store/legacy-session/fulfill") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/legacy-session/fulfill")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val legacy = runCatching { JSON.decodeFromString<LegacyFulfillmentRequest>(body) }.getOrNull()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        val products = PersistentDataStore.getListContainer<IProduct>().data["session_${legacy.sessionId}"]
            ?: return@post call.respondJson(mapOf("error" to "legacy_session_not_found"), status = HttpStatusCode.NotFound)
        val uuid = products.firstOrNull()?.uuid
            ?: return@post call.respondJson(mapOf("error" to "legacy_session_empty"), status = HttpStatusCode.BadRequest)
        val fulfillment = StoreFulfillmentRequest(
            orderId = "legacy-${legacy.sessionId}",
            sessionId = legacy.sessionId,
            playerUuid = uuid.toString(),
            amountTotal = legacy.amountTotal,
            currency = legacy.currency,
            mode = if (ServerConfig.instance.store.testMode) "test" else "live",
            products = products.mapIndexed { index, product ->
                when (product) {
                    is Product -> FulfillmentProduct("product", "legacy-${legacy.sessionId}-$index", id = product.id)
                    is SaraProduct -> FulfillmentProduct("sara", "legacy-${legacy.sessionId}-$index", amount = product.amount)
                }
            },
            customerEmail = legacy.customerEmail,
            paymentIntentId = legacy.paymentIntentId,
        )
        val payload = JSON.encodeToString(StoreFulfillmentRequest.serializer(), fulfillment)
        when (StoreFulfillmentOutbox.accept(fulfillment, payload)) {
            StoreFulfillmentOutbox.AcceptResult.CONFLICT ->
                call.respondJson(mapOf("error" to "order_conflict"), status = HttpStatusCode.Conflict)
            else -> call.respondJson(mapOf("status" to "accepted"))
        }
    }
}

private object StoreRequestAuthenticator {
    fun verify(request: io.ktor.server.request.ApplicationRequest, body: String, path: String): Boolean {
        val secret = System.getenv("STORE_WORKER_HMAC_SECRET")?.takeIf(String::isNotBlank) ?: return false
        val timestamp = request.header("X-Store-Timestamp")?.toLongOrNull() ?: return false
        val requestId = request.header("X-Store-Request-Id") ?: return false
        val signature = request.header("X-Store-Signature") ?: return false
        val now = System.currentTimeMillis() / 1000
        if (kotlin.math.abs(now - timestamp) > SIGNATURE_TOLERANCE_SECONDS) return false
        val bodyHash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).toHex()
        val canonical = "$timestamp\n$requestId\nPOST\n$path\n$bodyHash"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val expected = mac.doFinal(canonical.toByteArray(StandardCharsets.UTF_8)).toHex()
        if (!MessageDigest.isEqual(expected.toByteArray(), signature.lowercase().toByteArray())) return false
        return transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.StoreRequestsTable.deleteWhere {
                AzisabaAPI.StoreRequestsTable.createdAt less (now - SIGNATURE_TOLERANCE_SECONDS)
            }
            AzisabaAPI.StoreRequestsTable.insertIgnore {
                it[AzisabaAPI.StoreRequestsTable.requestId] = requestId
                it[createdAt] = now
            }.insertedCount == 1
        }
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
