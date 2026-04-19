package com.glycin.springsurvivors

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.map.repository.config.EnableMapRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableTransactionManagement
@EnableMethodSecurity(prePostEnabled = true)
@EnableMapRepositories(basePackages = ["com.glycin.springsurvivors.upgrades"])
class SpringSurvivorsApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    runApplication<SpringSurvivorsApplication>(*args)
}
