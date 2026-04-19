package com.glycin.springaria

import org.springframework.stereotype.Service
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Service
class InputService {

    private val pressedKeys = ConcurrentHashMap.newKeySet<Int>()
    private val oneShotKeys = ConcurrentHashMap<Int, AtomicBoolean>()

    @Volatile var mouseScreenX = 0
    @Volatile var mouseScreenY = 0

    fun keyPressed(keyCode: Int) {
        pressedKeys.add(keyCode)
        oneShotKeys[keyCode]?.set(true)
    }

    fun keyReleased(keyCode: Int) {
        pressedKeys.remove(keyCode)
    }

    fun consume(keyCode: Int): Boolean =
        oneShotKeys.getOrPut(keyCode) { AtomicBoolean() }.getAndSet(false)

    fun mouseClicked(screenX: Int, screenY: Int) {
        mouseScreenX = screenX
        mouseScreenY = screenY
        oneShotKeys.getOrPut(MOUSE_CLICK) { AtomicBoolean() }.set(true)
    }

    fun consumeClick(): Boolean = consume(MOUSE_CLICK)

    fun mouseMoved(screenX: Int, screenY: Int) {
        mouseScreenX = screenX
        mouseScreenY = screenY
    }

    fun isKeyDown(keyCode: Int): Boolean = keyCode in pressedKeys

    val movingLeft get() = isKeyDown(KeyEvent.VK_A) || isKeyDown(KeyEvent.VK_LEFT)
    val movingRight get() = isKeyDown(KeyEvent.VK_D) || isKeyDown(KeyEvent.VK_RIGHT)
    val movingUp get() = isKeyDown(KeyEvent.VK_W) || isKeyDown(KeyEvent.VK_UP)
    val movingDown get() = isKeyDown(KeyEvent.VK_S) || isKeyDown(KeyEvent.VK_DOWN)
    val jumping get() = isKeyDown(KeyEvent.VK_SPACE)

    companion object {
        private const val MOUSE_CLICK = -1
    }
}
