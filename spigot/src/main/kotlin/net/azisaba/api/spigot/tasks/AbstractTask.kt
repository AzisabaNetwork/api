package net.azisaba.api.spigot.tasks

import net.azisaba.api.spigot.SpigotPlugin
import org.bukkit.Bukkit

abstract class AbstractTask {
    abstract fun run()

    fun schedule() {
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.instance, this::run)
    }

    fun schedule(delay: Long) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(SpigotPlugin.instance, this::run, delay)
    }

    fun schedule(delay: Long, period: Long) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(SpigotPlugin.instance, this::run, delay, period)
    }
}
