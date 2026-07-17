package net.azisaba.api.server.resources

import net.azisaba.api.server.players.canAccessPlayerData
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerDataAccessTest {
    private val owner = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `owner can always access own data`() {
        assertTrue(canAccessPlayerData(owner, owner, false))
        assertTrue(canAccessPlayerData(owner, owner, null))
    }

    @Test
    fun `other player needs explicit opt in`() {
        assertTrue(canAccessPlayerData(other, owner, true))
        assertFalse(canAccessPlayerData(other, owner, false))
        assertFalse(canAccessPlayerData(other, owner, null))
    }
}
