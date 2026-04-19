package com.glycin.springsurvivors.rhythm

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.image.SpringGameImage
import com.glycin.springsurvivors.GameSettings
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import org.springframework.context.event.EventListener

private const val BASE_SIZE = 64
private const val PULSE_SCALE = 0.3f
private const val GLOW_EXTRA = 20
private const val DECAY_SPEED = 5f
private const val BAR_MARGIN = 20

@GameManager
class BeatIndicator(
    private val gameSettings: GameSettings,
) {

    private val leafImage: BufferedImage = SpringGameImage("sprites/leaf.png").image
    private var pulseAmount = 0f

    @EventListener
    fun onBeat(event: BeatCosmeticEvent) {
        pulseAmount = 1f
    }

    @Update(order = 50)
    fun update() {
        pulseAmount = (pulseAmount - DECAY_SPEED * 0.016f).coerceAtLeast(0f)
    }

    @Renderer
    fun render(g: Graphics2D) {
        val centerX = gameSettings.windowWidth / 2
        val centerY = gameSettings.windowHeight - BAR_MARGIN - 32
        val scale = 1f + pulseAmount * PULSE_SCALE
        val drawSize = (BASE_SIZE * scale).toInt()

        // Glow
        if (pulseAmount > 0f) {
            val glowAlpha = (pulseAmount * 120).toInt().coerceIn(0, 255)
            val glowSize = drawSize + GLOW_EXTRA
            g.color = Color(100, 255, 100, glowAlpha)
            g.fillOval(
                centerX - glowSize / 2,
                centerY - glowSize / 2,
                glowSize,
                glowSize,
            )
        }

        // Leaf
        val oldTransform = g.transform
        val tx = AffineTransform(oldTransform)
        tx.translate(centerX.toDouble(), centerY.toDouble())
        tx.scale(scale.toDouble(), scale.toDouble())
        tx.translate((-BASE_SIZE / 2).toDouble(), (-BASE_SIZE / 2).toDouble())
        g.transform = tx
        g.drawImage(leafImage, 0, 0, BASE_SIZE, BASE_SIZE, null)
        g.transform = oldTransform
    }
}
