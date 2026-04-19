package com.glycin.springsurvivors.enemies

import com.glycin.image.SpriteSheet
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.image.BufferedImage

enum class EnemyType(
    val baseHp: Float,
    val contactDamage: Float,
    val beatSkip: Int,
    val xpValue: Int,
    val tintColor: Color?,
    val sizeMultiplier: Float,
) {
    SKULL(1f, 1f, 3, 10, null, 1f),
    SPEEDER(0.5f, 0.5f, 1, 5, Color(0, 200, 0, 120), 0.7f),
    BRUTE(4f, 2f, 5, 25, Color(200, 0, 0, 120), 1.3f),
    WRAITH(2f, 1.5f, 2, 15, Color(150, 0, 200, 120), 1.0f),
    TITAN(8f, 3f, 7, 50, Color(200, 100, 0, 120), 1.5f),
}

private val frameCache = mutableMapOf<EnemyType, Array<BufferedImage>>()

fun initAllEnemyFrames(tileSize: Int) {
    val raw = SpriteSheet("sprites/enemy_1.png").getRow(256, 256)
    for (type in EnemyType.entries) {
        val size = (tileSize * type.sizeMultiplier).toInt()
        frameCache[type] = raw.map { frame ->
            BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).also { scaled ->
                val g = scaled.createGraphics()
                g.drawImage(frame, 0, 0, size, size, null)
                type.tintColor?.let { tint ->
                    g.composite = AlphaComposite.SrcAtop
                    g.color = tint
                    g.fillRect(0, 0, size, size)
                }
                g.dispose()
            }
        }.toTypedArray()
    }
}

fun framesFor(type: EnemyType): Array<BufferedImage> = frameCache[type] ?: emptyArray()
