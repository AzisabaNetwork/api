package net.azisaba.api.server.interchat.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

@SerialName("feedback")
@Serializable
data class OutgoingFeedbackPacket(val json: String) : OutgoingPacket {
    constructor(component: Component) : this(GsonComponentSerializer.gson().serialize(component))

    companion object {
        fun text(value: String) = OutgoingFeedbackPacket(Component.text(value))
    }
}
