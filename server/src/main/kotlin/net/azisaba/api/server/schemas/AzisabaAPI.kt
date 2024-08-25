package net.azisaba.api.server.schemas

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object AzisabaAPI {
    object APIKeyTable : LongIdTable("api_keys") {
        val key = varchar("key", 64)
        val player = varchar("player", 36) // uuid
        val createdAt = long("created_at")
        val uses = long("uses").default(0L)

        init {
            uniqueIndex("player_key", key, player)
        }
    }

    class APIKey(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<APIKey>(APIKeyTable)

        var key by APIKeyTable.key
        var player by APIKeyTable.player
        var createdAt by APIKeyTable.createdAt
        var uses by APIKeyTable.uses
    }

    object ProductsTable : LongIdTable("products") {
        val name = varchar("name", 128)
        val description = text("description")
        val price = integer("price")
        val imageUrl = varchar("image_url", 1000)
        val tags = varchar("tags", 1000)
        val hidden = bool("hidden")
        val priceId = varchar("price_id", 128)
    }

    class Product(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Product>(ProductsTable)

        var name by ProductsTable.name
        var description by ProductsTable.description
        var price by ProductsTable.price
        var imageUrl by ProductsTable.imageUrl
        var tags by ProductsTable.tags
        var hidden by ProductsTable.hidden
        var priceId by ProductsTable.priceId
    }

    object SaraProductsTable : LongIdTable("sara_products") {
        val name = varchar("name", 128)
        val description = text("description")
        val price = integer("price")
        val imageUrl = varchar("image_url", 1000)
        val hidden = bool("hidden")
        val productId = varchar("product_id", 128)
    }

    class SaraProduct(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<SaraProduct>(SaraProductsTable)

        var name by SaraProductsTable.name
        var description by SaraProductsTable.description
        var price by SaraProductsTable.price
        var imageUrl by SaraProductsTable.imageUrl
        var hidden by SaraProductsTable.hidden
        var productId by SaraProductsTable.productId
    }
}
