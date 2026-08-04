package net.azisaba.api.server.store

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.util.JSON
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

private const val PRODUCT_KIND = "product"
private const val SARA_KIND = "sara"

@Serializable
private data class StoreAdminListRequest(
    @SerialName("actor_user_id") val actorUserId: String,
    val kind: String,
    @SerialName("include_hidden") val includeHidden: Boolean = false,
)

@Serializable
private data class StoreAdminCreateRequest(
    @SerialName("actor_user_id") val actorUserId: String,
    val kind: String,
    val name: String,
    val description: String,
    val price: Int,
    @SerialName("image_url") val imageUrl: String,
    val tags: String? = null,
    @SerialName("stripe_id") val stripeId: String,
    val hidden: Boolean = true,
)

@Serializable
private data class StoreAdminUpdateRequest(
    @SerialName("actor_user_id") val actorUserId: String,
    val kind: String,
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val price: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val tags: String? = null,
    @SerialName("stripe_id") val stripeId: String? = null,
    val hidden: Boolean? = null,
)

@Serializable
private data class StoreAdminDeleteRequest(
    @SerialName("actor_user_id") val actorUserId: String,
    val kind: String,
    val id: Long,
    val confirm: Boolean,
)

@Serializable
data class StoreAdminProduct(
    val kind: String,
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    @SerialName("image_url") val imageUrl: String,
    val hidden: Boolean,
    val tags: String? = null,
    @SerialName("stripe_id") val stripeId: String,
)

@Serializable
private data class StoreAdminListResponse(val products: List<StoreAdminProduct>)

@Serializable
private data class StoreAdminProductResponse(val product: StoreAdminProduct)

fun Route.registerStoreAdminRoutes() {
    post("/internal/store/admin/products/list") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/admin/products/list")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val request = body.decodeOrNull<StoreAdminListRequest>()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        if (!request.isValidBase()) {
            return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        }
        val products = transaction(DatabaseManager.azisabaApi) {
            when (request.kind) {
                PRODUCT_KIND -> AzisabaAPI.Product.all()
                    .filter { request.includeHidden || !it.hidden }
                    .map(::productSnapshot)
                SARA_KIND -> AzisabaAPI.SaraProduct.all()
                    .filter { request.includeHidden || !it.hidden }
                    .map(::saraSnapshot)
                else -> emptyList()
            }.sortedBy(StoreAdminProduct::id)
        }
        call.respondSerialized(StoreAdminListResponse.serializer(), StoreAdminListResponse(products))
    }

    post("/internal/store/admin/products/create") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/admin/products/create")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val request = body.decodeOrNull<StoreAdminCreateRequest>()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        val validationError = request.validationError()
        if (validationError != null) {
            return@post call.respondJson(mapOf("error" to validationError), status = HttpStatusCode.BadRequest)
        }
        val created = transaction(DatabaseManager.azisabaApi) {
            val snapshot = when (request.kind) {
                PRODUCT_KIND -> productSnapshot(AzisabaAPI.Product.new {
                    name = request.name.trim()
                    description = request.description.trim()
                    price = request.price
                    imageUrl = request.imageUrl.trim()
                    tags = requireNotNull(request.tags).trim()
                    hidden = request.hidden
                    priceId = request.stripeId.trim()
                })
                SARA_KIND -> saraSnapshot(AzisabaAPI.SaraProduct.new {
                    name = request.name.trim()
                    description = request.description.trim()
                    price = request.price
                    imageUrl = request.imageUrl.trim()
                    hidden = request.hidden
                    productId = request.stripeId.trim()
                })
                else -> error("Unknown product kind")
            }
            audit(request.actorUserId, "create", snapshot.kind, snapshot.id, null, snapshot)
            snapshot
        }
        call.respondSerialized(
            StoreAdminProductResponse.serializer(),
            StoreAdminProductResponse(created),
            HttpStatusCode.Created,
        )
    }

    post("/internal/store/admin/products/update") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/admin/products/update")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val request = body.decodeOrNull<StoreAdminUpdateRequest>()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        val validationError = request.validationError()
        if (validationError != null) {
            return@post call.respondJson(mapOf("error" to validationError), status = HttpStatusCode.BadRequest)
        }
        val updated = transaction(DatabaseManager.azisabaApi) {
            when (request.kind) {
                PRODUCT_KIND -> AzisabaAPI.Product.findById(request.id)?.let { product ->
                    val before = productSnapshot(product)
                    request.name?.let { product.name = it.trim() }
                    request.description?.let { product.description = it.trim() }
                    request.price?.let { product.price = it }
                    request.imageUrl?.let { product.imageUrl = it.trim() }
                    request.tags?.let { product.tags = it.trim() }
                    request.stripeId?.let { product.priceId = it.trim() }
                    request.hidden?.let { product.hidden = it }
                    productSnapshot(product).also {
                        audit(request.actorUserId, "update", it.kind, it.id, before, it)
                    }
                }
                SARA_KIND -> AzisabaAPI.SaraProduct.findById(request.id)?.let { product ->
                    val before = saraSnapshot(product)
                    request.name?.let { product.name = it.trim() }
                    request.description?.let { product.description = it.trim() }
                    request.price?.let { product.price = it }
                    request.imageUrl?.let { product.imageUrl = it.trim() }
                    request.stripeId?.let { product.productId = it.trim() }
                    request.hidden?.let { product.hidden = it }
                    saraSnapshot(product).also {
                        audit(request.actorUserId, "update", it.kind, it.id, before, it)
                    }
                }
                else -> null
            }
        } ?: return@post call.respondJson(mapOf("error" to "product_not_found"), status = HttpStatusCode.NotFound)
        call.respondSerialized(StoreAdminProductResponse.serializer(), StoreAdminProductResponse(updated))
    }

    post("/internal/store/admin/products/delete") {
        val body = call.receiveText()
        if (!StoreRequestAuthenticator.verify(call.request, body, "/internal/store/admin/products/delete")) {
            return@post call.respondJson(mapOf("error" to "unauthorized"), status = HttpStatusCode.Unauthorized)
        }
        val request = body.decodeOrNull<StoreAdminDeleteRequest>()
            ?: return@post call.respondJson(mapOf("error" to "invalid_request"), status = HttpStatusCode.BadRequest)
        if (!request.isValidBase() || request.id <= 0 || !request.confirm) {
            return@post call.respondJson(mapOf("error" to "delete_confirmation_required"), status = HttpStatusCode.BadRequest)
        }
        val deleted = transaction(DatabaseManager.azisabaApi) {
            when (request.kind) {
                PRODUCT_KIND -> AzisabaAPI.Product.findById(request.id)?.let { product ->
                    productSnapshot(product).also {
                        product.delete()
                        audit(request.actorUserId, "delete", it.kind, it.id, it, null)
                    }
                }
                SARA_KIND -> AzisabaAPI.SaraProduct.findById(request.id)?.let { product ->
                    saraSnapshot(product).also {
                        product.delete()
                        audit(request.actorUserId, "delete", it.kind, it.id, it, null)
                    }
                }
                else -> null
            }
        } ?: return@post call.respondJson(mapOf("error" to "product_not_found"), status = HttpStatusCode.NotFound)
        call.respondSerialized(StoreAdminProductResponse.serializer(), StoreAdminProductResponse(deleted))
    }
}

private fun StoreAdminListRequest.isValidBase() = actorUserId.matches(Regex("^[0-9]{5,32}$")) && kind in setOf(PRODUCT_KIND, SARA_KIND)

private fun StoreAdminDeleteRequest.isValidBase() = actorUserId.matches(Regex("^[0-9]{5,32}$")) && kind in setOf(PRODUCT_KIND, SARA_KIND)

private fun StoreAdminCreateRequest.validationError(): String? {
    if (!actorUserId.matches(Regex("^[0-9]{5,32}$")) || kind !in setOf(PRODUCT_KIND, SARA_KIND)) return "invalid_request"
    if (name.isBlank() || name.length > 128) return "invalid_name"
    if (description.isBlank()) return "invalid_description"
    if (price < 0) return "invalid_price"
    if (imageUrl.length > 1000 || (!imageUrl.startsWith("https://") && !imageUrl.startsWith("http://"))) return "invalid_image_url"
    if (stripeId.isBlank() || stripeId.length > 128) return "invalid_stripe_id"
    if (kind == PRODUCT_KIND && (tags == null || tags.length > 1000)) return "invalid_tags"
    if (kind == SARA_KIND && tags != null) return "invalid_tags"
    return null
}

private fun StoreAdminUpdateRequest.validationError(): String? {
    if (!actorUserId.matches(Regex("^[0-9]{5,32}$")) || kind !in setOf(PRODUCT_KIND, SARA_KIND) || id <= 0) return "invalid_request"
    if (listOf(name, description, imageUrl, tags, stripeId, hidden, price).all { it == null }) return "no_changes"
    if (name != null && (name.isBlank() || name.length > 128)) return "invalid_name"
    if (description != null && description.isBlank()) return "invalid_description"
    if (price != null && price < 0) return "invalid_price"
    if (imageUrl != null && (imageUrl.length > 1000 || (!imageUrl.startsWith("https://") && !imageUrl.startsWith("http://")))) return "invalid_image_url"
    if (stripeId != null && (stripeId.isBlank() || stripeId.length > 128)) return "invalid_stripe_id"
    if (kind == SARA_KIND && tags != null) return "invalid_tags"
    if (tags != null && tags.length > 1000) return "invalid_tags"
    return null
}

private fun productSnapshot(product: AzisabaAPI.Product) = StoreAdminProduct(
    kind = PRODUCT_KIND,
    id = product.id.value,
    name = product.name,
    description = product.description,
    price = product.price,
    imageUrl = product.imageUrl,
    hidden = product.hidden,
    tags = product.tags,
    stripeId = product.priceId,
)

private fun saraSnapshot(product: AzisabaAPI.SaraProduct) = StoreAdminProduct(
    kind = SARA_KIND,
    id = product.id.value,
    name = product.name,
    description = product.description,
    price = product.price,
    imageUrl = product.imageUrl,
    hidden = product.hidden,
    stripeId = product.productId,
)

private fun audit(
    actorUserId: String,
    action: String,
    kind: String,
    productId: Long,
    before: StoreAdminProduct?,
    after: StoreAdminProduct?,
) {
    AzisabaAPI.StoreAdminAuditTable.insert {
        it[AzisabaAPI.StoreAdminAuditTable.actorUserId] = actorUserId
        it[AzisabaAPI.StoreAdminAuditTable.action] = action
        it[productKind] = kind
        it[AzisabaAPI.StoreAdminAuditTable.productId] = productId
        it[beforeJson] = before?.let { value -> JSON.encodeToString(StoreAdminProduct.serializer(), value) }
        it[afterJson] = after?.let { value -> JSON.encodeToString(StoreAdminProduct.serializer(), value) }
        it[createdAt] = System.currentTimeMillis() / 1000
    }
}

private suspend fun <T> ApplicationCall.respondSerialized(
    serializer: KSerializer<T>,
    value: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respondText(JSON.encodeToString(serializer, value), ContentType.Application.Json, status)

private inline fun <reified T> String.decodeOrNull() = runCatching { JSON.decodeFromString<T>(this) }.getOrNull()
