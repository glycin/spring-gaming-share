package com.glycin.springaria.gameplay

import com.glycin.extensions.flipHorizontal
import com.glycin.image.SpriteSheet
import java.awt.image.BufferedImage

private const val FRAME_WIDTH = 16
private const val FRAME_HEIGHT = 32

enum class PlayerState(val spriteSheetPath: String, val frameDuration: Int) {
    IDLE("sprites/player_sheet_idle.png", 15),
    WALK("sprites/player_sheet_walk.png", 8),
    JUMP("sprites/player_sheet_jump.png", 10),
    MINING("sprites/player_sheet_use.png", 6),
}

class PlayerAnimator {

    private val framesByState: Map<PlayerState, Array<BufferedImage>> = PlayerState.entries.associateWith { state ->
        SpriteSheet(state.spriteSheetPath).getRow(FRAME_WIDTH, FRAME_HEIGHT)
    }

    private val flippedFramesByState: Map<PlayerState, Array<BufferedImage>> = framesByState.mapValues { (_, frames) ->
        Array(frames.size) { i -> frames[i].flipHorizontal() }
    }

    private var state = PlayerState.IDLE
    private var facingLeft = false
    private var frameIndex = 0
    private var tickCount = 0

    fun update(movingX: Boolean, onGround: Boolean, mining: Boolean, movingLeft: Boolean, movingRight: Boolean) {
        if (movingLeft && !movingRight) facingLeft = true
        else if (movingRight && !movingLeft) facingLeft = false
        val newState = when {
            mining -> PlayerState.MINING
            !onGround -> PlayerState.JUMP
            movingX -> PlayerState.WALK
            else -> PlayerState.IDLE
        }

        if (newState != state) {
            state = newState
            frameIndex = 0
            tickCount = 0
        } else {
            tickCount++
            if (tickCount >= state.frameDuration) {
                tickCount = 0
                frameIndex = (frameIndex + 1) % frames().size
            }
        }
    }

    fun getCurrentFrame(): BufferedImage =
        (if (facingLeft) flippedFramesByState else framesByState).getValue(state)[frameIndex]

    private fun frames(): Array<BufferedImage> = framesByState.getValue(state)
}
