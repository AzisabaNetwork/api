package net.azisaba.api.server.resources

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.SpicyAzisaBan
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
@Resource("/punishments")
class Punishments {
    @Serializable
    @Resource("{id}")
    data class Id(
        @Suppress("unused")
        val parent: Punishments,
        val id: Long,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            if (id < 0) {
                call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
                return
            }
            val punishment = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.PunishmentHistory
                    .find(SpicyAzisaBan.PunishmentHistoryTable.id eq this@Id.id)
                    .firstOrNull()
            }
            if (punishment == null) {
                call.respondJson(mapOf("error" to "not found"), status = HttpStatusCode.NotFound)
                return
            }
            call.respondJson(punishment.toMap())
        }
    }

    @Serializable
    @Resource("search")
    data class Search(
        @Suppress("unused")
        val parent: Punishments,
    ): RequestHandler() {
        override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val query = call.request.queryParameters["q"] ?: run {
                call.respondJson(mapOf("error" to "q parameter must be specified"), status = HttpStatusCode.BadRequest)
                return
            }
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
            val punishments = transaction(DatabaseManager.spicyAzisaBan) {
                SpicyAzisaBan.PunishmentHistory
                    .find {
                        (SpicyAzisaBan.PunishmentHistoryTable.name like "%$query%") or
                            (SpicyAzisaBan.PunishmentHistoryTable.target eq query) or
                            (SpicyAzisaBan.PunishmentHistoryTable.reason like "%$query%")
                    }
                    .limit(500, offset)
                    .toList()
            }
            call.respondJson(punishments.map { it.toMap() })
        }
    }
}
