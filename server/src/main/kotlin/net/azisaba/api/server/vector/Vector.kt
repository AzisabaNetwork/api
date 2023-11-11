package net.azisaba.api.server.vector

import com.github.jelmerk.knn.Item
import kotlinx.serialization.Serializable

@Serializable
data class Vector(
    val id: String,
    val values: DoubleArray,
    val metadata: Map<String, String> = emptyMap(),
) : Item<String, DoubleArray> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector) return false

        if (id != other.id) return false
        if (!values.contentEquals(other.values)) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun id(): String = id

    override fun vector(): DoubleArray = values

    override fun dimensions(): Int = values.size
}
