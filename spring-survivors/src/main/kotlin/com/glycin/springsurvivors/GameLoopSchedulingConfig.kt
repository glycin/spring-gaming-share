package com.glycin.springsurvivors

import com.glycin.annotations.MillisCronTrigger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class GameLoopSchedulingConfig(
    private val gameLoop: GameLoop,
    @Value($$"${game.settings.cron}") private val cron: String,
) : SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.addTriggerTask({ gameLoop.tick() },
            MillisCronTrigger(cron),
        )
    }
}
