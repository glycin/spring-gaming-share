package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.annotations.WithLoopingSound
import com.glycin.springsurvivors.autosave.AutoSaveGameState
import com.glycin.springsurvivors.autosave.BeatMissedError
import com.glycin.springsurvivors.enemies.EnemyManager
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.rhythm.BeatEvent
import com.glycin.springsurvivors.rhythm.BeatScheduler
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener

@GameManager
class GameLoop(
    private val gameRenderService: GameRenderService,
    private val gameSettings: GameSettings,
    private val player: Player,
    private val enemyManager: EnemyManager,
    private val beatScheduler: BeatScheduler,
    private val autoSaveGameState: AutoSaveGameState,
    private val gameTickExecutor: GameTickExecutor,
    private val gameState: GameState,
    private val countdownManager: CountdownManager,
) {

    private val log = LoggerFactory.getLogger(GameLoop::class.java)

    private var initialized = false

    @EventListener
    fun onBeat(event: BeatEvent) {
        gameSettings.beat.bpmEscalation
            .filter { enemyManager.waveNumber >= it.waveNumber }
            .maxByOrNull { it.waveNumber }
            ?.let { beatScheduler.setBpm(it.bpm) }
    }

    fun shouldStopMusic(): Boolean = countdownManager.isCountingDown || gameState.gameOver

    @WithLoopingSound($$"${game.settings.music-path}", stopCondition = "shouldStopMusic")
    fun tick() {
        if (!initialized) {
            gameRenderService.initialize()
            autoSaveGameState.playerGridPos = player.gridPos
            initialized = true
            log.info("Enterprise Survivors started with initial BPM: {}", gameSettings.beat.initialBpm)
        }

        if (countdownManager.isCountingDown) {
            countdownManager.tick()
        } else if (!gameState.gameOver) {
            try {
                beatScheduler.tick()
            } catch (_: BeatMissedError) {
                if (!gameState.frozen) {
                    log.info("Beat missed you n00b. Setting you back!")
                }
            }

            gameTickExecutor.executeAll()
        }

        gameRenderService.render()
    }
}
