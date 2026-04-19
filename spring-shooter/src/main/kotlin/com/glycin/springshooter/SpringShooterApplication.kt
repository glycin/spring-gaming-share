package com.glycin.springshooter

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.CountDownLatch

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class SpringShooterApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")

    var profile = "tutorial"
    do {
        SpringApplicationContextService.nextProfile = null
        val latch = CountDownLatch(1)
        val app = SpringApplication(SpringShooterApplication::class.java)
        app.setAdditionalProfiles(profile)
        app.addListeners(ApplicationListener<ContextClosedEvent> { latch.countDown() })
        app.run(*args)
        latch.await()
        profile = SpringApplicationContextService.nextProfile ?: break
    } while (true)
}
