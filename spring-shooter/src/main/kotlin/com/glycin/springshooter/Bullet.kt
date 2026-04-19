package com.glycin.springshooter

data class Bullet(
    var x: Double,
    var y: Double,
    val dirX: Double,
    val dirY: Double,
    var alive: Boolean = true,
)
