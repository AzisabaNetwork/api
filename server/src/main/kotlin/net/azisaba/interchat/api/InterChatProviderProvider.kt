package net.azisaba.interchat.api

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object InterChatProviderProvider {
    @Suppress("UnstableApiUsage")
    fun register(api: InterChat) {
        InterChatProvider.register(api)
    }
}
