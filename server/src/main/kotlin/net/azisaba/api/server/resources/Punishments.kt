package net.azisaba.api.server.resources

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.schemas.SpicyAzisaBan
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
}
