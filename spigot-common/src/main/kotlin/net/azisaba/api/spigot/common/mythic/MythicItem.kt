package net.azisaba.api.spigot.common.mythic

import net.azisaba.api.data.ServerItemData

interface MythicItem {
    fun getInternalName(): String

    fun toServerItemData(): ServerItemData
}
