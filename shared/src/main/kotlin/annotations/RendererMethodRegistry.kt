package com.glycin.annotations

import java.lang.reflect.Method

class RendererMethodRegistry {

    data class RendererEntry(
        val bean: Any,
        val method: Method,
    )

    private val entries = mutableListOf<RendererEntry>()
    private val registered = mutableSetOf<String>()

    fun register(beanName: String, bean: Any, method: Method) {
        val key = "$beanName.${method.name}"
        if (registered.add(key)) {
            entries.add(RendererEntry(bean, method))
        }
    }

    fun getEntries(): List<RendererEntry> = entries
}
