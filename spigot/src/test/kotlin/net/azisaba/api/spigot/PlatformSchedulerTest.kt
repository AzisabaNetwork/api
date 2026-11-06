package net.azisaba.api.spigot

import net.azisaba.api.spigot.common.scheduler.PlatformScheduler
import net.azisaba.api.spigot.folia.FoliaPlatformScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PlatformSchedulerTest {
    @Test
    fun `does not detect Folia without Folia classes`() {
        assertFalse(PlatformScheduler.isFolia())
    }

    @Test
    fun `converts ticks to milliseconds`() {
        assertEquals(50L, PlatformScheduler.ticksToMillis(1))
        assertEquals(60_000L, PlatformScheduler.ticksToMillis(20 * 60))
    }

    @Test
    fun `Folia implementation class name matches reflective lookup`() {
        assertEquals(
            "net.azisaba.api.spigot.folia.FoliaPlatformScheduler",
            FoliaPlatformScheduler::class.java.name,
        )
    }
}
