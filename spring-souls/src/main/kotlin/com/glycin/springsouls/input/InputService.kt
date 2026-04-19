package com.glycin.springsouls.input

import org.lwjgl.glfw.GLFW.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Service
class InputService {

    private val pressedKeys = ConcurrentHashMap.newKeySet<Int>()
    private val oneShotKeys = ConcurrentHashMap<Int, AtomicBoolean>()
    private val leftMouseDown = AtomicBoolean(false)
    private val rightMouseDown = AtomicBoolean(false)

    private val mouseDeltaXMicros = AtomicInteger(0)
    private val mouseDeltaYMicros = AtomicInteger(0)
    @Volatile private var lastMouseX = 0f
    @Volatile private var lastMouseY = 0f
    @Volatile private var firstMouse = true

    fun keyPressed(keyCode: Int) {
        pressedKeys.add(keyCode)
        oneShotKeys.getOrPut(keyCode) { AtomicBoolean() }.set(true)
    }

    fun keyReleased(keyCode: Int) {
        pressedKeys.remove(keyCode)
    }

    fun mousePressed(button: Int) {
        when (button) {
            GLFW_MOUSE_BUTTON_LEFT -> leftMouseDown.set(true)
            GLFW_MOUSE_BUTTON_RIGHT -> rightMouseDown.set(true)
        }
    }

    fun mouseReleased(button: Int) {
        when (button) {
            GLFW_MOUSE_BUTTON_LEFT -> leftMouseDown.set(false)
            GLFW_MOUSE_BUTTON_RIGHT -> rightMouseDown.set(false)
        }
    }

    fun mouseMoved(x: Float, y: Float) {
        if (firstMouse) {
            lastMouseX = x
            lastMouseY = y
            firstMouse = false
        }
        mouseDeltaXMicros.addAndGet(((x - lastMouseX) * 1000f).toInt())
        mouseDeltaYMicros.addAndGet(((lastMouseY - y) * 1000f).toInt())
        lastMouseX = x
        lastMouseY = y
    }

    fun consumeMouseDeltaX(): Float = mouseDeltaXMicros.getAndSet(0) / 1000f

    fun consumeMouseDeltaY(): Float = mouseDeltaYMicros.getAndSet(0) / 1000f

    fun consume(keyCode: Int): Boolean =
        oneShotKeys.getOrPut(keyCode) { AtomicBoolean() }.getAndSet(false)

    fun isKeyDown(keyCode: Int): Boolean = keyCode in pressedKeys

    val movingForward get() = isKeyDown(GLFW_KEY_W)
    val movingBackward get() = isKeyDown(GLFW_KEY_S)
    val strafingLeft get() = isKeyDown(GLFW_KEY_A)
    val strafingRight get() = isKeyDown(GLFW_KEY_D)
    val jumping get() = isKeyDown(GLFW_KEY_SPACE)

    val punching get() = leftMouseDown.getAndSet(false)
    val kicking get() = rightMouseDown.getAndSet(false)
    val blocking get() = isKeyDown(GLFW_KEY_LEFT_CONTROL)
    val usePotion get() = consume(GLFW_KEY_R)
    val interact get() = consume(GLFW_KEY_E)
    val startGame get() = consume(GLFW_KEY_ENTER)
}
