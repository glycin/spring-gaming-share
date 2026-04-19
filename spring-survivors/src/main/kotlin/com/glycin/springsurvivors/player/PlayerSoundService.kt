package com.glycin.springsurvivors.player

import com.glycin.annotations.WithSound
import com.glycin.springsurvivors.rhythm.BeatCosmeticEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class PlayerSoundService {

    @WithSound("sounds/on_beat_success.wav")
    @EventListener
    fun onBeat(event: BeatCosmeticEvent) {}
}
