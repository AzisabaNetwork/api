package net.azisaba.api.server.vector

import kotlin.math.pow
import kotlin.math.sqrt

object VectorUtil {
    fun cosineSimilarity(vectorA: DoubleArray, vectorB: DoubleArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i].pow(2.0)
            normB += vectorB[i].pow(2.0)
        }
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
