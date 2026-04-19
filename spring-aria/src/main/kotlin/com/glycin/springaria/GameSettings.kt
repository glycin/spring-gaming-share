package com.glycin.springaria

import org.springframework.boot.context.properties.ConfigurationProperties

private const val DOUBLE_STEP = 0.5

@ConfigurationProperties(prefix = "game.settings")
data class GameSettings(
    val windowWidth: Int = 1200,
    val windowHeight: Int = 800,
    var soundMuted: Boolean = false,
    var jumpForce: Double = -5.5,
    var playerSpeed: Double = 2.0,
) {

    val editableSettings: List<SettingEntry> get() = listOf(
        SettingEntry("Sound Muted", "game.settings.sound-muted", { soundMuted }, { soundMuted = !soundMuted }),
        SettingEntry("Jump Force", "game.settings.jump-force", { jumpForce }, { dir -> jumpForce += dir * DOUBLE_STEP }),
        SettingEntry("Player Speed", "game.settings.player-speed", { playerSpeed }, { dir -> playerSpeed += dir * DOUBLE_STEP }),
    )
}

class SettingEntry(
    val label: String,
    val yamlKey: String,
    val get: () -> Any,
    private val onAdjust: (Int) -> Unit,
) {
    fun adjust(direction: Int) = onAdjust(direction)
}
