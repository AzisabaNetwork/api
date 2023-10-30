package net.azisaba.api.server.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import net.azisaba.api.Logger
import net.azisaba.api.server.TaskScheduler
import net.azisaba.api.util.JSON
import net.azisaba.api.util.getValue
import net.azisaba.api.util.setValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule
import kotlin.io.path.*

object PersistentDataStore {
    private val path: Path = File("persistent-data.json").toPath()
    var dirty by AtomicBoolean()
    @Serializable
    val containers: MutableList<IPersistentDataContainer<*>> = Collections.synchronizedList(mutableListOf())

    init {
        load()
        Logger.currentLogger.info("Loaded persistent data from ${path.toAbsolutePath()}")
        TaskScheduler.schedule(1000 * 30, 1000 * 30) {
            try {
                if (dirty) {
                    save()
                    Logger.currentLogger.info("Saved persistent data in background")
                }
            } catch (e: Exception) {
                Logger.currentLogger.error("Failed to save persistent data", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getContainerOrNull() =
        containers.find { it.type == T::class.java } as PersistentDataContainer<T>?

    inline fun <reified T : Any> getContainer() =
        getContainerOrNull() ?: PersistentDataContainer(T::class.java).also { containers.add(it) }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getListContainerOrNull() =
        containers.find { it.type == T::class.java } as PersistentListDataContainer<T>?

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getListContainer() =
        getListContainerOrNull() ?: PersistentListDataContainer(List::class.java as Class<List<T>>, T::class.java).also { containers.add(it) }

    fun save() {
        path.copyTo(path.resolve("../${path.name}.bak"), true)
        val array = JsonArray(containers.map { JSON.encodeToJsonElement(it.asSerialized()) })
        path.writeText(JSON.encodeToString(array))
    }

    fun load() {
        containers.clear()
        if (Files.exists(path)) {
            try {
                JSON.parseToJsonElement(path.readText()).jsonArray.forEach { element ->
                    containers.add(
                        JSON.decodeFromJsonElement(ISerializedPersistentDataContainer.serializer(), element)
                            .asPersistentDataContainer()
                    )
                }
            } catch (e: Exception) {
                val bakFile = path.resolve("../${path.name}.bak")
                if (!bakFile.exists()) {
                    throw RuntimeException("persistent-data.json is corrupted and no backup available", e)
                }
                Logger.currentLogger.error("persistent-data.json is corrupted, trying to restore from backup")
                bakFile.moveTo(path, true)
                load()
            }
        }
    }
}
