package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.annotations.UpdateMethodRegistry

@GameManager
class GameTickExecutor(
    private val registry: UpdateMethodRegistry,
) {

    private var entries: List<UpdateMethodRegistry.UpdateEntry>? = null

    fun executeAll() {
        val ordered = entries ?: registry.getOrderedEntries().also { entries = it }
        for (entry in ordered) {
            entry.method.invoke(entry.bean)
        }
    }
}
