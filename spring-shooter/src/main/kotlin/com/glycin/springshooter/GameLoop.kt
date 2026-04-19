package com.glycin.springshooter

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GameLoop(
    private val gameRenderService: GameRenderService,
    private val playerService: PlayerService,
    private val enemyService: EnemyService,
    @Value("#{1000.0 / \${game.settings.fps} / 1000.0}") private val deltaTimeSeconds: Double,
) {

    private val logger = LoggerFactory.getLogger(GameLoop::class.java)

    private var initialized = false

    @Scheduled(fixedRateString = "#{1000 / \${game.settings.fps}}")
    fun tick() {
        if (!initialized) {
            gameRenderService.initialize()
            gameRenderService.onStartGame = ::startGame
            initialized = true
            logger.info("Window initialized, showing menu")
        }

        when (gameRenderService.gameState) {
            GameState.MENU -> gameRenderService.renderMenu()
            GameState.PLAYING -> updateGame()
        }
    }

    private fun startGame() {
        playerService.spawn()
        enemyService.loadEnemies()
        logger.info("Game started with {} enemies!", enemyService.enemies.size)
    }

    private fun updateGame() {
        playerService.update(deltaTimeSeconds)
        enemyService.update(playerService.x, playerService.y, deltaTimeSeconds, playerService::takeDamage)
        gameRenderService.render()
    }
}
