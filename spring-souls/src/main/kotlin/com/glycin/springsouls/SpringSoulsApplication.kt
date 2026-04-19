package com.glycin.springsouls

import com.glycin.springsouls.render.RenderService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.map.repository.config.EnableMapRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import java.util.concurrent.CountDownLatch

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity(prePostEnabled = true)
@ConfigurationPropertiesScan
@EnableMapRepositories
class SpringSoulsApplication

val IS_MAC_OS: Boolean = System.getProperty("os.name").lowercase().contains("mac")

fun main(args: Array<String>) {
    if (IS_MAC_OS) {
        // On macOS, GLFW must run on the main thread.
        // Start Spring Boot in a background thread, then run the render loop here.
        var context: ConfigurableApplicationContext? = null
        val contextReady = CountDownLatch(1)
        Thread({
            context = runApplication<SpringSoulsApplication>(*args)
            contextReady.countDown()
        }, "spring-main").start()
        contextReady.await()
        context!!.getBean(RenderService::class.java).awaitAndRunOnMainThread()
    } else {
        runApplication<SpringSoulsApplication>(*args)
    }
}
