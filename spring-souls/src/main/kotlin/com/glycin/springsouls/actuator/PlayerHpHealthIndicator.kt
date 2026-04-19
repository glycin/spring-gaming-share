package com.glycin.springsouls.actuator

import com.glycin.springsouls.gameplay.Player
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("playerHp")
class PlayerHpHealthIndicator(
    private val player: Player,
) : HealthIndicator {

    override fun health(): Health {
        val builder = when {
            !player.isAlive -> Health.down()
            player.hpPercent <= 0.0f -> Health.status("OUT_OF_SERVICE")
            else -> Health.up()
        }

        return builder
            .withDetail("hp", player.hp)
            .withDetail("maxHp", player.maxHp)
            .withDetail("percentage", "%.0f%%".format(player.hpPercent * 100))
            .build()
    }
}
