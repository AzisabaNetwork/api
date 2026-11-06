package net.azisaba.api.spigot.tasks

import net.azisaba.api.spigot.SpigotPlugin

abstract class AbstractTask {
    abstract fun run()

    fun schedule() {
        SpigotPlugin.instance.scheduler.runAsync(this::run)
    }

    fun schedule(delay: Long) {
        SpigotPlugin.instance.scheduler.runAsyncLater(delay, this::run)
    }

    fun schedule(delay: Long, period: Long) {
        SpigotPlugin.instance.scheduler.runAsyncTimer(delay, period, this::run)
    }
}
