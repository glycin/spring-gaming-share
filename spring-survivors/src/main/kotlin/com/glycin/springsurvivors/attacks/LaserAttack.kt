package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.util.GridPos
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

private const val LASER_LIFETIME = 12
private val LASER_COLOR = Color(0, 255, 180)
private val LASER_CORE_COLOR = Color(200, 255, 230)

class LaserAttack(
    private val gameGrid: GameGrid,
) : Attack {

    var beatsPerFire: Int = 5
    var laserDamageMultiplier: Float = 1.0f
    var laserWidth: Float = 4f
    var diagonalLasers: Boolean = false

    private val activeBeams = mutableListOf<LaserBeam>()
    private var beatCounter = 0

    override fun onBeat(playerGridPos: GridPos) {
        beatCounter++
        if (beatCounter < beatsPerFire) return
        beatCounter = 0

        val directions = buildList {
            add(GridPos(1, 0)); add(GridPos(-1, 0)); add(GridPos(0, 1)); add(GridPos(0, -1))
            if (diagonalLasers) {
                add(GridPos(1, 1)); add(GridPos(1, -1)); add(GridPos(-1, 1)); add(GridPos(-1, -1))
            }
        }

        for (dir in directions) {
            val cells = mutableListOf<GridPos>()
            var pos = playerGridPos + dir
            while (gameGrid.isInBounds(pos)) {
                cells.add(pos)
                pos += dir
            }
            if (cells.isNotEmpty()) {
                activeBeams.add(LaserBeam(playerGridPos, cells, LASER_LIFETIME))
            }
        }
    }

    override fun update() {
        activeBeams.forEach { it.lifetime-- }
        activeBeams.removeAll { it.lifetime <= 0 }
    }

    override fun render(g: Graphics2D) {
        if (activeBeams.isEmpty()) return

        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        val oldComposite = g.composite
        val oldStroke = g.stroke

        for (beam in activeBeams) {
            val alpha = beam.lifetime.toFloat() / LASER_LIFETIME
            val originX = gameGrid.toPixelX(beam.origin.col) + halfTile
            val originY = gameGrid.toPixelY(beam.origin.row) + halfTile
            val endCell = beam.cells.last()
            val endX = gameGrid.toPixelX(endCell.col) + halfTile
            val endY = gameGrid.toPixelY(endCell.row) + halfTile

            g.composite = AlphaComposite.SrcOver.derive(alpha)
            g.stroke = BasicStroke(laserWidth * alpha + 1f)
            g.color = LASER_COLOR
            g.drawLine(originX, originY, endX, endY)

            g.stroke = BasicStroke(1.5f)
            g.color = LASER_CORE_COLOR
            g.drawLine(originX, originY, endX, endY)
        }

        g.composite = oldComposite
        g.stroke = oldStroke
    }

    override fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy> {
        val killed = mutableSetOf<Enemy>()

        for (beam in activeBeams) {
            if (beam.lifetime < LASER_LIFETIME - 1) continue // only damage on first frame
            for (cell in beam.cells) {
                val cellEnemies = enemiesByCell[cell] ?: continue
                for (enemy in cellEnemies) {
                    if (enemy in killed) continue
                    enemy.hp -= damageMultiplier * laserDamageMultiplier
                    if (enemy.hp <= 0f) {
                        killed.add(enemy)
                    }
                }
            }
        }

        return killed
    }

    private class LaserBeam(
        val origin: GridPos,
        val cells: List<GridPos>,
        var lifetime: Int,
    )
}
