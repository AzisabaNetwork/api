package net.azisaba.api.server.vector

@Suppress("LeakingThis")
abstract class VectorDatabase {
    val openAI = VectorOpenAI(this)

    abstract fun getVector(id: String): Vector?
    abstract fun getVectors(): List<Vector>
    abstract fun query(query: DoubleArray, k: Int, filterMetadata: (metadata: Map<String, String>) -> Boolean = { true }): List<Pair<Vector, Double>>
    abstract fun insertVector(vector: Vector)
    abstract fun removeVector(id: String)
}
