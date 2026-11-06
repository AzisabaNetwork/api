package net.azisaba.api.spigot.common.mythic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MythicAPITest {
    @Test
    fun `maps MythicMobs 4_12 to v4_12`() {
        assertEquals("v4_12", MythicAPI.getImplVersion("4.12.0"))
    }

    @Test
    fun `maps MythicMobs 5_12 and later 5_x releases to v5_12`() {
        assertEquals("v5_12", MythicAPI.getImplVersion("5.12.0-SNAPSHOT"))
        assertEquals("v5_12", MythicAPI.getImplVersion("5.13.0"))
        assertEquals("v5_12", MythicAPI.getImplVersion("5.14.1-SNAPSHOT-fb9fa74d"))
    }

    @Test
    fun `rejects unsupported versions`() {
        listOf("4.11.0", "5.11.2", "6.0.0", "dev").forEach { version ->
            assertFailsWith<UnsupportedOperationException> { MythicAPI.getImplVersion(version) }
        }
    }
}
