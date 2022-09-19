package net.azisaba.api.spigot.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CAData(
    @SerialName("Items")
    val items: Map<String, CAAuctionInfo>,
)
