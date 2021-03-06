package net.azisaba.api.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import net.azisaba.api.schemas.AzisabaAPI
import net.azisaba.api.server.DatabaseManager
import net.azisaba.api.server.util.Util
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureSecurity() {
    authentication {
        register(APIKeyAuthenticationProvider.create("api-key") {
            skipWhen { java.lang.Boolean.getBoolean("net.azisaba.api.skipAuth") }
        })
    }
}

class APIKeyAuthenticationProvider(config: Config) : AuthenticationProvider(config) {
    companion object {
        fun create(name: String, configure: Config.() -> Unit): APIKeyAuthenticationProvider {
            return APIKeyAuthenticationProvider(Config(name).apply(configure))
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val authorization = call.request.header("Authorization")
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            context.challenge("APIKey", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                call.respond(UnauthorizedResponse())
                challenge.complete()
            }
            return
        }
        val token = authorization.removePrefix("Bearer ")
        transaction(DatabaseManager.azisabaApi) {
            val key = keyAuthenticator(token)
            if (key == null) {
                context.challenge("APIKey", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                    call.respond(UnauthorizedResponse())
                    challenge.complete()
                }
                return@transaction
            }
            key.uses++
            context.principal = UserIdPrincipal(key.player)
        }
    }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name)
}

private val keyAuthenticator: (String) -> AzisabaAPI.APIKey? = Util.memoize(1000 * 60) { key ->
    AzisabaAPI.APIKey.find(AzisabaAPI.APIKeyTable.key eq key).firstOrNull()
}
