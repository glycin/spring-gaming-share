package com.glycin.springsouls.gameplay

import com.glycin.springsouls.render.animation.AnimationPlayer
import kotlin.math.atan2

class Boss(
    val x: Float,
    val z: Float,
) {
    var hp = 400
    var maxHp = 400
    var state: BossState = BossState.IDLE
    var facingYaw = 0f

    val isAlive get() = hp > 0
    val isDead get() = state == BossState.DEATH

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
