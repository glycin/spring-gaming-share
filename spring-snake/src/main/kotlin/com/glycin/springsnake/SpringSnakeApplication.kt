package com.glycin.springsnake

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SpringSnakeApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    runApplication<SpringSnakeApplication>(*args)
}
