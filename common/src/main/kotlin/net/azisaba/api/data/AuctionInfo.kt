package net.azisaba.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class AuctionInfo(
    val price: Long,
    @Serializable(with = UUIDSerializer::class)
    val seller: UUID,
    @SerialName("expires_at")
    val expiresAt: Long, // time until the auction expires
    @SerialName("time_till_data_removal")
    val timeTillDataRemoval: Long, // time until the data is removed from the file
    @SerialName("store_id")
    val storeId: Long, // unique at a time
    val biddable: Boolean,
    @SerialName("top_bidder")
    @Serializable(with = UUIDSerializer::class)
    val topBidder: UUID?, // null if biddable is false or no one bid
    @SerialName("item_bytes")
    val itemBytes: String, // base64 of item stack byte array
    @SerialName("item_name")
    val itemName: String,
    @SerialName("item_lore")
    val itemLore: String?,
)
