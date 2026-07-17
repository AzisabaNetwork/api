package net.azisaba.api.server.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@Serializable
@Resource("/players")
class RoutePlayers {
    @Serializable
    @Resource("{uuid}")
    data class Id(
        @Suppress("unused")
        val parent: RoutePlayers,
        @Serializable(with = UUIDSerializer::class)
        val uuid: UUID,
    ) {
        @Serializable
        @Resource("punishments")
        data class Punishments(val parent: Id)

        @Serializable
        @Resource("inventory")
        data class Inventory(val parent: Id)

        @Serializable
        @Resource("enderchest")
        data class EnderChest(val parent: Id)
    }

    @Serializable
    @Resource("by-name/{name}")
    data class ByName(
        @Suppress("unused")
        val parent: RoutePlayers,
        val name: String,
    ) {
        @Serializable
        @Resource("punishments")
        data class Punishments(val parent: ByName)

        @Serializable
        @Resource("inventory")
        data class Inventory(val parent: ByName)

        @Serializable
        @Resource("enderchest")
        data class EnderChest(val parent: ByName)
    }

    @Serializable
    @Resource("me")
    data class Me(
        @Suppress("unused")
        val parent: RoutePlayers,
    )
}
