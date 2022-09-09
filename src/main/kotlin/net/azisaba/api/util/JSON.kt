package net.azisaba.api.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.azisaba.api.serializers.DynamicLookupSerializer
import net.azisaba.api.serializers.UUIDSerializer
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    serializersModule = SerializersModule {
        contextual(Any::class, DynamicLookupSerializer)
        contextual(UUID::class, UUIDSerializer)
    }
}