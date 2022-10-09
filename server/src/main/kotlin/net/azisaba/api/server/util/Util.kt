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

    inline fun <T, U, R> memoize2(expireAfter: Long = 0, crossinline fn: (T, U) -> R): (T, U) -> R {
        val cache = mutableMapOf<Pair<T, U>, Pair<R, Long>>()

        return { t, u ->
            val pair = Pair(t, u)
            val now = System.currentTimeMillis()
            val (value, lastUsed) = cache[pair] ?: Pair(fn(t, u), now)
            if (lastUsed == now || (expireAfter > 0 && now - lastUsed > expireAfter)) {
                if (lastUsed != now) {
                    cache[pair] = Pair(fn(t, u), now)
                } else {
                    cache[pair] = Pair(value, lastUsed)
                }
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

    fun <R> runNoinline(fn: () -> R): R = fn()
}
