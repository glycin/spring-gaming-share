package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.util.GridPos
import java.awt.Graphics2D

interface Attack {
    fun onBeat(playerGridPos: GridPos)
    fun update()
    fun render(g: Graphics2D)
    fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy>
    fun onEnemiesKilled(killed: Set<Enemy>) {}
    fun onLightningFinished(gridPos: GridPos) {}
    fun snapshot(): Any? = null
    fun restore(snapshot: Any?) {}
}
