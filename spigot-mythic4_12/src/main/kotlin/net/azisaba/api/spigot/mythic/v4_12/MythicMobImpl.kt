package net.azisaba.api.spigot.mythic.v4_12

import net.azisaba.api.spigot.common.mythic.MythicMob
import io.lumine.xikage.mythicmobs.mobs.MythicMob as MMythicMob

data class MythicMobImpl(val handle: MMythicMob): MythicMob {
    override fun getTypeName(): String = handle.internalName

    override fun getDisplayName(): String = handle.displayName.get()
}
