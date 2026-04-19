package com.glycin.springsnake

import com.glycin.springsnake.FoodRepository.Companion.GRID_HEIGHT
import com.glycin.springsnake.FoodRepository.Companion.GRID_WIDTH
import com.glycin.util.Vec2
import org.springframework.stereotype.Service

@Service
class SnakeService {

    var direction = Vec2.right
    val body = mutableListOf(
        Vec2(5f, 5f),
        Vec2(4f, 5f),
        Vec2(3f, 5f),
    )

    val head: Vec2 get() = body.first()

    fun move(grew: Boolean) {
        val newHead = wrap(head + direction)
        body.add(0, newHead)
        if (!grew) {
            body.removeLast()
        }
    }

    fun nextHeadPos(): Vec2 = wrap(head + direction)

    private fun wrap(pos: Vec2): Vec2 = Vec2(
        (pos.x + GRID_WIDTH) % GRID_WIDTH,
        (pos.y + GRID_HEIGHT) % GRID_HEIGHT,
    )

    fun changeDirection(newDirection: Vec2) {
        if (direction + newDirection != Vec2.zero) {
            direction = newDirection
        }
    }

    fun collidesWithSelf(): Boolean = body.drop(1).any { it == head }
}
