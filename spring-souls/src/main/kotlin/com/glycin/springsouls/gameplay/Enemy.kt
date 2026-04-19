package com.glycin.springsouls.gameplay

import com.glycin.springsouls.render.animation.AnimationPlayer
import kotlin.math.atan2

class Enemy(
    val x: Float,
    val z: Float,
    val tileX: Int,
    val tileZ: Int,
) {
    var hp = 50
    var maxHp = 50
    var state: EnemyState = EnemyState.IDLE
    var facingYaw = 0f

    val isAlive get() = hp > 0
    val isDead get() = state == EnemyState.DEATH

    var animationPlayer: AnimationPlayer? = null

    fun faceToward(targetX: Float, targetZ: Float) {
        facingYaw = Math.toDegrees(atan2((targetZ - z).toDouble(), (targetX - x).toDouble())).toFloat()
    }

    fun takeDamage(amount: Int): Int {
        if (!isAlive) return 0
        val actual = amount.coerceAtMost(hp)
        hp -= actual
        return actual
    }
}
