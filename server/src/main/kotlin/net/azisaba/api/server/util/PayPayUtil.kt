package net.azisaba.api.server.util
/*
import jp.ne.paypay.ApiException
import jp.ne.paypay.Configuration
import jp.ne.paypay.api.PaymentApi
import jp.ne.paypay.model.MoneyAmount
import jp.ne.paypay.model.PaymentState
import jp.ne.paypay.model.QRCode
import jp.ne.paypay.model.Refund
import net.azisaba.api.Logger
import net.azisaba.api.server.ServerConfig
import net.azisaba.api.server.TaskScheduler
import java.util.UUID
import kotlin.concurrent.schedule

object PayPayUtil {
    val apiClient by lazy {
        Configuration().defaultApiClient.apply {
            isProductionMode = ServerConfig.instance.paypay.production
            setApiKey(ServerConfig.instance.paypay.apiKey)
            setApiSecretKey(ServerConfig.instance.paypay.apiSecretKey)
            assumeMerchant = ServerConfig.instance.paypay.merchantId
        }
    }
    private val paymentApi by lazy { PaymentApi(apiClient) }

    fun createQRCode(amount: Int, description: String?, purchased: () -> Unit): String {
        val qrCode = QRCode()
        val paymentId = UUID.randomUUID().toString()
        Logger.currentLogger.info("Attempting to create QR code with id: {}, amount: {}, description: {}", paymentId, amount, description)
        qrCode.amount = MoneyAmount().amount(amount).currency(MoneyAmount.CurrencyEnum.JPY)
        qrCode.merchantPaymentId = paymentId
        qrCode.codeType = "ORDER_QR"
        if (description != null) {
            qrCode.orderDescription = description
        }
        qrCode.isAuthorization(false)
        val details = try {
            paymentApi.createQRCode(qrCode)
        } catch (e: ApiException) {
            error("Error returned from PayPay API, response body: ${e.responseBody}")
        }
        if (details.resultInfo.code != "SUCCESS") {
            error("Something went wrong (code: ${details.resultInfo.code})")
        }
        onPaymentCreated(paymentId, amount, description)
        val start = System.currentTimeMillis()
        TaskScheduler.schedule(1000 * 30, 1000 * 30) {
            try {
                if (System.currentTimeMillis() - start > 1000 * 60 * 6) error("Timeout")
                val response = paymentApi.getCodesPaymentDetails(paymentId)
                if (response.resultInfo.code != "SUCCESS") {
                    error("Response code was ${response.resultInfo.code}")
                }
                if (response.data.status != PaymentState.StatusEnum.CREATED) {
                    cancel()
                }
                if (response.data.status == PaymentState.StatusEnum.COMPLETED) {
                    Logger.currentLogger.info("Payment {} was success, executing callback", paymentId)
                    onPaymentCompleted(response.data.paymentId, paymentId, amount, description)
                    purchased()
                }
            } catch (e: Throwable) {
                cancel()
                Logger.currentLogger.info("Cancelling the payment {} because of an error", paymentId, e)
                paymentApi.cancelPayment(paymentId)
                onPaymentCancelled(paymentId, amount, description)
                throw e
            }
        }
        Logger.currentLogger.info("URL returned from PayPay API: ${details.data.url}")
        return details.data.url
    }

    fun cancelPayment(paymentId: String) {
        paymentApi.cancelPayment(paymentId)
    }

    fun refundPayment(paymentId: String, reason: String) {
        paymentApi.refundPayment(Refund().paymentId(paymentId).reason(reason))
    }

    fun onPaymentCreated(paymentId: String, amount: Int, description: String?) {
        Util.sendDiscordWebhookAsync(
            ServerConfig.instance.paypay.discordNotifyUrl,
            null,
            "`$paymentId`の決済が作成されました(決済は完了していません)。\n金額: $amount JPY\n説明: $description",
        )
    }

    fun onPaymentCompleted(paymentId: String, merchantPaymentId: String, amount: Int, description: String?) {
        Util.sendDiscordWebhookAsync(
            ServerConfig.instance.paypay.discordNotifyUrl,
            null,
            "`$merchantPaymentId`の決済が完了しました。\n金額: $amount JPY\n説明: $description\nPayPay側決済ID: $paymentId",
        )
    }

    fun onPaymentCancelled(paymentId: String, amount: Int, description: String?) {
        Util.sendDiscordWebhookAsync(
            ServerConfig.instance.paypay.discordNotifyUrl,
            null,
            "`$paymentId`の決済がエラーのため取り消されました。\n金額: $amount JPY\n説明: $description",
        )
    }
}
*/
