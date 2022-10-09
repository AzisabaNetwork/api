package net.azisaba.api.server

import java.util.Timer
import kotlin.concurrent.schedule

object TaskScheduler : Timer("Async Task Scheduler", true) {
    init {
        // cache auctions every day
        schedule(1000 * 60, 1000 * 60 * 60 * 24) {
            RedisManager.getAuctions()
        }
    }
}
