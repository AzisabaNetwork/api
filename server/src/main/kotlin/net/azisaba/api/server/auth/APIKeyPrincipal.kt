package net.azisaba.api.server.auth

import io.ktor.server.auth.*
import java.util.UUID

data class APIKeyPrincipal(val player: UUID) : Principal
