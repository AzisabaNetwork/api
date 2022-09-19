package net.azisaba.api.spigot.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.data.AuctionInfo
import net.azisaba.api.serializers.NullableUUIDSerializer
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class CAAuctionInfo(
    @SerialName("Price")
    val price: Long,
    @SerialName("Seller")
    @Serializable(with = UUIDSerializer::class)
    val seller: UUID,
    @SerialName("Time-Till-Expire")
    val timeTillExpire: Long,
    @SerialName("Full-Time")
    val timeTillDataRemoval: Long,
    @SerialName("StoreID")
    val storeId: Long,
    @SerialName("Biddable")
    val biddable: Boolean,
    @SerialName("TopBidder")
    @Serializable(with = NullableUUIDSerializer::class)
    val topBidder: UUID?,
    @SerialName("ItemBytes")
    val itemBytes: String,
) {
    fun toAuctionInfo(itemName: String, itemLore: String?): AuctionInfo {
        return AuctionInfo(
            price,
            seller,
            timeTillExpire,
            timeTillDataRemoval,
            storeId,
            biddable,
            topBidder,
            itemBytes,
            itemName,
            itemLore,
        )
    }
}
