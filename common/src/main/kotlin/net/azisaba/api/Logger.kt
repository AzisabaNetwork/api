package net.azisaba.api

import net.azisaba.api.util.ReflectionUtil
import org.jetbrains.annotations.Contract
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.logging.Level

/**
 * Represents a logger.
 */
interface Logger {
    fun info(message: String)
    fun info(message: String, p1: Any?)
    fun info(message: String, p1: Any, p2: Any)
    fun info(message: String, vararg params: Any?)
    fun info(message: String, throwable: Throwable?)
    fun warn(message: String)
    fun warn(message: String, p1: Any?)
    fun warn(message: String, p1: Any, p2: Any)
    fun warn(message: String, vararg params: Any?)
    fun warn(message: String, throwable: Throwable?)
    fun error(message: String)
    fun error(message: String, p1: Any?)
    fun error(message: String, p1: Any, p2: Any)
    fun error(message: String, vararg params: Any?)
    fun error(message: String, throwable: Throwable?)

    companion object {
        private lateinit var logger: Logger
        private val defaultLogger = createFromJavaLogger(java.util.logging.Logger.getLogger("api-ktor"))

        /**
         * Creates logger by creating Proxy class for the instance.
         * @param instance the instance
         * @return the logger
         */
        @Contract(value = "_ -> new", pure = true)
        fun createByProxy(instance: Any): Logger {
            val instClass: Class<*> = instance.javaClass
            return Proxy.newProxyInstance(Logger::class.java.classLoader, arrayOf(Logger::class.java)) { _, method, args ->
                val m: Method = ReflectionUtil.findMethod(instClass, method)
                    ?: throw RuntimeException(instClass.typeName + " does not implements " + method.toGenericString())
                m.invoke(instance, *args)
            } as Logger
        }

        @Contract(value = "_ -> new", pure = true)
        fun createFromJavaLogger(logger: java.util.logging.Logger): Logger {
            return object : Logger {
                private fun format(msg: String): String {
                    var actualMessage = msg
                    var length = 0
                    while (actualMessage.contains("{}")) {
                        actualMessage = actualMessage.replaceFirst("\\{}".toRegex(), "{" + length++ + "}")
                    }
                    return actualMessage
                }

                override fun info(message: String) {
                    logger.log(Level.INFO, format(message))
                }

                override fun info(message: String, p1: Any?) {
                    logger.log(Level.INFO, format(message), p1)
                }

                override fun info(message: String, p1: Any, p2: Any) {
                    logger.log(Level.INFO, format(message), arrayOf(p1, p2))
                }

                override fun info(message: String, vararg params: Any?) {
                    logger.log(Level.INFO, format(message), params)
                }

                override fun info(message: String, throwable: Throwable?) {
                    logger.log(Level.INFO, format(message), throwable)
                }

                override fun warn(message: String) {
                    logger.log(Level.WARNING, format(message))
                }

                override fun warn(message: String, p1: Any?) {
                    logger.log(Level.WARNING, format(message), p1)
                }

                override fun warn(message: String, p1: Any, p2: Any) {
                    logger.log(Level.WARNING, format(message), arrayOf(p1, p2))
                }

                override fun warn(message: String, vararg params: Any?) {
                    logger.log(Level.WARNING, format(message), params)
                }

                override fun warn(message: String, throwable: Throwable?) {
                    logger.log(Level.WARNING, format(message), throwable)
                }

                override fun error(message: String) {
                    logger.log(Level.SEVERE, format(message))
                }

                override fun error(message: String, p1: Any?) {
                    logger.log(Level.SEVERE, format(message), p1)
                }

                override fun error(message: String, p1: Any, p2: Any) {
                    logger.log(Level.SEVERE, format(message), arrayOf(p1, p2))
                }

                override fun error(message: String, vararg params: Any?) {
                    logger.log(Level.SEVERE, format(message), params)
                }

                override fun error(message: String, throwable: Throwable?) {
                    logger.log(Level.SEVERE, format(message), throwable)
                }
            }
        }

        /**
         * Returns the logger on the current environment. Returns a default one if the plugin is not yet enabled.
         * @return the logger
         */
        val currentLogger: Logger
            get() {
                if (::logger.isInitialized) {
                    return logger
                }
                return defaultLogger
            }

        fun setLogger(newLogger: Logger) {
            if (::logger.isInitialized) {
                throw IllegalStateException("Logger is already initialized")
            }
            logger = newLogger
        }

        fun java.util.logging.Logger.registerLogger() = setLogger(createFromJavaLogger(this))
        fun org.slf4j.Logger.registerLogger() = setLogger(createByProxy(this))
    }
}
