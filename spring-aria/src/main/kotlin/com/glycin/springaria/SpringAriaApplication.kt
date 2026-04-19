package com.glycin.springaria

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.map.repository.config.EnableMapRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableMapRepositories
class SpringAriaApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    runApplication<SpringAriaApplication>(*args)
}
