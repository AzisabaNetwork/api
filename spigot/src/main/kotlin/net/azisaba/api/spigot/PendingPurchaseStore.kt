package net.azisaba.api.spigot

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import net.azisaba.api.data.IProduct
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

    private fun read(): PendingPurchaseFile {
        if (!Files.exists(path)) return PendingPurchaseFile()
        return path.inputStream().use { input ->
            Yaml.default.decodeFromStream(PendingPurchaseFile.serializer(), input)
        }
    }

    private fun write(data: PendingPurchaseFile) {
        Files.createDirectories(path.parent)
        val temporaryPath = path.resolveSibling("${path.fileName}.tmp")
        temporaryPath.writeText(Yaml.default.encodeToString(PendingPurchaseFile.serializer(), data) + "\n")
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
}
