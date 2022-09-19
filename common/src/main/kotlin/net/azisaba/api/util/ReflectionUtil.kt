package net.azisaba.api.util

import java.lang.reflect.Method

object ReflectionUtil {
    // 1. look for method on superclass
    // 2. look for method on interfaces, then interfaces in interface...
    /**
     * Finds the method recursively with the given method (uses name and parameter types for finding a method).
     * @param clazz the base class to look for the method
     * @param m the method to look for
     * @return the method if found; null otherwise
     */
    fun findMethod(clazz: Class<*>, m: Method): Method? {
        try {
            return clazz.getDeclaredMethod(m.name, *m.parameterTypes)
        } catch (ignore: ReflectiveOperationException) {
        }
        clazz.superclass?.apply {
            findMethod(this, m)?.let { return it }
        }
        for (c in clazz.interfaces) {
            findMethod(c, m)?.let { return it }
        }
        return null
    }

    // 2 + this method
    fun getCallerClass(): Class<*> = getCallerClass(3) // 2 + this method

    fun getCallerClass(offset: Int): Class<*> {
        val stElements = Thread.currentThread().stackTrace
        for (i in 1 + offset until stElements.size) {
            val ste = stElements[i]
            if (ste.className != ReflectionUtil::class.java.name && !ste.className.contains("java.lang.Thread")) {
                return try {
                    Class.forName(ste.className)
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e)
                }
            }
        }
        throw NoSuchElementException("elements: ${stElements.size}, offset: $offset")
    }
}
