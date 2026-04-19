package com.glycin.annotations

import java.lang.reflect.Method

class UpdateMethodRegistry {

    data class UpdateEntry(
        val bean: Any,
        val method: Method,
        val order: Int,
    )

    private val entries = mutableListOf<UpdateEntry>()
    private val registered = mutableSetOf<String>()
    private var sorted: List<UpdateEntry>? = null

    fun register(beanName: String, bean: Any, method: Method, order: Int) {
        val key = "$beanName.${method.name}"
        if (registered.add(key)) {
            entries.add(UpdateEntry(bean, method, order))
            sorted = null
        }
    }

    fun getOrderedEntries(): List<UpdateEntry> {
        return sorted ?: entries.sortedBy { it.order }.also { sorted = it }
    }
}
