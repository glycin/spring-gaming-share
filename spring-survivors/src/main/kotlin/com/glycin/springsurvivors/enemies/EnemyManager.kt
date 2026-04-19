package com.glycin.springsurvivors.enemies

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.util.GridPos
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.rhythm.BeatEvent
import com.glycin.springsurvivors.GameState
import org.springframework.context.event.EventListener
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

private const val HOP_HEIGHT = 12f

@GameManager
class EnemyManager(
    private val gameSettings: GameSettings,
    private val gameGrid: GameGrid,
    private val player: Player,
    private val gameState: GameState,
    private val difficultyManager: DifficultyManager,
) {

    init {
        initAllEnemyFrames(gameGrid.tileSize)
    }

    val enemies = mutableListOf<Enemy>()
    private var spawnTimer = 0

    var waveNumber = 0

    @Update(order = 20)
    fun update() {
        for (enemy in enemies) {
            enemy.updateLerp()
        }
    }

    @EventListener
    fun onBeat(event: BeatEvent) {
        if (gameState.frozen) return

        difficultyManager.onBeat()

        val effectiveInterval = difficultyManager.getSpawnInterval()
        spawnTimer++
        if (spawnTimer > effectiveInterval && enemies.size < gameSettings.spawning.maxEnemies) {
            spawnTimer = 0
            waveNumber++
            val range = difficultyManager.getSpawnCountRange()
            repeat(Random.nextInt(range.first, range.last + 1)) { spawnAtEdge() }
        }

        for (enemy in enemies) {
            enemy.nextFrame()

            if (enemy.beatCounter < enemy.beatSkip + gameState.extraBeatSkip) {
                enemy.beatCounter++
                continue
            }
            enemy.beatCounter = 0

            val dx = player.gridPos.col - enemy.gridPos.col
            val dy = player.gridPos.row - enemy.gridPos.row
            val step = if (abs(dx) >= abs(dy)) {
                GridPos(dx.sign, 0)
            } else {
                GridPos(0, dy.sign)
            }
            val target = enemy.gridPos + step
            if (target == player.gridPos) {
                player.takeDamage(enemy.contactDamage)
            } else {
                enemy.moveTo(target)
            }
        }
    }

    @Renderer
    fun render(g: Graphics2D) {
        val tileSize = gameGrid.tileSize
        for (enemy in enemies) {
            val (drawX, drawY) = gameGrid.lerpPixelPosition(
                enemy.previousGridPos, enemy.gridPos, enemy.lerpProgress, HOP_HEIGHT,
            )
            val frame = enemy.currentFrame()
            if (frame != null) {
                val offsetX = (tileSize - frame.width) / 2
                val offsetY = frame.height / 2
                g.drawImage(frame, drawX + offsetX, drawY - offsetY, null)
            } else {
                g.color = Color.RED
                g.fillRect(drawX, drawY - tileSize / 2, tileSize, tileSize)
            }
        }
    }

    private fun spawnAtEdge() {
        val table = difficultyManager.getSpawnTable()
        val type = weightedRandom(table)
        val hp = type.baseHp * difficultyManager.getHpMultiplier()
        val gridPos = randomEdgePosition()
        enemies.add(Enemy(gridPos, hp, type))
    }

    private fun randomEdgePosition(): GridPos = when (Random.nextInt(4)) {
        0 -> GridPos(Random.nextInt(gameGrid.cols), 0)
        1 -> GridPos(Random.nextInt(gameGrid.cols), gameGrid.rows - 1)
        2 -> GridPos(0, Random.nextInt(gameGrid.rows))
        else -> GridPos(gameGrid.cols - 1, Random.nextInt(gameGrid.rows))
    }

    private fun weightedRandom(table: List<Pair<EnemyType, Int>>): EnemyType {
        val totalWeight = table.sumOf { it.second }
        var roll = Random.nextInt(totalWeight)
        for ((type, weight) in table) {
            roll -= weight
            if (roll < 0) return type
        }
        return table.last().first
    }
}
