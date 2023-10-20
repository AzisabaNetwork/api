package net.azisaba.api.server.resources

import io.ktor.server.websocket.*

abstract class WebSocketRequestHandler {
    abstract suspend fun DefaultWebSocketServerSession.handleRequest()
}

suspend fun DefaultWebSocketServerSession.handle(handler: WebSocketRequestHandler) {
    with(handler) {
        handleRequest()
    }
}
