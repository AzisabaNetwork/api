package net.azisaba.api.server.players

import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlayerServiceTest {
    private val playerId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `builds player response without exposing database entities`() {
        val repository = FakePlayerRepository().apply {
            usernames[playerId] = "Player"
            permissions += PlayerGroupPermission("group.default", "global", "global")
            permissions += PlayerGroupPermission("group.showhideblue", "global", "global")
            permissions += PlayerGroupPermission("group.admin", "life", "global")
            permissions += PlayerGroupPermission("group.rank3", "life", "global")
            economy = PlayerEconomy(1.5, 2.5)
        }

        val result = assertIs<PlayerResult.Success<Map<String, Any?>>>(
            PlayerService(repository).getPlayer(playerId)
        )
        assertEquals(playerId.toString(), result.value["uuid"])
        assertEquals("Player", result.value["name"])
        assertEquals(listOf("blueyen"), result.value["groups"])

        @Suppress("UNCHECKED_CAST")
        val servers = result.value["servers"] as Map<String, Map<String, Any>>
        assertEquals(true, servers.getValue("life")["admin"])
        assertEquals(3, servers.getValue("life")["rank"])
        assertEquals(4.0, servers.getValue("life")["balance"])
        assertEquals(42L, servers.getValue("life")["total_play_time"])
    }

    @Test
    fun `owner can read inventory without a privacy lookup`() {
        val repository = FakePlayerRepository().apply {
            inventory = StoredPlayerInventory("Player", "inventory", "armor", 2, 1, "true", "123")
        }
        val service = PlayerService(repository, ::JsonPrimitive)

        val result = assertIs<PlayerResult.Success<Map<String, Any?>>>(
            service.getInventory(playerId, playerId)
        )
        assertEquals(JsonPrimitive("inventory"), result.value["inventory"])
        assertEquals(0, repository.privacyLookups)
    }

    @Test
    fun `other player needs sharing enabled`() {
        val repository = FakePlayerRepository().apply {
            inventory = StoredPlayerInventory("Player", "inventory", "armor", 2, 1, "true", "123")
        }
        val service = PlayerService(repository, ::JsonPrimitive)

        val denied = assertIs<PlayerResult.Failure>(service.getInventory(playerId, otherId))
        assertEquals("data_sharing_disabled", denied.error)
        assertEquals(PlayerResultStatus.FORBIDDEN, denied.status)

        repository.shared = true
        assertIs<PlayerResult.Success<Map<String, Any?>>>(service.getInventory(playerId, otherId))
    }

    @Test
    fun `invalid stored nbt becomes an internal error`() {
        val repository = FakePlayerRepository().apply {
            inventory = StoredPlayerInventory("Player", "invalid", "armor", 2, 1, "true", "123")
        }
        val service = PlayerService(repository) { throw IllegalArgumentException("invalid") }

        val result = assertIs<PlayerResult.Failure>(service.getInventory(playerId, playerId))
        assertEquals("invalid_stored_player_data", result.error)
        assertEquals(PlayerResultStatus.INTERNAL_SERVER_ERROR, result.status)
    }

    private class FakePlayerRepository : PlayerRepository {
        val usernames = mutableMapOf<UUID, String>()
        val permissions = mutableListOf<PlayerGroupPermission>()
        var economy: PlayerEconomy? = null
        var inventory: StoredPlayerInventory? = null
        var enderChest: StoredPlayerEnderChest? = null
        var shared = false
        var privacyLookups = 0

        override fun findUsername(uuid: UUID) = usernames[uuid]

        override fun findUuid(username: String) = usernames.entries.firstOrNull { it.value == username }?.key

        override fun findPunishments(uuid: UUID) = emptyList<Map<String, Any?>>()

        override fun findGroupPermissions(uuid: UUID) = permissions

        override fun findLifeEconomy(uuid: UUID) = economy

        override fun findLifePlayTime(uuid: UUID) = 42L

        override fun isPlayerDataShared(uuid: UUID, data: SharedPlayerData): Boolean {
            privacyLookups++
            return shared
        }

        override fun findInventory(uuid: UUID) = inventory

        override fun findEnderChest(uuid: UUID) = enderChest
    }
}
