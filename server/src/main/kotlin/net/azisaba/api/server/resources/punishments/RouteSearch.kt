package net.azisaba.api.server.resources.punishments

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.resources.RequestHandler
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.schemas.SpicyAzisaBan
import net.azisaba.api.server.vector.MapVectorDatabase
import net.azisaba.api.server.vector.VectorUtil
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

@Serializable
@Resource("search")
data class RouteSearch(
    val parent: RouteRoot,
): RequestHandler() {
    companion object {
        private val vectorDatabaseFile = File("punishments.json")
        val vectorDatabase by lazy {
            if (vectorDatabaseFile.exists()) {
                Json.decodeFromString<MapVectorDatabase>(vectorDatabaseFile.readText())
            } else {
                MapVectorDatabase()
            }
        }
    }

    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val query = call.request.queryParameters["q"] ?: run {
            call.respondJson(mapOf("error" to "q parameter must be specified"), status = HttpStatusCode.BadRequest)
            return
        }
        val revertedOnly = call.request.queryParameters["revertedOnly"]?.toBoolean() ?: false
        val restrictServer = call.request.queryParameters["server"]
        val restrictType = call.request.queryParameters["type"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        call.request.queryParameters["test"]?.toLongOrNull()?.let { test ->
            val punishment = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.PunishmentHistory
                    .find { SpicyAzisaBan.PunishmentHistoryTable.id eq test }
                    .firstOrNull()
            }
            if (punishment == null) {
                call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
                return
            }
            vectorDatabase.openAI.embedding(query).let { embedding ->
                call.respondJson(
                    mapOf(
                        "query" to query,
                        "reason" to punishment.reason,
                        "similarity" to VectorUtil.cosineSimilarity(embedding, vectorDatabase.openAI.embedding(punishment.reason)),
                    )
                )
            }
            return
        }
        val punishments = transaction(DatabaseManager.spicyAzisaBan) {
            runBlocking {
                vectorDatabase.openAI.search(query, limit + offset) {
                    if (revertedOnly && it["reverted"] != "true") {
                        return@search false
                    }
                    if (restrictServer != null && it["server"] != restrictServer) {
                        return@search false
                    }
                    when (restrictType) {
                        "BAN" -> if (it["type"]?.contains("BAN") != true) return@search false
                        "MUTE" -> if (it["type"]?.contains("MUTE") != true) return@search false
                        "KICK" -> if (it["type"]?.contains("KICK") != true) return@search false
                        "WARNING" -> if (it["type"] != "WARNING") return@search false
                        "CAUTION" -> if (it["type"] != "CAUTION") return@search false
                        "NOTE" -> if (it["type"] != "NOTE") return@search false
                    }
                    true
                }
                    .map { (vector, _) -> SpicyAzisaBan.PunishmentHistory[vector.metadata["id"]!!.toLong()].toMap() }
                    .drop(offset)
            }
        }
        call.respondJson(punishments)
    }
}
