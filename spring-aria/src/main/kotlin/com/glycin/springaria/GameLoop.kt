package com.glycin.springaria

import com.glycin.util.YamlSettingsWriter
import com.glycin.springaria.gameplay.BulletService
import com.glycin.springaria.gameplay.EnemyService
import com.glycin.springaria.gameplay.FluidSimulationService
import com.glycin.springaria.gameplay.Hotbar
import com.glycin.springaria.gameplay.HotbarItem
import com.glycin.springaria.gameplay.EnemyMissileService
import com.glycin.springaria.gameplay.PlayerMissileService
import com.glycin.springaria.gameplay.Player
import com.glycin.springaria.gameplay.PlayerActionService
import com.glycin.springaria.world.Camera
import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.WorldGeneratorService
import com.glycin.springaria.world.repositories.CaveRecord
import com.glycin.springaria.world.repositories.CaveRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.awt.event.KeyEvent
import java.nio.file.Path

@Component
class GameLoop(
    private val gameRenderService: GameRenderService,
    private val inputService: InputService,
    private val worldGeneratorService: WorldGeneratorService,
    private val camera: Camera,
    private val player: Player,
    private val caveRepository: CaveRepository,
    private val gameSettings: GameSettings,
    private val fluidSimulationService: FluidSimulationService,
    private val hotbar: Hotbar,
    private val bulletService: BulletService,
    private val playerMissileService: PlayerMissileService,
    private val enemyMissileService: EnemyMissileService,
    private val playerActionService: PlayerActionService,
    private val enemyService: EnemyService,
    @Value("\${game.settings.fps}") private val fps: Int,
) {

    private val logger = LoggerFactory.getLogger(GameLoop::class.java)

    private var initialized = false
    private var lastCaveName: String? = null
    private var fluidTick = 0
    private var enemySpawnTick = 0

    @Scheduled(fixedRateString = "#{1000 / \${game.settings.fps}}")
    fun tick() {
        if (!initialized) {
            gameRenderService.initialize()
            initialized = true
            logger.info("Window initialized, showing menu")
        }

        when (gameRenderService.gameState) {
            GameState.MENU -> {
                if (inputService.consume(KeyEvent.VK_ENTER)) {
                    worldGeneratorService.generate()
                    fluidSimulationService.registerFluids()
                    fluidTick = 0
                    populateCaveRepository()
                    spawnPlayer()
                    gameRenderService.gameState = GameState.PLAYING
                    logger.info("Game started!")
                }
                gameRenderService.render()
            }
            GameState.PLAYING -> {
                if (inputService.consume(KeyEvent.VK_ESCAPE)) {
                    gameRenderService.gameState = GameState.SETTINGS
                    gameRenderService.render()
                } else if (inputService.consume(KeyEvent.VK_M)) {
                    gameRenderService.gameState = GameState.MAP
                    gameRenderService.render()
                } else {
                    updateGame()
                }
            }
            GameState.SETTINGS -> {
                if (inputService.consume(KeyEvent.VK_ESCAPE)) {
                    saveSettings()
                    gameRenderService.gameState = GameState.PLAYING
                } else {
                    updateSettings()
                }
                gameRenderService.render()
            }
            GameState.MAP -> {
                if (inputService.consume(KeyEvent.VK_M)) {
                    gameRenderService.gameState = GameState.PLAYING
                }
                gameRenderService.render()
            }
        }
    }

    private fun spawnPlayer() {
        val caves = worldGeneratorService.generatedCaves
        val spawnCave = caves.firstOrNull { it.isUserBean } ?: caves.first()
        player.spawnAt(spawnCave.worldX, spawnCave.worldY)
        camera.centerOn(player.x, player.y)
    }

    private fun populateCaveRepository() {
        for (cave in worldGeneratorService.generatedCaves) {
            caveRepository.save(CaveRecord(cave.name, cave.worldX, cave.worldY, cave.radius, cave.isUserBean))
        }
        logger.info("Populated CaveRepository with {} caves", worldGeneratorService.generatedCaves.size)
    }

    @Scheduled(fixedRate = 1000)
    fun checkCaveProximity() {
        if (gameRenderService.gameState != GameState.PLAYING) return

        val tileX = (player.x / TILE_SIZE).toInt()
        val tileY = (player.y / TILE_SIZE).toInt()
        val cave = caveRepository.findByPlayerInRange(tileX, tileY)

        val caveName = cave?.name
        if (caveName != null && caveName != lastCaveName) {
            logger.info("Entered cave: {}", caveName)
            gameRenderService.showCaveLabel(caveName)
        }
        lastCaveName = caveName
    }

    private fun updateSettings() {
        val count = gameSettings.editableSettings.size
        if (inputService.consume(KeyEvent.VK_UP)) {
            gameRenderService.settingsSelectedIndex = (gameRenderService.settingsSelectedIndex - 1 + count) % count
        }
        if (inputService.consume(KeyEvent.VK_DOWN)) {
            gameRenderService.settingsSelectedIndex = (gameRenderService.settingsSelectedIndex + 1) % count
        }
        val left = inputService.consume(KeyEvent.VK_LEFT)
        val right = inputService.consume(KeyEvent.VK_RIGHT)
        if (left || right) {
            val direction = if (right) 1 else -1
            gameSettings.editableSettings[gameRenderService.settingsSelectedIndex].adjust(direction)
        }
    }

    private fun saveSettings() {
        val values = gameSettings.editableSettings.associate { it.yamlKey to it.get() }
        val paths = listOf(
            Path.of("spring-aria/src/main/resources/application.yml"),
            Path.of("spring-aria/build/resources/main/application.yml"),
        )
        YamlSettingsWriter.writeAll(paths, values)
        logger.info("Settings saved to {}", paths)
    }

    private fun updateGame() {
        fluidTick++
        if (fluidTick >= 4) {
            fluidTick = 0
            fluidSimulationService.tick()
        }

        // Hotbar selection
        for (i in hotbar.items.indices) {
            if (inputService.consume(KeyEvent.VK_1 + i)) {
                hotbar.select(i)
            }
        }

        player.update(inputService.movingLeft, inputService.movingRight, inputService.jumping)
        bulletService.update(enemyService)
        playerMissileService.update(enemyService)
        enemyMissileService.update(player)
        if (enemyMissileService.playerHit) {
            enemyMissileService.playerHit = false
            if (player.hp > 0) {
                player.hp--
                player.lastDamageTime = System.currentTimeMillis()
            }
        }
        enemyService.update(player.x, player.y)
        enemySpawnTick++
        if (enemySpawnTick >= fps) {
            enemySpawnTick = 0
            enemyService.trySpawn(player.x, player.y)
        }
        if (playerMissileService.pendingExplosion) {
            playerMissileService.pendingExplosion = false
            playerActionService.explode()
        }
        if (enemyMissileService.pendingExplosion) {
            enemyMissileService.pendingExplosion = false
            playerActionService.explode()
        }

        if (inputService.consumeClick()) {
            when (hotbar.selectedItem) {
                HotbarItem.PICKAXE -> player.tryMine(inputService.mouseScreenX, inputService.mouseScreenY)
                HotbarItem.HAMMER -> {
                    gameRenderService.triggerSwing(inputService.mouseScreenX, inputService.mouseScreenY)
                    player.tryBuild(inputService.mouseScreenX, inputService.mouseScreenY)
                }
                HotbarItem.UZI -> player.shoot(inputService.mouseScreenX, inputService.mouseScreenY)
                HotbarItem.SOAKER -> player.sprayLava(inputService.mouseScreenX, inputService.mouseScreenY)
                HotbarItem.BAZOOKA -> player.shootMissile(inputService.mouseScreenX, inputService.mouseScreenY)
            }
        }

        camera.centerOn(player.x + player.widthInPixels / 2.0, player.y + player.heightInPixels / 2.0)
        gameRenderService.render()
    }
}
