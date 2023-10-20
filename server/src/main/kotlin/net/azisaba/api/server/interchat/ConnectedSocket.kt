package net.azisaba.api.server.interchat

import io.ktor.websocket.*
import java.util.UUID

data class ConnectedSocket(var uuid: UUID?, var server: String, val connection: DefaultWebSocketSession)
