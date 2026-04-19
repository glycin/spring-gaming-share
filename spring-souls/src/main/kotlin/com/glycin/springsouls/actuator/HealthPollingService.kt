package com.glycin.springsouls.actuator

import com.glycin.springsouls.render.HudRenderService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class HealthPollingService(
    private val hudRenderService: HudRenderService,
    @Value($$"${server.port:8080}") private val serverPort: Int,
) {
    private val logger = LoggerFactory.getLogger(HealthPollingService::class.java)
    private val restClient = RestClient.create()

    @Scheduled(fixedRate = 100)
    fun pollHealth() {
        try {
            val response = restClient.get()
                .uri("http://localhost:$serverPort/actuator/health")
                .retrieve()
                .body<HealthResponse>() ?: return

            val hp = response.components.playerHp
            val hpCurrent = (hp.details["hp"] as? Number)?.toFloat() ?: 0f
            val hpMax = (hp.details["maxHp"] as? Number)?.toFloat() ?: 1f
            hudRenderService.hpPercent = hpCurrent / hpMax
            hudRenderService.hpStatus = hp.status

            val stamina = response.components.stamina
            val staminaCurrent = (stamina.details["stamina"] as? String)?.toFloatOrNull() ?: 0f
            val staminaMax = (stamina.details["maxStamina"] as? String)?.toFloatOrNull() ?: 1f
            hudRenderService.staminaPercent = staminaCurrent / staminaMax
            hudRenderService.staminaStatus = stamina.status

            val potion = response.components.potion
            hudRenderService.potionCount = (potion.details["charges"] as? Number)?.toInt() ?: 0

            val bossHp = response.components.bossHp
            if (bossHp != null) {
                val bossHpCurrent = (bossHp.details["hp"] as? Number)?.toFloat() ?: 0f
                val bossHpMax = (bossHp.details["maxHp"] as? Number)?.toFloat() ?: 1f
                hudRenderService.bossHpPercent = bossHpCurrent / bossHpMax
            }
        } catch (e: Exception) {
            logger.trace("Health poll failed (server may not be ready yet): {}", e.message)
        }
    }
}
data class HealthResponse(
    val components: Components = Components(),
) {
    data class Components(
        val playerHp: ComponentHealth = ComponentHealth(),
        val stamina: ComponentHealth = ComponentHealth(),
        val potion: ComponentHealth = ComponentHealth(),
        val bossHp: ComponentHealth? = null,
    )

    data class ComponentHealth(
        val status: String = "UP",
        val details: Map<String, Any> = emptyMap(),
    )
}