package net.azisaba.api.server.util

object Util {
    inline fun <T, R> memoize(expireAfter: Long = 0, crossinline fn: (T) -> R): (T) -> R {
        val cache = mutableMapOf<T, Pair<R, Long>>()
        return {
            val now = System.currentTimeMillis()
            val (value, lastUsed) = cache[it] ?: Pair(fn(it), now)
            if (lastUsed == now || (expireAfter > 0 && now - lastUsed > expireAfter)) {
                cache[it] = Pair(fn(it), now)
            }
            value
        }
    }

    fun <R> memoizeSupplier(expireAfter: Long = 0, fn: () -> R): () -> R {
        var cache: Pair<R, Long>? = null

        return {
            val now = System.currentTimeMillis()
            val (value, lastUsed) = cache ?: Pair(fn(), now)
            if (lastUsed == now || (expireAfter > 0 && now - lastUsed > expireAfter)) {
                cache = Pair(fn(), now)
            }
            value
        }
    }
}
