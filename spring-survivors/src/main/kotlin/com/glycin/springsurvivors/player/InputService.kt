package com.glycin.springsurvivors.player

import com.glycin.annotations.Update
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.rhythm.BeatScheduler
import com.glycin.util.GridPos
import org.springframework.stereotype.Service
import java.awt.event.KeyEvent

private const val FRAME_DELTA_MS = 16 // ~60 FPS

private val DIRECTION_KEYS = setOf(KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D)

@Service
class InputService(
    private val gameSettings: GameSettings,
    private val beatScheduler: BeatScheduler,
) {

    private val lock = Any()
    private val pressedKeys = mutableMapOf<Int, Int>()
    private val consumedKeys = mutableSetOf<Int>()

    @Update(order = 15)
    fun update() {
        synchronized(lock) {
            val iter = pressedKeys.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val remaining = entry.value - FRAME_DELTA_MS
                if (remaining <= 0) {
                    iter.remove()
                } else {
                    entry.setValue(remaining)
                }
            }
        }
    }

    fun currentGridDirection(): GridPos? = synchronized(lock) {
        when {
            KeyEvent.VK_A in pressedKeys -> GridPos.LEFT
            KeyEvent.VK_D in pressedKeys -> GridPos.RIGHT
            KeyEvent.VK_W in pressedKeys -> GridPos.UP
            KeyEvent.VK_S in pressedKeys -> GridPos.DOWN
            else -> null
        }
    }

    fun consumeDirection() {
        synchronized(lock) {
            val used = pressedKeys.keys.filter { it in DIRECTION_KEYS }
            pressedKeys.keys.removeAll(DIRECTION_KEYS)
            consumedKeys.addAll(used)
        }
    }

    fun keyPressed(keyCode: Int) {
        synchronized(lock) {
            if (keyCode in consumedKeys) return
            pressedKeys[keyCode] = gameSettings.beat.toleranceMsForBpm(beatScheduler.currentBpmValue)
        }
    }

    fun keyReleased(keyCode: Int) {
        synchronized(lock) {
            pressedKeys.remove(keyCode)
            consumedKeys.remove(keyCode)
        }
    }
}
