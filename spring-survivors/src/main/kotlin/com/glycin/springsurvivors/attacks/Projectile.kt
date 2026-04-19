package com.glycin.springsurvivors.attacks

import com.glycin.util.Vec2

data class Projectile(
    val position: Vec2,
    val direction: Vec2,
    val speed: Float,
    val size: Int,
    val isSplit: Boolean = false,
)
