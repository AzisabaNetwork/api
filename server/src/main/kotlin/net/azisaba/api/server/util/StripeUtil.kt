package net.azisaba.api.server.util

import com.stripe.StripeClient
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import net.azisaba.api.server.ServerConfig

object StripeUtil {
    val stripe = StripeClient(ServerConfig.instance.stripe.secretKey)

    fun createCheckoutSession(successUrl: String, products: Map<String, Long>, saraProd: String?, sara: Int?, saraDiscount: Int?): Session {
        return stripe.checkout().sessions().create(
            SessionCreateParams.Builder()
                .setSuccessUrl(successUrl)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .apply {
                    products.forEach { (productId, quantity) ->
                        addLineItem(
                            SessionCreateParams.LineItem.Builder()
                                .setPrice(productId)
                                .setQuantity(quantity)
                                .build()
                        )
                    }
                    if (saraProd != null && sara != null) {
                        addLineItem(
                            SessionCreateParams.LineItem.Builder()
                                .setQuantity(1)
                                .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.Builder()
                                        .setCurrency("jpy")
                                        .setUnitAmount(sara.toLong() - (saraDiscount ?: 0))
                                        .setProduct(saraProd)
                                        .build()
                                )
                                .build()
                        )
                    }
                }
                .build()
        )
    }
}
