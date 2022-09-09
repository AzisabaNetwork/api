package net.azisaba.api.velocity.schemes

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializerOrNull
import net.azisaba.api.util.JSON
import net.azisaba.api.velocity.DatabaseManager
import org.intellij.lang.annotations.Language
import java.lang.reflect.Parameter
import java.sql.ResultSet
import java.util.UUID
import kotlin.reflect.KClass

abstract class Table<T : Any>(clazz: KClass<T>) {
    private val clazz = clazz.java

    /**
     * Select rows from the table. [sql] is a SQL query. [args] is a list of arguments. Order of columns in [sql] must
     * match the order of arguments of constructor of [T].
     */
    fun select(@Language("SQL") sql: String, vararg args: Any): Collection<T> =
        DatabaseManager.execute(sql) {
            for (i in args.indices) {
                it.setObject(i + 1, args[i])
            }
            val rs = it.executeQuery()
            val values = mutableListOf<T>()
            val ctr = clazz.constructors.first { !it.isSynthetic }
            while (rs.next()) {
                val ctrArgs = mutableListOf<Any>()
                ctr.parameters.forEachIndexed { index, param ->
                    try {
                        ctrArgs.add(param.extractValue(index + 1, rs))
                    } catch (e: Exception) {
                        // TODO: use proper logger
                        System.err.println("Failed to extract value from column ${index + 1}")
                        e.printStackTrace()
                        throw e
                    }
                }
                ctr.newInstance(*ctrArgs.toTypedArray()).apply {
                    @Suppress("UNCHECKED_CAST")
                    values.add(this as T)
                }
            }
            values
        }

    fun insertB(tableName: String, value: T, valueOverridesBuilder: MutableMap<String, Any?>.() -> Unit = {}) {
        val map = mutableMapOf<String, Any?>()
        valueOverridesBuilder(map)
        insertM(tableName, value, map)
    }

    fun insertM(tableName: String, value: T, valueOverrides: Map<String, Any?> = emptyMap()) {
        val ctr = value
            .javaClass
            .constructors
            .first { !it.isSynthetic }
        val sqlValues = ctr.parameters.joinToString(", ") { "?" }
        val sqlActualValues =
            ctr.parameters
                .mapIndexed { index, param ->
                    if (valueOverrides.containsKey(param.name)) {
                        valueOverrides[param.name]
                    } else if (valueOverrides.containsKey("$index")) {
                        valueOverrides["$index"]
                    } else {
                        val fieldValue =
                            value.javaClass
                                .declaredFields
                                .find { f -> f.name == param.name }
                                ?.apply { isAccessible = true }
                                ?.get(value)
                                ?: value.javaClass
                                    .declaredFields
                                    .filter { f -> f.name[0].isLowerCase() }[index]
                                    .apply { isAccessible = true }
                                    .get(value)
                        if (fieldValue == null) {
                            null
                        } else {
                            param.toValue(fieldValue)
                        }
                    }
                }
                .toTypedArray()

        DatabaseManager.execute("INSERT INTO `$tableName` VALUES ($sqlValues)") {
            for (i in sqlActualValues.indices) {
                it.setObject(i + 1, sqlActualValues[i])
            }
            it.executeUpdate()
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun Parameter.extractValue(i: Int, rs: ResultSet): Any =
        when (type) {
            Int::class.java -> rs.getInt(i)
            Float::class.java -> rs.getFloat(i)
            Double::class.java -> rs.getDouble(i)
            Long::class.java -> rs.getLong(i)
            Byte::class.java -> rs.getByte(i)
            Short::class.java -> rs.getShort(i)
            Boolean::class.java -> rs.getBoolean(i)
            String::class.java -> rs.getString(i)
            UUID::class.java -> UUID.fromString(rs.getString(i))
            else -> {
                type.kotlin.serializerOrNull().let {
                    if (it == null) {
                        JSON.decodeFromString(rs.getString(i))
                    } else {
                        JSON.decodeFromString(it, rs.getString(i))
                    }
                }
            }
        }

    @OptIn(InternalSerializationApi::class)
    private inline fun <reified T : Any> Parameter.toValue(value: T): Any =
        when (type) {
            Int::class.java -> value as Int
            Float::class.java -> value as Float
            Double::class.java -> value as Double
            Long::class.java -> value as Long
            Byte::class.java -> value as Byte
            Short::class.java -> value as Short
            Boolean::class.java -> value as Boolean
            String::class.java -> value as String
            UUID::class.java -> value.toString()
            else -> {
                type.kotlin.serializerOrNull().let {
                    if (it == null) {
                        JSON.encodeToString(value)
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        JSON.encodeToString(it as KSerializer<Any>, value)
                    }
                }
            }
        }
}
