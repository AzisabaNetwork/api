package net.azisaba.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerItemData(
    val name: String,
    val material: String,
    @SerialName("display_name")
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    @SerialName("custom_model_data")
    val customModelData: Int? = null,
    val amount: Int = 1,
    val unbreakable: Boolean = false,
    @SerialName("hide_flags")
    val hideFlags: List<String> = emptyList(),
    val enchants: Map<String, Int> = emptyMap(),
    @SerialName("custom_nbt")
    val customNbt: Map<String, String> = emptyMap(),
)
