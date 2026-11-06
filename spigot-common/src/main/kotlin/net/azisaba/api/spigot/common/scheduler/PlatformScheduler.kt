package net.azisaba.api.spigot.common.scheduler

import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

/**
 * Schedules tasks on the correct thread for both Bukkit/Paper and Folia.
 *
 * On Folia, [runGlobal] runs on the global region thread and [runForEntity] runs on the thread owning the entity.
 * On Bukkit/Paper, both run on the main server thread.
 */
interface PlatformScheduler {
    companion object {
        private const val FOLIA_CLASS_NAME = "io.papermc.paper.threadedregions.RegionizedServer"
        private const val FOLIA_IMPL_CLASS_NAME = "net.azisaba.api.spigot.folia.FoliaPlatformScheduler"

        fun isFolia(): Boolean =
            try {
                Class.forName(FOLIA_CLASS_NAME)
                true
            } catch (_: ClassNotFoundException) {
                false
            }

        fun ticksToMillis(ticks: Long): Long = ticks * 50L

        fun create(plugin: Plugin): PlatformScheduler =
            if (isFolia()) {
                Class.forName(FOLIA_IMPL_CLASS_NAME)
                    .getConstructor(Plugin::class.java)
                    .newInstance(plugin) as PlatformScheduler
            } else {
                BukkitPlatformScheduler(plugin)
            }
    }

    fun runAsync(task: Runnable)

    fun runAsyncLater(delayTicks: Long, task: Runnable)

    fun runAsyncTimer(delayTicks: Long, periodTicks: Long, task: Runnable)

    fun runGlobal(task: Runnable)

    fun runGlobalLater(delayTicks: Long, task: Runnable)

    /**
     * Runs [task] on the thread owning [entity]. The task is silently dropped if the entity is removed before it runs.
     */
    fun runForEntity(entity: Entity, task: Runnable)

    fun cancelTasks()
}
