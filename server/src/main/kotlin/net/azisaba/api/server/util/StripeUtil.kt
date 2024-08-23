package net.azisaba.api.server.util

import com.stripe.StripeClient
import net.azisaba.api.server.ServerConfig

object StripeUtil {
    val client = StripeClient(ServerConfig.instance.stripe.secretKey)
}
