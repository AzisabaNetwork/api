package net.azisaba.api.spigot.folia

import net.azisaba.api.spigot.common.scheduler.PlatformScheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

class FoliaPlatformScheduler(private val plugin: Plugin) : PlatformScheduler {
    override fun runAsync(task: Runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin) { task.run() }
    }

    override fun runAsyncLater(delayTicks: Long, task: Runnable) {
        Bukkit.getAsyncScheduler().runDelayed(
            plugin,
            { task.run() },
            PlatformScheduler.ticksToMillis(delayTicks.coerceAtLeast(0)),
            TimeUnit.MILLISECONDS,
        )
    }

    override fun runAsyncTimer(delayTicks: Long, periodTicks: Long, task: Runnable) {
        Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { task.run() },
            PlatformScheduler.ticksToMillis(delayTicks.coerceAtLeast(0)),
            PlatformScheduler.ticksToMillis(periodTicks.coerceAtLeast(1)),
            TimeUnit.MILLISECONDS,
        )
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task)
    }

    override fun runGlobalLater(delayTicks: Long, task: Runnable) {
        // Folia rejects delays <= 0
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { task.run() }, delayTicks.coerceAtLeast(1))
    }

    override fun runForEntity(entity: Entity, task: Runnable) {
        entity.scheduler.execute(plugin, task, null, 1L)
    }

    override fun cancelTasks() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin)
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
    }
}
