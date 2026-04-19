package com.glycin.springsouls.gameplay

enum class EnemyState {
    IDLE,
    ATTACK,
    HIT,
    DEATH;

    val isLooping get() = this == IDLE
    val isAction get() = this == ATTACK || this == HIT || this == DEATH
}
