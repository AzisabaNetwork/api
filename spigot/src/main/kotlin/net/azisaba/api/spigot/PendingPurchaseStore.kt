package net.azisaba.api.spigot

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import net.azisaba.api.data.IProduct
import net.azisaba.api.data.StorePurchaseItemV2
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

@Serializable
private data class PendingPurchaseFile(
    val purchases: List<IProduct> = emptyList(),
    val deliveries: List<StorePurchaseItemV2> = emptyList(),
    val processedDeliveryIds: Map<String, Long> = emptyMap(),
)

internal class PendingPurchaseStore(private val path: Path) {
    private val lock = Any()

    fun add(product: IProduct) = synchronized(lock) {
        val purchases = read().purchases + product
        write(PendingPurchaseFile(purchases))
    }

    fun get(uuid: UUID): List<IProduct> = synchronized(lock) {
        read().purchases.filter { it.uuid == uuid }
    }

    fun remove(processed: List<IProduct>) = synchronized(lock) {
        if (processed.isEmpty()) return@synchronized

        val remaining = read().purchases.toMutableList()
        processed.forEach { remaining.remove(it) }
        write(PendingPurchaseFile(remaining))
    }

    fun addDelivery(delivery: StorePurchaseItemV2): Boolean = synchronized(lock) {
        val data = read()
        if (delivery.deliveryId in data.processedDeliveryIds || data.deliveries.any { it.deliveryId == delivery.deliveryId }) {
            return@synchronized false
        }
        write(data.copy(deliveries = data.deliveries + delivery))
        true
    }

    fun getDeliveries(uuid: UUID): List<StorePurchaseItemV2> = synchronized(lock) {
        read().deliveries.filter { it.product.uuid == uuid }
    }

    fun completeDeliveries(deliveries: List<StorePurchaseItemV2>) = synchronized(lock) {
        if (deliveries.isEmpty()) return@synchronized
        val data = read()
        val ids = deliveries.mapTo(mutableSetOf()) { it.deliveryId }
        val now = System.currentTimeMillis()
        write(
            data.copy(
                deliveries = data.deliveries.filterNot { it.deliveryId in ids },
                processedDeliveryIds = data.processedDeliveryIds + ids.associateWith { now },
            ),
        )
    }

    fun recordOnce(deliveryId: String): Boolean = synchronized(lock) {
        val data = read()
        if (deliveryId in data.processedDeliveryIds) return@synchronized false
        write(data.copy(processedDeliveryIds = data.processedDeliveryIds + (deliveryId to System.currentTimeMillis())))
        true
    }

    private fun read(): PendingPurchaseFile {
        if (!Files.exists(path)) return PendingPurchaseFile()
        return path.inputStream().use { input ->
            Yaml.default.decodeFromStream(PendingPurchaseFile.serializer(), input)
        }
    }

    private fun write(data: PendingPurchaseFile) {
        val oldestRetained = System.currentTimeMillis() - PROCESSED_RETENTION_MILLIS
        val compacted = data.copy(
            processedDeliveryIds = data.processedDeliveryIds.filterValues { it >= oldestRetained },
        )
        Files.createDirectories(path.parent)
        val temporaryPath = path.resolveSibling("${path.fileName}.tmp")
        temporaryPath.writeText(Yaml.default.encodeToString(PendingPurchaseFile.serializer(), compacted) + "\n")
        try {
            Files.move(
                temporaryPath,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val PROCESSED_RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
