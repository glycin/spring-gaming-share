package com.glycin.springaria.gameplay

import com.glycin.annotations.WithSound
import org.springframework.stereotype.Service

@Service
class PlayerActionService {

    @WithSound("sounds/jump.wav")
    fun jump(onJump: () -> Unit) {
        onJump()
    }

    @WithSound("sounds/spring.wav")
    fun shoot(onShoot: () -> Unit) {
        onShoot()
    }

    @WithSound("sounds/data.wav")
    fun sprayLava(onSpray: () -> Unit) {
        onSpray()
    }

    @WithSound("sounds/data.wav")
    fun shootMissile(onShoot: () -> Unit) {
        onShoot()
    }

    @WithSound("sounds/explosion.wav")
    fun explode() {}
}
