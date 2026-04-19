package com.glycin.springsurvivors.rhythm

import com.glycin.springsurvivors.GameSettings
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import com.glycin.annotations.GameManager
import java.util.concurrent.atomic.AtomicInteger

@GameManager
class BeatScheduler(
    gameSettings: GameSettings,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(BeatScheduler::class.java)

    private val currentBpm = AtomicInteger(gameSettings.beat.initialBpm)
    private var lastBeatTime = 0L

    val currentBpmValue: Int get() = currentBpm.get()

    var beatProgress: Float = 0f

    fun setBpm(bpm: Int) {
        val previous = currentBpm.getAndSet(bpm)
        if (previous != bpm) {
            log.info("BPM changed from {} to {}", previous, bpm)
        }
    }

    fun tick() {
        val now = System.currentTimeMillis()
        val beatIntervalMs = 60_000L / currentBpm.get()

        if (lastBeatTime == 0L) {
            lastBeatTime = now
            return
        }

        val elapsed = now - lastBeatTime

        beatProgress = (elapsed.toFloat() / beatIntervalMs).coerceIn(0f, 1f)

        if (elapsed >= beatIntervalMs) {
            lastBeatTime += beatIntervalMs
            val bpm = currentBpm.get()
            eventPublisher.publishEvent(BeatCosmeticEvent(bpm, now))
            eventPublisher.publishEvent(BeatEvent(bpm, now))
        }
    }
}
