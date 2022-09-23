package net.azisaba.api.server.resources

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import net.azisaba.api.server.PlayerInfo
import net.azisaba.api.server.RedisManager

// map of games for /counts
private val games = mapOf(
    "life" to mapOf(
        "life" to "life|liferesource".toRegex(),
        "pve" to "lifetravel|lifeevent|lifepve.*".toRegex(),
        "other" to "life.*".toRegex(),
    ),
    "diverse" to "diverse(?!dev).*".toRegex(),
    "jg" to mapOf(
        "sclat" to "sclat.*".toRegex(),
        "jg" to "jg|jg1|jgdebug".toRegex(),
    ),
    "lgw" to "lgw.*".toRegex(),
    "pg" to "pg".toRegex(),
    "vanilife" to "vanilife.*".toRegex(),
    "despawn" to "despawn.*".toRegex(),
    "coretol" to "coretol.*".toRegex(),
    "test" to "test.*".toRegex(),
    "lobby" to "lobby.*".toRegex(),
    "afk" to "afk.*".toRegex(),
)

private const val showServers = false

@Serializable
@Resource("/counts")
object Counts : RequestHandler() {
    override suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
        val players = RedisManager.getPlayers()
        val nonNullPlayers = players.filter { it.childServer != null }
        val mappedGames = games.mapValues { (_, regexOrMap) ->
            when (regexOrMap) {
                is Regex -> {
                    val matchedPlayers = nonNullPlayers.filter { regexOrMap.matches(it.childServer!!) }
                    if (showServers) {
                        val serverList = matchedPlayers.map { it.childServer!! }.distinct()
                        val servers = mutableMapOf<String, Int>()
                        for (server in serverList) {
                            servers[server] = matchedPlayers.count { it.childServer == server }
                        }
                        mapOf(
                            "players" to matchedPlayers.size,
                            "servers" to servers,
                        )
                    } else {
                        mapOf("players" to matchedPlayers.size)
                    }
                }
                is Map<*, *> -> {
                    val totalMatchedPlayers = mutableListOf<PlayerInfo>()
                    var total = 0
                    val modes = mutableMapOf<Any?, Int>()
                    regexOrMap.forEach { (key, value) ->
                        if (value is Regex) {
                            val matchedPlayers = nonNullPlayers
                                .filter { !totalMatchedPlayers.contains(it) }
                                .filter { value.matches(it.childServer!!) }
                            totalMatchedPlayers.addAll(matchedPlayers)
                            modes[key] = matchedPlayers.size
                            total += matchedPlayers.size
                        } else {
                            throw IllegalArgumentException("Expected Regex")
                        }
                    }
                    if (showServers) {
                        val serverList = totalMatchedPlayers.map { it.childServer!! }.distinct()
                        val servers = mutableMapOf<String, Int>()
                        for (server in serverList) {
                            servers[server] = totalMatchedPlayers.count { it.childServer == server }
                        }
                        mapOf(
                            "players" to total,
                            "modes" to modes,
                            "servers" to servers,
                        )
                    } else {
                        mapOf(
                            "players" to total,
                            "modes" to modes,
                        )
                    }
                }
                else -> error("Invalid type: ${regexOrMap::class.java.typeName}")
            }
        }
        call.respondJson(
            mapOf(
                "total_players" to players.size,
                "games" to mappedGames,
            )
        )
    }
}
