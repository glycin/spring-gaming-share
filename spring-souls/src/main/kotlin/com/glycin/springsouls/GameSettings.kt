package com.glycin.springsouls

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game.settings")
data class GameSettings(
    val windowWidth: Int = 1280,
    val windowHeight: Int = 720,
    val title: String = "Spring Souls",
    val fov: Float = 70f,
    val nearPlane: Float = 0.1f,
    val farPlane: Float = 1000f,
)
