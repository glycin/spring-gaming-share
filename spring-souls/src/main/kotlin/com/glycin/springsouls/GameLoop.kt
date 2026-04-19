package com.glycin.springsouls

import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.input.InputService
import com.glycin.springsouls.metrics.GameMetricsService
import com.glycin.springsouls.render.HudRenderService
import com.glycin.springsouls.render.LevelRenderService
import com.glycin.springsouls.render.PlayerRenderService
import com.glycin.springsouls.render.BossRenderService
import com.glycin.springsouls.render.EnemyRenderService
import com.glycin.springsouls.render.KeyRenderService
import com.glycin.springsouls.render.PotionRenderService
import com.glycin.springsouls.render.MainMenuRenderService
import com.glycin.springsouls.render.RenderService
import com.glycin.springsouls.security.DoorService
import com.glycin.springsouls.security.PlayerAuthentication
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

private const val DEATH_SHUTDOWN_DELAY = 10.0

@Component
class GameLoop(
    private val renderService: RenderService,
    private val inputService: InputService,
    private val player: Player,
    private val metricsService: GameMetricsService,
    private val levelService: LevelService,
    private val playerRenderService: PlayerRenderService,
    private val potionRenderService: PotionRenderService,
    private val keyRenderService: KeyRenderService,
    private val enemyRenderService: EnemyRenderService,
    private val bossRenderService: BossRenderService,
    private val hudRenderService: HudRenderService,
    private val levelRenderService: LevelRenderService,
    private val mainMenuRenderService: MainMenuRenderService,
    private val playerAuthentication: PlayerAuthentication,
    private val doorService: DoorService,
    private val audioService: AudioService,
    private val applicationContext: ConfigurableApplicationContext,
    @Value("#{1000.0 / \${game.settings.fps} / 1000.0}") private val deltaTimeSeconds: Double, // For units per second calculations
) {
    private val logger = LoggerFactory.getLogger(GameLoop::class.java)
    private var initialized = false
    private var deathTimer = 0.0
    private var deathRecorded = false

    @Scheduled(fixedRateString = "#{1000 / \${game.settings.fps}}")
    fun tick() {
        if (!initialized) {
            player.x = levelService.spawnX
            player.z = levelService.spawnZ
            renderService.initialize()
            enemyRenderService.onAllEnemiesDefeated = {
                playerAuthentication.grant("BOSS")
                hudRenderService.showNotification("GRANTED ARCHITECHT AUTHORITY", 3f, 0.8f, 0.2f, 1f)
                logger.info("All enemies defeated -> BOSS authority granted")
            }
            initialized = true
            logger.info("Spring Souls initialized... Prepare to die (enterprise edition)")
        }

        if (renderService.isWindowClosed()) {
            logger.info("Window closed, shutting down")
            renderService.shutdown()
            return
        }

        if (!renderService.isRunning()) return

        metricsService.timeGameTick {
            when (renderService.gameState) {
                GameState.MENU -> updateMenu()
                GameState.PLAYING -> {
                    audioService.playBackgroundTheme()
                    updateGame()
                }
            }
        }
    }

    private fun updateMenu() {
        mainMenuRenderService.update(deltaTimeSeconds.toFloat())
        if (inputService.startGame) {
            renderService.gameState = GameState.PLAYING
            logger.info("Game started")
        }
    }

    private fun updateGame() {
        if (inputService.consume(GLFW.GLFW_KEY_ESCAPE)) {
            hudRenderService.menuOpen = !hudRenderService.menuOpen
        }

        renderService.withStateLock {
            player.update(inputService, renderService.camera, deltaTimeSeconds)
            playerRenderService.update(deltaTimeSeconds.toFloat(), renderService.camera.yaw)
            player.processAttack { x, z, radius, damage ->
                enemyRenderService.damageEnemyAt(x, z, radius, damage)
                    || bossRenderService.damageBossAt(x, z, radius, damage)
            }

            potionRenderService.update(deltaTimeSeconds.toFloat())
            keyRenderService.update(deltaTimeSeconds.toFloat())
            enemyRenderService.update(deltaTimeSeconds.toFloat())
            bossRenderService.update(deltaTimeSeconds.toFloat())
            hudRenderService.updateNotification(deltaTimeSeconds.toFloat())
            player.updateCamera(renderService.camera)
        }

        if (!player.isAlive) {
            if (!deathRecorded) {
                deathRecorded = true
                metricsService.recordDeath("combat")
            }
            hudRenderService.playerDead = true
            deathTimer += deltaTimeSeconds
            if (deathTimer >= DEATH_SHUTDOWN_DELAY) {
                renderService.shutdown()
                applicationContext.close()
                return
            }
        }

        if (inputService.interact) {
            val tileX = player.x.toInt()
            val tileZ = player.z.toInt()
            outer@ for (dx in -1..1) {
                for (dz in -1..1) {
                    val tile = levelService.getTile(tileX + dx, tileZ + dz)
                    if (tile == TILE_DOOR) {
                        keyRenderService.useKey(tileX + dx, tileZ + dz)
                        break@outer
                    }
                    if (tile == TILE_BOSS_WALL) {
                        try {
                            doorService.traverseWhiteFogWall()
                            levelRenderService.removeAllBossWalls()
                            hudRenderService.showNotification("THE SCRUM WALL DISSOLVES", 3f, 0.9f, 0.9f, 1f)
                            metricsService.recordDoorOpened("fog_wall")
                        } catch (e: AccessDeniedException) {
                            hudRenderService.showNotification("A SCRUM WALL BLOCKS YOUR PATH", 3f, 0.8f, 0.2f, 0.2f)
                            metricsService.recordDoorDenied("fog_wall")
                        }
                        break@outer
                    }
                }
            }
        }
    }

}