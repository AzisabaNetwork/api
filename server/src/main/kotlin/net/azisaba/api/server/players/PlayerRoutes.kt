package net.azisaba.api.server.players

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authentication
import io.ktor.server.resources.get
import io.ktor.server.routing.Route
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.resources.RoutePlayers
import net.azisaba.api.server.resources.respondJson

fun Route.registerPlayerRoutes(service: PlayerService) {
    get<RoutePlayers.Me> {
        val caller = call.authentication.principal<APIKeyPrincipal>()?.player ?: return@get
        call.respond(service.getPlayer(caller))
    }
    get<RoutePlayers.Id> {
        call.respond(service.getPlayer(it.uuid))
    }
    get<RoutePlayers.ByName> {
        call.respond(service.getPlayerByName(it.name))
    }
    get<RoutePlayers.Id.Inventory> {
        call.respond(service.getInventory(it.parent.uuid, call.authenticatedPlayer()))
    }
    get<RoutePlayers.Id.EnderChest> {
        call.respond(service.getEnderChest(it.parent.uuid, call.authenticatedPlayer()))
    }
    get<RoutePlayers.ByName.Inventory> {
        call.respond(service.getInventoryByName(it.parent.name, call.authenticatedPlayer()))
    }
    get<RoutePlayers.ByName.EnderChest> {
        call.respond(service.getEnderChestByName(it.parent.name, call.authenticatedPlayer()))
    }
}

fun Route.registerPlayerPunishmentRoutes(service: PlayerService) {
    get<RoutePlayers.Id.Punishments> {
        call.respond(service.getPunishments(it.parent.uuid))
    }
    get<RoutePlayers.ByName.Punishments> {
        call.respond(service.getPunishmentsByName(it.parent.name))
    }
}

private fun ApplicationCall.authenticatedPlayer() =
    authentication.principal<APIKeyPrincipal>()?.player

private suspend fun ApplicationCall.respond(result: PlayerResult<*>) {
    when (result) {
        is PlayerResult.Success -> respondJson(result.value)
        is PlayerResult.Failure -> {
            if (result.logMessage != null && result.cause != null) {
                application.log.error(result.logMessage, result.cause)
            }
            respondJson(
                mapOf("error" to result.error),
                status = when (result.status) {
                    PlayerResultStatus.NOT_FOUND -> HttpStatusCode.NotFound
                    PlayerResultStatus.FORBIDDEN -> HttpStatusCode.Forbidden
                    PlayerResultStatus.INTERNAL_SERVER_ERROR -> HttpStatusCode.InternalServerError
                },
            )
        }
    }
}
