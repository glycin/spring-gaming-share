package com.glycin.springpong

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

private val logger = LoggerFactory.getLogger(CoolBeanRegistrar::class.java)

@Configuration
@Import(CoolBeanRegistrar::class)
class CoolBeanConfiguration

class CoolBeanRegistrar : BeanRegistrarDsl({
    val playerWon = PongGame.play()
    if (playerWon) {
        logger.info("Player wins! Registering SuperCoolService")
        registerBean<SuperCoolService>()
    } else {
        logger.info("AI wins! Registering KindaCoolService")
        registerBean<KindaCoolService>()
    }
})