package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

@SerialName("component")
@Serializable
data class OutgoingComponentPacket(val message: String) : OutgoingPacket {
    constructor(component: Component) : this(GsonComponentSerializer.gson().serialize(component))
}
