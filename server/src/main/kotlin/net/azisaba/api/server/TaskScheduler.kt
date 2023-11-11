package net.azisaba.api.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.azisaba.api.Logger
import net.azisaba.api.server.resources.Punishments
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.server.vector.RawTextVector
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object TaskScheduler : Timer("Async Task Scheduler", true) {
    init {
        // cache auctions every day
        schedule(1000 * 60, 1000 * 60 * 60 * 24) {
            try {
                RedisManager.getAuctions()
            } catch (e: Exception) {
                Logger.currentLogger.error("Error fetching auctions", e)
            }
        }

        schedule(1000 * 3, 1000 * 60 * 10) {
            try {
                runBlocking {
                    val list = transaction(DatabaseManager.spicyAzisaBan) {
                        SpicyAzisaBan.PunishmentHistory
                            .find { SpicyAzisaBan.PunishmentHistoryTable.id notInList Punishments.Search.vectorDatabase.vectors.keys.map { it.toLong() } }
                            .toList()
                            .map {
                                RawTextVector(
                                    it.id.value.toString(),
                                    it.reason,
                                    mapOf(
                                        "id" to it.id.value.toString(),
                                        "type" to it.type,
                                        "start" to it.start.toString(),
                                        "end" to it.end.toString(),
                                        "server" to it.server,
                                        "reverted" to (SpicyAzisaBan.Unpunish.findCachedByPunishId(it.id.value).firstOrNull() != null).toString(),
                                    ),
                                )
                            }
                    }
                    var totalProcessed = 0
                    list.chunked(500).forEach { historyList ->
                        val added = Punishments.Search.vectorDatabase.openAI.insertBulk(historyList)
                        totalProcessed += historyList.size
                        Logger.currentLogger.info(
                            "Processed $totalProcessed/${list.size} vectors (added ${added.size}/${historyList.size} vectors)"
                        )
                    }
                    File("punishments.json").writeText(Json.encodeToString(Punishments.Search.vectorDatabase))
                    Logger.currentLogger.info("Saved punishments.json")
                }
            } catch (e: Exception) {
                Logger.currentLogger.error("Error fetching punishments", e)
            }
        }
    }
}

fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}
