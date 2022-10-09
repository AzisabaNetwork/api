package net.azisaba.api.util

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty

operator fun AtomicBoolean.getValue(thisRef: Any?, property: KProperty<*>): Boolean {
    return get()
}

operator fun AtomicBoolean.setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
    set(value)
}
