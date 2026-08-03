package net.azisaba.api.server.resources

import io.ktor.resources.Resource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.schemas.SpicyAzisaBan
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

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
            val uuid = SpicyAzisaBan.Players.getIdByUsername(name) ?: UUID(0, 0)
            return transaction(DatabaseManager.luckPerms) {
                val groups = LuckPerms.UserPermissions.getGroupsForPlayer(uuid).map {
                    it.permission.substringAfter("group.").substringAfter("hide").substringBefore("yen")
                }
                if (groups.isEmpty()) return@transaction null
                groups.firstOrNull { it in ranks }?.toInt() ?: return@transaction 0
            }
        }

        fun hasGamingSara(name: String): Boolean {
            val uuid = SpicyAzisaBan.Players.getIdByUsername(name) ?: UUID(0, 0)
            return transaction(DatabaseManager.luckPerms) {
                val groups = LuckPerms.UserPermissions.getGroupsForPlayer(uuid).map {
                    it.permission.substringAfter("group.").substringAfter("hide")
                }
                if (groups.isEmpty()) return@transaction false
                return@transaction groups.any { it == "gamingsara" || it == "changegamingsara" }
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
    @Resource("players/{name}/highest_sara")
    data class HighestSara(
        @Suppress("unused")
        val parent: Store,
        val name: String,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            call.respondJson(mapOf("highest_sara" to getHighestSara(name), "gaming_sara" to hasGamingSara(name)))
        }
    }
}
