package com.glycin.springsouls.actuator

import com.glycin.springsouls.gameplay.Player
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("stamina")
class StaminaHealthIndicator(
    private val player: Player,
) : HealthIndicator {

    override fun health(): Health {
        val builder = when {
            player.stamina <= 0f -> Health.status("OUT_OF_SERVICE")
            else -> Health.up()
        }

        return builder
            .withDetail("stamina", "%.1f".format(player.stamina))
            .withDetail("maxStamina", "%.1f".format(player.maxStamina))
            .withDetail("percentage", "%.0f%%".format(player.staminaPercent * 100))
            .build()
    }
}
