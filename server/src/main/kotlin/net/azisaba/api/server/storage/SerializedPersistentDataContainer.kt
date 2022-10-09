package net.azisaba.api.server.storage

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import net.azisaba.api.server.util.Util
import net.azisaba.api.util.JSON

@Serializable
sealed interface ISerializedPersistentDataContainer {
    fun asPersistentDataContainer(): IPersistentDataContainer<*>
}

@Serializable
data class SerializedPersistentDataContainer(
    @SerialName("container_type")
    val type: String,
    val serializerType: String,
    val data: Map<String, String>,
) : ISerializedPersistentDataContainer {
    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    override fun asPersistentDataContainer(): PersistentDataContainer<Any> {
        val serializer = Class.forName(serializerType).kotlin.serializer() as kotlinx.serialization.KSerializer<Any>
        val newData = data.mapValues { (_, value) -> JSON.decodeFromString(serializer, value) }.toMutableMap()
        return PersistentDataContainer(
            type = Class.forName(type) as Class<Any>,
            data = newData,
        )
    }
}

@Serializable
data class SerializedPersistentListDataContainer(
    @SerialName("container_type")
    val type: String,
    val serializerType: String,
    val data: Map<String, List<String>>,
) : ISerializedPersistentDataContainer {
    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    override fun asPersistentDataContainer(): IPersistentDataContainer<Any> {
        val serializer = Class.forName(serializerType).kotlin.serializerOrNull()
            ?: Util.runNoinline { Class.forName(serializerType.substring(0, serializerType.length - "$\$serializer".length)).kotlin.serializer() }
            as kotlinx.serialization.KSerializer<Any>
        val newData = data.mapValues { (_, value) -> value.map { JSON.decodeFromString(serializer, it) } }.toMutableMap()
        return PersistentListDataContainer(
            type = List::class.java as Class<List<Any>>,
            trueType = Class.forName(type) as Class<Any>,
            data = newData,
        ) as IPersistentDataContainer<Any>
    }
}
