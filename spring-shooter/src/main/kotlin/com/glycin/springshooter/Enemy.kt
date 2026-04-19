package com.glycin.springshooter

data class Enemy(
    val beanName: String,
    val beanClassName: String,
    var x: Double,
    var y: Double,
    var distance: Double = 0.0,
    var health: Int,
    val size: Double,
    val type: EnemyType,
    var alive: Boolean = true,
    var attackCooldown: Int = 0,
)
