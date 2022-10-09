package net.azisaba.api.server.storage

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.azisaba.api.serializers.ClassTypeSerializer
import net.azisaba.api.util.JSON
import kotlin.reflect.KProperty

interface IPersistentDataContainer<T : Any> {
    val type: Class<T>
    val data: MutableMap<String, T>

    operator fun set(key: String, value: T) {
        data[key] = value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        data[property.name] = value
    }

    operator fun get(key: String): T? =
        data[key]

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? =
        data[property.name]

    fun asSerialized(): ISerializedPersistentDataContainer
}

@Serializable
data class PersistentDataContainer<T : Any>(
    @Serializable(with = ClassTypeSerializer::class)
    override val type: Class<T>,
    override val data: MutableMap<String, T> = mutableMapOf(),
) : IPersistentDataContainer<T> {
    @OptIn(InternalSerializationApi::class)
    override fun asSerialized(): SerializedPersistentDataContainer {
        val serializer = type.kotlin.serializer()
        return SerializedPersistentDataContainer(
            type = type.name,
            serializerType = serializer.javaClass.typeName,
            data = data.mapValues { (_, value) -> JSON.encodeToString(serializer, value) },
        )
    }
}

@Serializable
data class PersistentListDataContainer<T : Any>(
    @Serializable(with = ClassTypeSerializer::class)
    override val type: Class<List<T>>,
    @Serializable(with = ClassTypeSerializer::class)
    val trueType: Class<T>,
    override val data: MutableMap<String, List<T>> = mutableMapOf(),
) : IPersistentDataContainer<List<T>> {
    @OptIn(InternalSerializationApi::class)
    override fun asSerialized(): SerializedPersistentListDataContainer {
        val serializer = trueType.kotlin.serializer()
        return SerializedPersistentListDataContainer(
            type = trueType.name,
            serializerType = serializer.javaClass.typeName,
            data = data.mapValues { (_, value) -> value.map { JSON.encodeToString(serializer, it) } },
        )
    }
}
