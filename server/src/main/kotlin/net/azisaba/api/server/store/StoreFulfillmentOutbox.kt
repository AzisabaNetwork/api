package net.azisaba.api.server.store

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.azisaba.api.Logger
import net.azisaba.api.data.PurchaseAnnouncementV2
import net.azisaba.api.data.StorePurchaseItemV2
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.RedisManager
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.util.UUID

object StoreFulfillmentOutbox {
    enum class AcceptResult { ACCEPTED, DUPLICATE, CONFLICT }

    fun accept(fulfillment: StoreFulfillmentRequest, payload: String): AcceptResult {
        val hash = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).toHex()
        return transaction(DatabaseManager.azisabaApi) {
            val existing = AzisabaAPI.StoreFulfillmentsTable
                .select { AzisabaAPI.StoreFulfillmentsTable.orderId eq fulfillment.orderId }
                .singleOrNull()
            if (existing != null) {
                return@transaction if (existing[AzisabaAPI.StoreFulfillmentsTable.payloadHash] == hash) {
                    AcceptResult.DUPLICATE
                } else {
                    AcceptResult.CONFLICT
                }
            }
            val now = System.currentTimeMillis() / 1000
            AzisabaAPI.StoreFulfillmentsTable.insertIgnore {
                it[AzisabaAPI.StoreFulfillmentsTable.orderId] = fulfillment.orderId
                it[AzisabaAPI.StoreFulfillmentsTable.payloadHash] = hash
                it[AzisabaAPI.StoreFulfillmentsTable.payload] = payload
                it[AzisabaAPI.StoreFulfillmentsTable.status] = "pending"
                it[AzisabaAPI.StoreFulfillmentsTable.createdAt] = now
                it[AzisabaAPI.StoreFulfillmentsTable.updatedAt] = now
            }
            AcceptResult.ACCEPTED
        }
    }

    fun dispatchPending() {
        val pending = transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.StoreFulfillmentsTable.selectAll()
                .filter {
                    it[AzisabaAPI.StoreFulfillmentsTable.status] == "pending" ||
                        it[AzisabaAPI.StoreFulfillmentsTable.status] == "failed"
                }
                .take(25)
                .map(::toPending)
        }
        pending.forEach { (orderId, payload) ->
            try {
                val fulfillment = JSON.decodeFromString<StoreFulfillmentRequest>(payload)
                if (fulfillment.mode == "live") publish(fulfillment)
                mark(orderId, if (fulfillment.mode == "live") "dispatched" else "test_skipped", null)
            } catch (exception: Exception) {
                Logger.currentLogger.error("Failed to dispatch store fulfillment $orderId", exception)
                mark(orderId, "failed", exception.message?.take(1000))
            }
        }
    }

    private fun publish(fulfillment: StoreFulfillmentRequest) {
        val uuid = UUID.fromString(fulfillment.playerUuid)
        RedisManager.pool.resource.use { jedis ->
            fulfillment.products.forEach { product ->
                val message = StorePurchaseItemV2(
                    fulfillment.orderId,
                    product.deliveryId,
                    product.toProduct(uuid),
                )
                jedis.publish(
                    "azisaba-api:store:purchase-item:v2",
                    JSON.encodeToString(StorePurchaseItemV2.serializer(), message),
                )
            }
            val announcement = PurchaseAnnouncementV2(
                fulfillment.orderId,
                "${fulfillment.orderId}:announcement",
                uuid,
                fulfillment.amountTotal,
            )
            jedis.publish(
                "azisaba-api:store:purchase:v2",
                JSON.encodeToString(PurchaseAnnouncementV2.serializer(), announcement),
            )
        }
        val email = fulfillment.customerEmail ?: "(email unavailable)"
        val payment = fulfillment.paymentIntentId ?: "(payment intent unavailable)"
        Util.sendDiscordWebhookAsync(
            ServerConfig.instance.store.discordNotifyUrl,
            null,
            """
                `$email` が ${fulfillment.amountTotal} ${fulfillment.currency.uppercase()} の支払いを完了しました。
                Checkout Session ID: `${fulfillment.sessionId}`
                Payment Intent ID: [`$payment`](https://dashboard.stripe.com/payments/$payment)
                プレイヤーUUID: $uuid
                Order ID: `${fulfillment.orderId}`
            """.trimIndent(),
        )
    }

    private fun mark(orderId: String, status: String, error: String?) {
        transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.StoreFulfillmentsTable.update({ AzisabaAPI.StoreFulfillmentsTable.orderId eq orderId }) {
                it[AzisabaAPI.StoreFulfillmentsTable.status] = status
                it[AzisabaAPI.StoreFulfillmentsTable.attempts] =
                    AzisabaAPI.StoreFulfillmentsTable.attempts + 1
                it[AzisabaAPI.StoreFulfillmentsTable.lastError] = error
                it[AzisabaAPI.StoreFulfillmentsTable.updatedAt] = System.currentTimeMillis() / 1000
            }
        }
    }

    private fun toPending(row: ResultRow) =
        row[AzisabaAPI.StoreFulfillmentsTable.orderId] to row[AzisabaAPI.StoreFulfillmentsTable.payload]
}
