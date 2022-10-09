package net.azisaba.api.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ClassTypeSerializer : KSerializer<Class<*>> {
    override fun deserialize(decoder: Decoder): Class<*> = Class.forName(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Class<*>) = encoder.encodeString(value.typeName)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Class", PrimitiveKind.STRING)
}
