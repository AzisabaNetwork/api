package net.azisaba.api.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.auth.APIKeyPrincipal
import net.azisaba.api.server.resources.respondJson
import net.azisaba.api.server.schemas.AzisabaAPI
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Timer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

fun Application.configureSecurity() {
    Timer().schedule(1000 * 60, 1000 * 60) {
        rateLimits.clear()
    }
    authentication {
        register(APIKeyAuthenticationProvider.create("api-key") {
            skipWhen { java.lang.Boolean.getBoolean("net.azisaba.api.skipAuth") }
        })
        register(GroupAuthenticationProvider.create("punishments") {
            group("punish-manager")
            group("punish-expert")
            group("switchalladmin")
            group("alladmin")
            group("switchalladminmember")
            group("alladminmember")
            group("switchdeveloper")
            group("developer")
            group("switchdevelopermember")
            group("developermember")
        })
    }
}

// TODO: implement rate limit with redis
val rateLimits = ConcurrentHashMap<String, Long>()
const val MAX_REQUESTS_PER_MINUTE = 120

class APIKeyAuthenticationProvider(config: Config) : AuthenticationProvider(config) {
    companion object {
        fun create(name: String, configure: Config.() -> Unit): APIKeyAuthenticationProvider {
            return APIKeyAuthenticationProvider(Config(name).apply(configure))
        }

        private fun handleAnonymousCaller(context: AuthenticationContext) {
            context.challenge("APIKey", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                context.call.respond(UnauthorizedResponse())
                challenge.complete()
            }
        }

        internal fun tryAuth(context: AuthenticationContext): APIKeyPrincipal? {
            val call = context.call
            val authorization = call.request.header("Authorization")
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                handleAnonymousCaller(context)
                return null
            }
            val token = authorization.removePrefix("Bearer ")
            return transaction(DatabaseManager.azisabaApi) {
                val key = keyAuthenticator(token) ?: run {
                    handleAnonymousCaller(context)
                    return@transaction null
                }
                val current = rateLimits.getOrDefault(key.key, 0)
                if (current > MAX_REQUESTS_PER_MINUTE) {
                    context.challenge("APIKey", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                        call.respondJson(
                            mapOf("error" to "too many requests; please wait a bit and try again"),
                            status = HttpStatusCode.TooManyRequests,
                        )
                        challenge.complete()
                    }
                    return@transaction null
                }
                rateLimits[key.key] = current + 1
                //key.uses++
                return@transaction APIKeyPrincipal(UUID.fromString(key.player))
            }
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal = tryAuth(context)
    }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name)
}

private val keyAuthenticator: (String) -> AzisabaAPI.APIKey? = Util.memoize(1000 * 60) { key ->
    AzisabaAPI.APIKey.find(AzisabaAPI.APIKeyTable.key eq key).firstOrNull()
}

// guard /punishments/**

class GroupAuthenticationProvider(private val config: Config) : AuthenticationProvider(config) {
    companion object {
        fun create(name: String, configure: Config.() -> Unit): GroupAuthenticationProvider {
            return GroupAuthenticationProvider(Config(name).apply(configure))
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val principal = APIKeyAuthenticationProvider.tryAuth(context) ?: return
        if (!LuckPerms.UserPermissions.hasAnyGroup(principal.player, *config.groups.toTypedArray())) {
            context.challenge("Group", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                context.call.respondJson(
                    mapOf("error" to "You don't have permission to access this resource."),
                    status = HttpStatusCode.Forbidden,
                )
                challenge.complete()
            }
        }
    }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name) {
        internal val groups = mutableListOf<String>()

        fun group(name: String) {
            groups.add(name)
        }
    }
}
