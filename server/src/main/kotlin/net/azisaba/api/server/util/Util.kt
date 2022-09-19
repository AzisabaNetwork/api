package net.azisaba.api.server.util

import java.util.concurrent.atomic.AtomicBoolean

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
        val updating = AtomicBoolean(false)
        var cache: Pair<R, Long>? = null

        return {
            if (updating.get()) {
                cache!!.first
            } else {
                val now = System.currentTimeMillis()
                val (value, lastFetched) = cache ?: Pair(fn(), 0L)
                if (lastFetched == 0L || (expireAfter > 0 && now - lastFetched > expireAfter)) {
                    updating.set(true)
                    fn().apply {
                        cache = Pair(this, now)
                        updating.set(false)
                    }
                } else {
                    value
                }
            }
        }
    }
}
