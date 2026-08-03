package net.azisaba.api.data

import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@Serializable
sealed interface IProduct {
    val uuid: UUID
}

@Serializable
data class Product(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    val id: Long,
) : IProduct

@Serializable
data class SaraProduct(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    val amount: Int,
) : IProduct

// ---

@Serializable
data class PurchaseData(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val amount: Long,
)

@Serializable
data class StorePurchaseItemV2(
    val orderId: String,
    val deliveryId: String,
    val product: IProduct,
)

@Serializable
data class PurchaseAnnouncementV2(
    val orderId: String,
    val deliveryId: String,
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val amount: Long,
)
