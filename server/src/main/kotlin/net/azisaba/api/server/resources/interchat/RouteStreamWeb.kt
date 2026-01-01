@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.api.server.resources.interchat

import io.ktor.resources.Resource
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import net.azisaba.api.server.interchat.ConnectedSocket
import net.azisaba.api.server.interchat.InterChatPacketListener
import net.azisaba.api.server.resources.WebSocketRequestHandler
import net.azisaba.api.util.JSON

@Resource("/interchat/stream/web")
class RouteStreamWeb : WebSocketRequestHandler() {
    override suspend fun DefaultWebSocketServerSession.handleRequest() {
        val server = call.request.queryParameters["server"] ?: run {
            return close(CloseReason(CloseReason.Codes.GOING_AWAY, "server parameter must be specified"))
        }
        val conn = ConnectedSocket(null, server, this, plainText = true)
        InterChatPacketListener.sockets += conn
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val packet = JSON.decodeFromString<RouteStream.Packet>(frame.readText())
                if (conn.uuid == null && packet !is RouteStream.AuthPacket) continue
                packet.handle(conn)
            }
        } finally {
            InterChatPacketListener.sockets -= conn
        }
    }
}
