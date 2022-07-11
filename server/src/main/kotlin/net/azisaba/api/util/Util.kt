package net.azisaba.api.util

object Util {
    fun <T, R> memoize(expireAfter: Long = 0, fn: (T) -> R): (T) -> R {
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
}
