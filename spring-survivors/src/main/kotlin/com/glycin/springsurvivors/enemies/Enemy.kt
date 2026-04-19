package com.glycin.springsurvivors.enemies

import com.glycin.util.GridPos
import java.awt.image.BufferedImage

private const val LERP_SPEED = 0.12f

open class Enemy(
    var gridPos: GridPos,
    var hp: Float,
    val type: EnemyType = EnemyType.SKULL,
) {
    val beatSkip: Int get() = type.beatSkip
    val contactDamage: Float get() = type.contactDamage
    val xpValue: Int get() = type.xpValue

    var beatCounter = 0
    private var frameIndex = 0

    var previousGridPos: GridPos = gridPos
    var lerpProgress: Float = 1f

    fun moveTo(target: GridPos) {
        previousGridPos = gridPos
        gridPos = target
        lerpProgress = 0f
    }

    fun updateLerp() {
        if (lerpProgress < 1f) {
            lerpProgress = (lerpProgress + LERP_SPEED).coerceAtMost(1f)
        }
    }

    fun nextFrame() {
        val frames = framesFor(type)
        if (frames.isNotEmpty()) {
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    fun currentFrame(): BufferedImage? = framesFor(type).getOrNull(frameIndex)
}
