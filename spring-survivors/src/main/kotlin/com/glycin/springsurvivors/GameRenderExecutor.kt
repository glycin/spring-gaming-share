package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.annotations.RendererMethodRegistry
import java.awt.Graphics2D

@GameManager
class GameRenderExecutor(
    private val registry: RendererMethodRegistry,
) {

    private var entries: List<RendererMethodRegistry.RendererEntry>? = null

    fun executeAll(g: Graphics2D) {
        val renderers = entries ?: registry.getEntries().also { entries = it }
        for (entry in renderers) {
            entry.method.invoke(entry.bean, g)
        }
    }
}
