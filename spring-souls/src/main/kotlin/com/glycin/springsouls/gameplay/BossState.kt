package com.glycin.springsouls.gameplay

enum class BossState {
    IDLE,
    SHORT_RANGE_ATTACK,
    LONG_RANGE_ATTACK,
    HIT,
    DEATH;

    val isLooping get() = this == IDLE
    val isAction get() = this != IDLE
}
