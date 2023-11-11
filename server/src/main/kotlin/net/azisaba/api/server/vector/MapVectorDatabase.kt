package net.azisaba.api.server.vector

import kotlinx.serialization.Serializable

@Serializable
data class MapVectorDatabase(val vectors: MutableMap<String, Vector> = mutableMapOf()) : VectorDatabase() {
    override fun getVector(id: String): Vector? {
        return vectors[id]
    }

    override fun getVectors(): List<Vector> {
        return vectors.values.toList()
    }

    override fun query(query: DoubleArray, k: Int, filterMetadata: (metadata: Map<String, String>) -> Boolean): List<Pair<Vector, Double>> =
        vectors.values
            .asSequence()
            .filter { filterMetadata(it.metadata) }
            .map { it to VectorUtil.cosineSimilarity(query, it.values) }
            .sortedByDescending { it.first.metadata["id"]?.toLongOrNull() ?: 0 }
            .sortedByDescending { (_, similarity) -> similarity }
            .take(k)
            .toList()

    override fun insertVector(vector: Vector) {
        vectors[vector.id] = vector
    }

    override fun removeVector(id: String) {
        vectors.remove(id)
    }

//    fun clear() {
//        vectors.forEach { (id, _) -> index.remove(id) }
//        vectors.clear()
//    }
}
