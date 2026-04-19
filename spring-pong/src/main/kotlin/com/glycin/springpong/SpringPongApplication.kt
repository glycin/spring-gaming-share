package com.glycin.springpong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringPongApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "false")
    runApplication<SpringPongApplication>(*args)
}
