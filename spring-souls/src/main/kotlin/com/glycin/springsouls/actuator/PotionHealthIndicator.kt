package com.glycin.springsouls.actuator

import com.glycin.springsouls.gameplay.PotionRepository
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("potion")
class PotionHealthIndicator(
    private val potionRepository: PotionRepository,
) : HealthIndicator {

    override fun health(): Health {
        val potions = potionRepository.findAll()
        val builder = if (potions.isNotEmpty()) Health.up() else Health.unknown()

        return builder
            .withDetail("charges", potions.size)
            .withDetail("potions", potions.map { "potion(heal=${(it.healPercent * 100).toInt()}%)" })
            .build()
    }
}
