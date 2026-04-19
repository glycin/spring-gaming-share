package com.glycin.springsouls.gameplay

enum class PlayerState {
    IDLE,
    RUN_FORWARD, RUN_BACK, RUN_LEFT, RUN_RIGHT,
    DODGE_FORWARD, DODGE_BACK, DODGE_LEFT, DODGE_RIGHT,
    PUNCH, KICK, DRINK, HIT, DEATH;

    val isAction get() = this in ACTION_STATES
    val isDodge get() = this in DODGE_STATES
    val isMovement get() = this in MOVEMENT_STATES
    val isLooping get() = this == IDLE || isMovement

    companion object {
        private val DODGE_STATES = setOf(DODGE_FORWARD, DODGE_BACK, DODGE_LEFT, DODGE_RIGHT)
        private val ACTION_STATES = DODGE_STATES + setOf(PUNCH, KICK, DRINK, HIT, DEATH)
        private val MOVEMENT_STATES = setOf(RUN_FORWARD, RUN_BACK, RUN_LEFT, RUN_RIGHT)
    }
}
