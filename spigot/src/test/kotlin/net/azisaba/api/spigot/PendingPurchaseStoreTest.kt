package net.azisaba.api.spigot

import net.azisaba.api.data.Product
import net.azisaba.api.data.SaraProduct
import net.azisaba.api.data.StorePurchaseItemV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class PendingPurchaseStoreTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `persists products between store instances`() {
        val path = temporaryDirectory.resolve("pending-purchases.yml")
        val uuid = UUID.randomUUID()

        PendingPurchaseStore(path).apply {
            add(Product(uuid, 10))
            add(SaraProduct(uuid, 500))
        }

        assertEquals(
            listOf(Product(uuid, 10), SaraProduct(uuid, 500)),
            PendingPurchaseStore(path).get(uuid),
        )
    }

    @Test
    fun `removes only processed occurrences`() {
        val path = temporaryDirectory.resolve("pending-purchases.yml")
        val uuid = UUID.randomUUID()
        val product = Product(uuid, 10)
        val store = PendingPurchaseStore(path)
        store.add(product)
        store.add(product)

        store.remove(listOf(product))

        assertEquals(listOf(product), store.get(uuid))
    }

    @Test
    fun `deduplicates delivery ids across restarts`() {
        val path = temporaryDirectory.resolve("pending-purchases.yml")
        val uuid = UUID.randomUUID()
        val delivery = StorePurchaseItemV2("order-1", "delivery-1", Product(uuid, 10))

        assertEquals(true, PendingPurchaseStore(path).addDelivery(delivery))
        assertEquals(false, PendingPurchaseStore(path).addDelivery(delivery))
        PendingPurchaseStore(path).completeDeliveries(listOf(delivery))

        assertEquals(emptyList<StorePurchaseItemV2>(), PendingPurchaseStore(path).getDeliveries(uuid))
        assertEquals(false, PendingPurchaseStore(path).addDelivery(delivery))
    }
}
