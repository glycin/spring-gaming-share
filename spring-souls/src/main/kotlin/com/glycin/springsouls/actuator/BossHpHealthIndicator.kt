package com.glycin.springsouls.actuator

import com.glycin.springsouls.render.BossRenderService
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("bossHp")
class BossHpHealthIndicator(
    private val bossRenderService: BossRenderService,
) : HealthIndicator {

    override fun health(): Health {
        val boss = bossRenderService.boss
            ?: return Health.up()
                .withDetail("hp", 0)
                .withDetail("maxHp", 0)
                .withDetail("percentage", "N/A")
                .build()

        val builder = when {
            boss.isDead -> Health.down()
            !boss.isAlive -> Health.status("OUT_OF_SERVICE")
            else -> Health.up()
        }

        return builder
            .withDetail("hp", boss.hp)
            .withDetail("maxHp", boss.maxHp)
            .withDetail("percentage", "%.0f%%".format(boss.hp.toFloat() / boss.maxHp.toFloat() * 100))
            .build()
    }
}
