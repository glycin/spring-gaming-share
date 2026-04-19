package com.glycin.springsnake

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class GameLoop(
    private val snakeService: SnakeService,
    private val foodRepository: FoodRepository,
    private val gameRenderService: GameRenderService,
) {

    private val logger = LoggerFactory.getLogger(GameLoop::class.java)
    private var initialized = false
    private var gameStarted = false
    private var gameOver = false
    private var score = 0
    private var tick = 0

    @Scheduled(fixedRateString = $$"${game.settings.tick-rate}")
    fun tick() {
        logger.info("tick ${tick++}")
        if (!initialized) {
            gameRenderService.initialize()
            gameRenderService.onStartGame = ::startGame
            initialized = true
            logger.info("Window initialized, showing menu")
        }

        when (gameRenderService.gameState) {
            GameState.MENU -> gameRenderService.renderMenu()
            GameState.PLAYING -> updateGame()
            GameState.GAME_OVER -> {}
        }
    }

    private fun startGame() {
        if (gameStarted) return
        foodRepository.spawn(snakeService.body)
        gameStarted = true
        logger.info("Game started")
    }

    private fun updateGame() {
        if (gameOver) return

        val ate = snakeService.nextHeadPos() == foodRepository.food
        snakeService.move(ate)

        if (ate) {
            score += Random.nextInt(10, 51)
            foodRepository.spawn(snakeService.body)
            logger.info("Score: $score")
        }

        if (snakeService.collidesWithSelf()) {
            gameOver = true
            gameRenderService.gameState = GameState.GAME_OVER
            logger.info("Game over! Final score: $score")
        }

        val aboutToEat = snakeService.nextHeadPos() == foodRepository.food
        gameRenderService.render(snakeService.body, foodRepository.food, foodRepository.foodType, score, gameOver, aboutToEat)
    }
}
