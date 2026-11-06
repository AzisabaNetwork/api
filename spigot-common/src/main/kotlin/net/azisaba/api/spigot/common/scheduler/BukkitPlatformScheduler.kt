package net.azisaba.api.spigot.common.scheduler

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

class BukkitPlatformScheduler(private val plugin: Plugin) : PlatformScheduler {
    override fun runAsync(task: Runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }

    override fun runAsyncLater(delayTicks: Long, task: Runnable) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks)
    }

    override fun runAsyncTimer(delayTicks: Long, periodTicks: Long, task: Runnable) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks)
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runGlobalLater(delayTicks: Long, task: Runnable) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
    }

    override fun runForEntity(entity: Entity, task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (entity.isValid) task.run()
        })
    }

    override fun cancelTasks() {
        Bukkit.getScheduler().cancelTasks(plugin)
    }
}
