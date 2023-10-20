package net.azisaba.api.server.interchat

import net.azisaba.interchat.api.network.JedisBox

object JedisBoxProvider {
    private lateinit var jedisBox: JedisBox

    fun get(): JedisBox {
        if (!(::jedisBox.isInitialized)) {
            jedisBox = InterChatConfig.instance.redis.createJedisBox()
        }
        return jedisBox
    }
}
