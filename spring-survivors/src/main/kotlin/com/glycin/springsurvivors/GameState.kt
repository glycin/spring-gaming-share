package com.glycin.springsurvivors

import org.springframework.stereotype.Component

@Component
class GameState(
    gameSettings: GameSettings,
) {
    var projectileSpeedMultiplier = 1f
    var projectileSizeMultiplier = 1f
    var xpGainMultiplier = 1f
    var spawnIntervalMultiplier = 1f
    var damageMultiplier = 1f
    var extraBeatSkip = 0
    var maxHp: Float = gameSettings.player.maxHp
    var piercingShots = false
    var frozen = false
    var gameOver = false
}