package com.glycin.springshooter

import com.glycin.image.SpringGameImage
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

const val WINDOW_WIDTH = 1200
const val WINDOW_HEIGHT = 800

private const val CROSSHAIR_SIZE = 10
private const val MIN_ENEMY_RENDER_SIZE = 8
private const val MAX_ENEMY_RENDER_SIZE = 400
private const val PROJECTION_DISTANCE = 4.0
private const val LABEL_FONT_MIN = 8
private const val LABEL_FONT_MAX = 20
private const val MAX_RAY_DEPTH = 30.0

private const val GRID_LINE_THRESHOLD = 0.03
private const val GRID_FADE_DISTANCE = 15.0

private val CEILING_COLOR = Color(45, 20, 70)
private val FLOOR_COLOR_RGB = Color(30, 12, 50).rgb
private val FLOOR_GRID_RGB = Color(70, 35, 110).rgb

class GamePanel(
    private val renderService: GameRenderService,
    private val playerService: PlayerService,
    private val enemyService: EnemyService,
    private val levelService: LevelService,
) : JPanel() {

    private val wallTexture: BufferedImage = SpringGameImage("sprites/wall-sprite.png").image
    private val wallpaperImage: BufferedImage = SpringGameImage("sprites/wallpaper.png").image
    private val wallDistances = DoubleArray(WINDOW_WIDTH) { Double.MAX_VALUE }
    private val floorBuffer = BufferedImage(WINDOW_WIDTH, WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB)
    private val floorPixels = (floorBuffer.raster.dataBuffer as java.awt.image.DataBufferInt).data
    private val rayDirXs = DoubleArray(WINDOW_WIDTH)
    private val rayDirYs = DoubleArray(WINDOW_WIDTH)

    init {
        preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
        background = Color.BLACK
        isFocusable = true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        when (renderService.gameState) {
            GameState.MENU -> paintMenu(g2)
            GameState.PLAYING -> paintGame(g2)
        }
    }

    private fun paintMenu(g2: Graphics2D) {
        val imgAspect = wallpaperImage.width.toDouble() / wallpaperImage.height
        val winAspect = WINDOW_WIDTH.toDouble() / WINDOW_HEIGHT
        val srcX: Int
        val srcY: Int
        val srcW: Int
        val srcH: Int
        if (imgAspect > winAspect) {
            srcH = wallpaperImage.height
            srcW = (srcH * winAspect).toInt()
            srcX = (wallpaperImage.width - srcW) / 2
            srcY = 0
        } else {
            srcW = wallpaperImage.width
            srcH = (srcW / winAspect).toInt()
            srcX = 0
            srcY = (wallpaperImage.height - srcH) / 2
        }
        g2.drawImage(wallpaperImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, srcX, srcY, srcX + srcW, srcY + srcH, null)

        drawCenteredOutlinedString(g2, "SPRING", Font("Monospaced", Font.BOLD, 96), WINDOW_HEIGHT / 3, Color.WHITE, 3)
        drawCenteredOutlinedString(g2, "Enterprise Evolved", Font("Monospaced", Font.PLAIN, 36), WINDOW_HEIGHT / 3 + 50, Color.WHITE, 2)
        drawCenteredOutlinedString(g2, "Press ENTER to start", Font("Monospaced", Font.PLAIN, 24), WINDOW_HEIGHT - 40, Color.LIGHT_GRAY, 2)
    }

    private fun drawCenteredOutlinedString(g2: Graphics2D, text: String, font: Font, y: Int, fill: Color, thickness: Int) {
        g2.font = font
        val x = (WINDOW_WIDTH - g2.fontMetrics.stringWidth(text)) / 2
        drawOutlinedString(g2, text, x, y, fill, thickness)
    }

    private fun drawOutlinedString(g2: Graphics2D, text: String, x: Int, y: Int, fill: Color, thickness: Int) {
        g2.color = Color.BLACK
        for (dx in -thickness..thickness) {
            for (dy in -thickness..thickness) {
                if (dx != 0 || dy != 0) {
                    g2.drawString(text, x + dx, y + dy)
                }
            }
        }
        g2.color = fill
        g2.drawString(text, x, y)
    }

    private fun paintGame(g2: Graphics2D) {
        val halfFov = playerService.fov / 2.0
        val horizon = WINDOW_HEIGHT / 2 + playerService.pitch.toInt()

        paintSkyAndFloor(g2, horizon)
        paintWalls(g2, halfFov, horizon)
        paintBullets(g2, halfFov, horizon)
        paintEnemies(g2, halfFov, horizon)
        paintPlayerSprite(g2)
        paintEnemyHit(g2)
        paintDamageFlash(g2)
        paintCrosshair(g2)
        paintKillFeed(g2)
        paintHud(g2)
    }

    private fun paintSkyAndFloor(g2: Graphics2D, horizon: Int) {
        g2.color = CEILING_COLOR
        g2.fillRect(0, 0, WINDOW_WIDTH, horizon)

        val clampedHorizon = horizon.coerceIn(0, WINDOW_HEIGHT)
        val px = playerService.x
        val py = playerService.y
        val pAngle = playerService.angle
        val halfFov = playerService.fov / 2.0
        val fov = playerService.fov

        for (screenX in 0..<WINDOW_WIDTH) {
            val rayAngle = pAngle - halfFov + (screenX.toDouble() / WINDOW_WIDTH) * fov
            rayDirXs[screenX] = cos(rayAngle)
            rayDirYs[screenX] = sin(rayAngle)
        }

        for (screenY in clampedHorizon..<WINDOW_HEIGHT) {
            val rowDist = (WINDOW_HEIGHT / 2.0) / (screenY - horizon).coerceAtLeast(1)
            val pixelOffset = screenY * WINDOW_WIDTH

            for (screenX in 0..<WINDOW_WIDTH) {
                val worldX = px + rayDirXs[screenX] * rowDist
                val worldY = py + rayDirYs[screenX] * rowDist

                val fracX = worldX - floor(worldX)
                val fracY = worldY - floor(worldY)
                val nearGrid = fracX < GRID_LINE_THRESHOLD || fracX > 1.0 - GRID_LINE_THRESHOLD ||
                    fracY < GRID_LINE_THRESHOLD || fracY > 1.0 - GRID_LINE_THRESHOLD

                floorPixels[pixelOffset + screenX] = if (nearGrid && rowDist < GRID_FADE_DISTANCE) {
                    FLOOR_GRID_RGB
                } else {
                    FLOOR_COLOR_RGB
                }
            }
        }

        g2.drawImage(floorBuffer, 0, clampedHorizon, WINDOW_WIDTH, WINDOW_HEIGHT,
            0, clampedHorizon, WINDOW_WIDTH, WINDOW_HEIGHT, null)
    }

    private fun paintWalls(g2: Graphics2D, halfFov: Double, horizon: Int) {
        val px = playerService.x
        val py = playerService.y
        val pAngle = playerService.angle

        for (screenX in 0..<WINDOW_WIDTH) {
            val rayAngle = pAngle - halfFov + (screenX.toDouble() / WINDOW_WIDTH) * playerService.fov

            val rayDirX = cos(rayAngle)
            val rayDirY = sin(rayAngle)

            // DDA setup
            var mapX = px.toInt()
            var mapY = py.toInt()

            val deltaDistX = if (rayDirX != 0.0) abs(1.0 / rayDirX) else Double.MAX_VALUE
            val deltaDistY = if (rayDirY != 0.0) abs(1.0 / rayDirY) else Double.MAX_VALUE

            val stepX: Int
            val stepY: Int
            var sideDistX: Double
            var sideDistY: Double

            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (px - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - px) * deltaDistX
            }

            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (py - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - py) * deltaDistY
            }

            // DDA
            var hitSide: Int  // 0 = vertical wall (NS), 1 = horizontal wall (EW)
            var perpWallDist: Double

            while (true) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    hitSide = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    hitSide = 1
                }

                if (levelService.isWall(mapX, mapY)) {
                    perpWallDist = if (hitSide == 0) {
                        sideDistX - deltaDistX
                    } else {
                        sideDistY - deltaDistY
                    }
                    break
                }
            }

            wallDistances[screenX] = perpWallDist

            if (perpWallDist <= 0 || perpWallDist >= MAX_RAY_DEPTH) continue

            val correctedDist = perpWallDist * cos(rayAngle - pAngle)
            val wallHeight = (WINDOW_HEIGHT / correctedDist).toInt()

            val drawStart = (horizon - wallHeight / 2).coerceAtLeast(0)
            val drawEnd = (horizon + wallHeight / 2).coerceAtMost(WINDOW_HEIGHT)

            val wallX = if (hitSide == 0) {
                (py + perpWallDist * rayDirY) - floor(py + perpWallDist * rayDirY)
            } else {
                (px + perpWallDist * rayDirX) - floor(px + perpWallDist * rayDirX)
            }

            val texX = (wallX * wallTexture.width).toInt().coerceIn(0, wallTexture.width - 1)

            g2.drawImage(
                wallTexture,
                screenX, drawStart, screenX + 1, drawEnd,
                texX, 0, texX + 1, wallTexture.height,
                null,
            )

            val darkness = (min(perpWallDist / MAX_RAY_DEPTH, 0.8) * 255).toInt()
            val sideDarkness = if (hitSide == 1) 50 else 0
            val alpha = (darkness + sideDarkness).coerceIn(0, 255)
            g2.color = Color(0, 0, 0, alpha)
            g2.drawLine(screenX, drawStart, screenX, drawEnd)
        }
    }

    private fun normalizeAngle(angle: Double): Double {
        var a = angle
        while (a > Math.PI) a -= 2 * Math.PI
        while (a < -Math.PI) a += 2 * Math.PI
        return a
    }

    private fun paintEnemies(g2: Graphics2D, halfFov: Double, horizon: Int) {
        val visible = enemyService.getVisibleEnemies(playerService.x, playerService.y, playerService.angle, playerService.fov)

        for (enemy in visible) {
            val dx = enemy.x - playerService.x
            val dy = enemy.y - playerService.y
            val relativeAngle = normalizeAngle(atan2(dy, dx) - playerService.angle)

            if (relativeAngle < -halfFov || relativeAngle > halfFov) continue

            val screenX = (WINDOW_WIDTH / 2.0 + (relativeAngle / halfFov) * (WINDOW_WIDTH / 2.0)).toInt()

            val correctedDist = enemy.distance * cos(relativeAngle)
            val scale = PROJECTION_DISTANCE / correctedDist * enemy.size
            val renderHeight = (WINDOW_HEIGHT * scale * 0.2).toInt().coerceIn(MIN_ENEMY_RENDER_SIZE, MAX_ENEMY_RENDER_SIZE)

            val centerCol = screenX.coerceIn(0, WINDOW_WIDTH - 1)
            if (enemy.distance > wallDistances[centerCol]) continue

            val facingRight = relativeAngle <= 0
            val sprite = enemyService.getSprite(enemy.type, facingRight)

            val aspectRatio = sprite.width.toDouble() / sprite.height
            val spriteDrawWidth = (renderHeight * aspectRatio).toInt()
            val spriteLeft = screenX - spriteDrawWidth / 2
            val spriteTop = horizon - renderHeight / 4

            g2.drawImage(sprite, spriteLeft, spriteTop, spriteDrawWidth, renderHeight, null)

            val fontSize = (LABEL_FONT_MAX * scale).toInt().coerceIn(LABEL_FONT_MIN, LABEL_FONT_MAX)
            g2.font = Font("Monospaced", Font.BOLD, fontSize)
            g2.color = Color.WHITE
            val label = enemy.beanName
            val labelWidth = g2.fontMetrics.stringWidth(label)
            g2.drawString(label, screenX - labelWidth / 2, spriteTop - 4)
        }
    }

    private fun paintBullets(g2: Graphics2D, halfFov: Double, horizon: Int) {
        val sprite = enemyService.bulletSprite

        for (bullet in enemyService.bullets) {
            if (!bullet.alive) continue

            val dx = bullet.x - playerService.x
            val dy = bullet.y - playerService.y
            val relativeAngle = normalizeAngle(atan2(dy, dx) - playerService.angle)

            if (relativeAngle < -halfFov || relativeAngle > halfFov) continue

            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 0.1) continue

            val screenX = (WINDOW_WIDTH / 2.0 + (relativeAngle / halfFov) * (WINDOW_WIDTH / 2.0)).toInt()

            val centerCol = screenX.coerceIn(0, WINDOW_WIDTH - 1)
            if (dist > wallDistances[centerCol]) continue

            val correctedDist = dist * cos(relativeAngle)
            val scale = PROJECTION_DISTANCE / correctedDist
            val bulletSize = (WINDOW_HEIGHT * scale * 0.03).toInt().coerceIn(4, 40)

            val bulletY = horizon + bulletSize / 2
            g2.drawImage(sprite, screenX - bulletSize / 2, bulletY - bulletSize / 2, bulletSize, bulletSize, null)
        }
    }

    private fun paintPlayerSprite(g2: Graphics2D) {
        val playerSprite = playerService.sprite
        val scale = WINDOW_WIDTH.toDouble() / playerSprite.width * 0.6
        val drawWidth = (playerSprite.width * scale).toInt()
        val drawHeight = (playerSprite.height * scale).toInt()
        val weaponDrawX = WINDOW_WIDTH - drawWidth
        val weaponDrawY = WINDOW_HEIGHT - drawHeight

        if (playerService.muzzleFlashTimer > 0) {
            val sprite = playerService.muzzleFlashSprite
            val flashSize = 60 + playerService.muzzleFlashTimer * 15
            val cx = weaponDrawX + (WINDOW_WIDTH / 4) - 10
            val flashY = WINDOW_HEIGHT - 215

            g2.drawImage(sprite, cx - flashSize / 2, flashY - flashSize / 2, flashSize, flashSize, null)
        }

        val bobOffset = if (playerService.isMoving()) {
            (sin(System.currentTimeMillis() / 100.0) * 4).toInt()
        } else {
            0
        }

        g2.drawImage(playerSprite, weaponDrawX, weaponDrawY + bobOffset, drawWidth, drawHeight, null)

        val ammoText = if (playerService.reloading) "--" else "${playerService.ammo}"
        g2.font = Font("Monospaced", Font.BOLD, 32)
        g2.color = Color(0, 141, 255)
        val ammoWidth = g2.fontMetrics.stringWidth(ammoText)
        val ammoX = weaponDrawX + drawWidth / 2 + ammoWidth + 65
        val ammoY = weaponDrawY + drawHeight - 130 + bobOffset
        g2.drawString(ammoText, ammoX, ammoY)
    }

    private fun paintEnemyHit(g2: Graphics2D) {
        if (playerService.muzzleFlashTimer <= 0 || !playerService.lastShotHit) return
        val sprite = playerService.enemyHitSprite
        val dist = enemyService.lastHitDistance.coerceAtLeast(0.5)
        val scale = PROJECTION_DISTANCE / dist
        val hitSize = ((30 + playerService.muzzleFlashTimer * 6) * scale).toInt().coerceIn(8, 120)
        val cx = WINDOW_WIDTH / 2
        val cy = WINDOW_HEIGHT / 2

        g2.drawImage(sprite, cx - hitSize / 2, cy - hitSize / 2, hitSize, hitSize, null)
    }

    private fun paintDamageFlash(g2: Graphics2D) {
        if (playerService.damageFlashTimer <= 0) return
        val alpha = (playerService.damageFlashTimer / 15.0 * 120).toInt().coerceIn(0, 120)
        g2.color = Color(255, 0, 0, alpha)
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT)
    }

    private fun paintCrosshair(g2: Graphics2D) {
        val cx = WINDOW_WIDTH / 2
        val cy = WINDOW_HEIGHT / 2

        g2.color = Color.WHITE
        g2.drawLine(cx - CROSSHAIR_SIZE, cy, cx - 3, cy)
        g2.drawLine(cx + 3, cy, cx + CROSSHAIR_SIZE, cy)
        g2.drawLine(cx, cy - CROSSHAIR_SIZE, cx, cy - 3)
        g2.drawLine(cx, cy + 3, cx, cy + CROSSHAIR_SIZE)
    }

    private fun paintKillFeed(g2: Graphics2D) {
        if (enemyService.killMessageTimer <= 0) return
        val message = enemyService.lastKillMessage ?: return
        val alpha = ((enemyService.killMessageTimer / 180.0) * 255).toInt().coerceIn(0, 255)
        g2.color = Color(255, 0, 0, alpha)
        g2.font = Font("Monospaced", Font.BOLD, 14)
        val msgWidth = g2.fontMetrics.stringWidth(message)
        g2.drawString(message, WINDOW_WIDTH - msgWidth - 10, 25)
    }

    private fun paintHud(g2: Graphics2D) {
        g2.font = Font("Monospaced", Font.BOLD, 16)

        g2.color = Color.WHITE
        val aliveCount = enemyService.aliveCount()
        val spawnedCount = enemyService.spawnedCount()
        val totalCount = enemyService.enemies.size
        g2.drawString("Beans: $aliveCount / $spawnedCount  | Remaining: ${totalCount - spawnedCount}", 10, WINDOW_HEIGHT - 30)

        g2.font = Font("Monospaced", Font.PLAIN, 12)
        g2.drawString("WASD: move | Mouse: look | Click/Space: shoot | R: reload", WINDOW_WIDTH - 430, WINDOW_HEIGHT - 10)

        if (renderService.isTutorial) {
            g2.font = Font("Monospaced", Font.BOLD, 18)
            val hint = "Press P to end the tutorial"
            val hintWidth = g2.fontMetrics.stringWidth(hint)
            g2.color = Color(255, 255, 100)
            g2.drawString(hint, WINDOW_WIDTH - hintWidth - 15, 25)
        }
    }
}
