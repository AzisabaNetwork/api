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
    @Serializable
    @Resource("products")
    data class Products(
        @Suppress("unused")
        val parent: Store,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val (products, saraProducts) = transaction(DatabaseManager.azisabaApi) {
                AzisabaAPI.Product.all().map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "description" to it.description,
                        "price" to it.price,
                        "image_url" to it.imageUrl,
                        "tags" to it.tags,
                        "hidden" to it.hidden,
                    )
                } to
                        AzisabaAPI.SaraProduct.all().map {
                            mapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "description" to it.description,
                                "price" to it.price,
                                "image_url" to it.imageUrl,
                                "hidden" to it.hidden,
                            )
                        }
            }
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
        companion object {
            private val ranks = listOf("100000", "50000", "10000", "5000", "2000", "1000", "500", "100")
        }

        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val uuid = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.Players.getIdByUsername(name) ?: UUID(0, 0)
            }
            val highestRank = transaction(DatabaseManager.luckPerms) {
                val groups = LuckPerms.UserPermissions.getGroupsForPlayer(uuid).map {
                    it.permission.substringAfter("group.").substringAfter("hide").substringBefore("yen")
                }
                if (groups.isEmpty()) return@transaction null
                groups.firstOrNull { it in ranks }?.toInt() ?: return@transaction 0
            }
            call.respondJson(mapOf("highest_sara" to highestRank))
        }
    }
}
