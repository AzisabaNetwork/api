package net.azisaba.api.server.util.hypixel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HypixelPlayerData(
    val uuid: String,
    val firstLogin: Long,
    @SerialName("playername")
    val playerName: String,
    @SerialName("displayname")
    val displayName: String,
    val prefix: String? = null,
    val rank: String? = null,
    val newPackageRank: String = "NONE",
    val packageRank: String = "NONE",
    val rankPlusColor: String = "RED",
    val monthlyPackageRank: String = "NONE",
    val monthlyRankColor: String = "GOLD",
) {
    fun getRank(): HypixelRank {
        if (rank != null) {
            return HypixelRank.valueOf(rank)
        }
        if (monthlyPackageRank == "SUPERSTAR") {
            return HypixelRank.SUPERSTAR
        }
        if (newPackageRank != "NONE" && newPackageRank != "NORMAL") {
            return HypixelRank.valueOf(newPackageRank)
        }
        return HypixelRank.valueOf(packageRank)
    }

    @JvmName("getRealPrefix")
    fun getPrefix() = prefix?.let { "$it " } ?: getRank().prefix(monthlyRankColor, rankPlusColor)
}
