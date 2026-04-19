package com.glycin.springsouls

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game.environment")
data class EnvironmentSettings(
    val tileSizeX: Float = 1.0f,
    val tileSizeY: Float = 1.0f,
    val tileSizeZ: Float = 5.0f,
)
