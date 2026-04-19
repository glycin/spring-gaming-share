package com.glycin.springsouls

import com.glycin.annotations.WithLoopingSound
import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.security.PlayerAuthentication
import org.springframework.stereotype.Service

@Service
class AudioService(
    private val player: Player,
    private val playerAuthentication: PlayerAuthentication,
) {
    fun shouldStopBackgroundTheme(): Boolean =
        !player.isAlive || playerAuthentication.authorities.any { it.authority == "BOSS" }

    @WithLoopingSound("audio/background_theme.wav", stopCondition = "shouldStopBackgroundTheme", volume = 0.3f)
    fun playBackgroundTheme() { }
}
