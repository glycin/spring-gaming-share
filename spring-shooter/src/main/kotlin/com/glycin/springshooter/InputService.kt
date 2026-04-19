package com.glycin.springshooter

import org.springframework.stereotype.Service
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Service
class InputService {

    private val pressedKeys = ConcurrentHashMap.newKeySet<Int>()
    private val mouseDeltaX = AtomicInteger(0)
    private val mouseDeltaY = AtomicInteger(0)
    private val shootRequested = AtomicBoolean(false)

    fun keyPressed(keyCode: Int) {
        pressedKeys.add(keyCode)
    }

    fun keyReleased(keyCode: Int) {
        pressedKeys.remove(keyCode)
    }

    fun addMouseDelta(dx: Int, dy: Int) {
        mouseDeltaX.addAndGet(dx)
        mouseDeltaY.addAndGet(dy)
    }

    @Volatile var edgeScrollX: Int = 0
    @Volatile var edgeScrollY: Int = 0

    fun consumeMouseDeltaX(): Int = mouseDeltaX.getAndSet(0) + edgeScrollX

    fun consumeMouseDeltaY(): Int = mouseDeltaY.getAndSet(0) + edgeScrollY

    fun requestShoot() {
        shootRequested.set(true)
    }

    fun consumeShoot(): Boolean = shootRequested.getAndSet(false)

    fun isKeyDown(keyCode: Int): Boolean = keyCode in pressedKeys

    val movingForward get() = isKeyDown(KeyEvent.VK_W)
    val movingBackward get() = isKeyDown(KeyEvent.VK_S)
    val strafingLeft get() = isKeyDown(KeyEvent.VK_A)
    val strafingRight get() = isKeyDown(KeyEvent.VK_D)
    val turningLeft get() = isKeyDown(KeyEvent.VK_LEFT)
    val turningRight get() = isKeyDown(KeyEvent.VK_RIGHT)
    val shooting get() = isKeyDown(KeyEvent.VK_SPACE)
    val reloading get() = isKeyDown(KeyEvent.VK_R)
}
