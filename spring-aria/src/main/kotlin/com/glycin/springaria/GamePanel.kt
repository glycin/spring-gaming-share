package com.glycin.springaria

import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_HEIGHT
import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_WIDTH
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_PIXELS
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_PIXELS
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import com.glycin.image.SpringGameImage
import com.glycin.image.SpriteSheet
import com.glycin.springaria.gameplay.HotbarItem
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val CAVE_BG_COLOR = Color(20, 15, 10)
private val HOTBAR_LABEL_FONT = Font("Monospaced", Font.BOLD, 11)
private val HOTBAR_KEY_FONT = Font("Monospaced", Font.PLAIN, 10)
private val SWITCH_LABEL_FONT = Font("Monospaced", Font.BOLD, 16)
private val BULLET_COLOR = Color(255, 220, 50)
private val ENEMY_MISSILE_COLOR = Color(180, 50, 50)
private const val EXPLOSION_FRAME_WIDTH = 137
private const val EXPLOSION_FRAME_HEIGHT = 200

class GamePanel(
    private val gameSettings: GameSettings,
    private val renderService: GameRenderService,
) : JPanel() {

    private var mapImage: BufferedImage? = null
    private val heartImage = SpringGameImage("sprites/heart.png").image
    private val axeSheet = SpriteSheet("sprites/axes.png")
    private val hammerImage = axeSheet.getSprite(0, 0, 32, 64)
    private val pickaxeImage = axeSheet.getSprite(32, 0, 32, 64)
    private val uziImage = SpringGameImage("sprites/uzi.png").image
    private val soakerImage = SpringGameImage("sprites/soaker.png").image
    private val bazookaImage = SpringGameImage("sprites/bazooka.png").image
    private val mainMenuBgImage = SpringGameImage("sprites/main_menu_bg.png").image
    private val enemyImage = SpringGameImage("sprites/enemy.png").image
    private val leafImage = SpringGameImage("sprites/leaf.png").image
    private val idleFrames = SpriteSheet("sprites/player_sheet_idle.png").getRow(16, 32)
    private var menuTick = 0L
    private var menuIdleFrame = 0
    private var menuIdleTick = 0
    private val explosionFrames = SpriteSheet("sprites/explosion.png").getRow(EXPLOSION_FRAME_WIDTH, EXPLOSION_FRAME_HEIGHT)
    private val explosionFrameDuration = 1000L / explosionFrames.size
    private val hotbarIcons = mapOf(
        HotbarItem.PICKAXE to pickaxeImage,
        HotbarItem.HAMMER to hammerImage,
        HotbarItem.UZI to uziImage,
        HotbarItem.SOAKER to soakerImage,
        HotbarItem.BAZOOKA to bazookaImage,
    )

    init {
        preferredSize = Dimension(gameSettings.windowWidth, gameSettings.windowHeight)
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
            GameState.MAP -> paintMap(g2)
            GameState.SETTINGS -> paintSettings(g2)
        }
    }

    private fun paintMenu(g2: Graphics2D) {
        val w = gameSettings.windowWidth
        val h = gameSettings.windowHeight
        menuTick++

        g2.drawImage(mainMenuBgImage, 0, 0, w, h, null)

        // Floating leaves
        drawMenuLeaves(g2, w, h)

        // Idle player sprite in center
        menuIdleTick++
        if (menuIdleTick >= 15) {
            menuIdleTick = 0
            menuIdleFrame = (menuIdleFrame + 1) % idleFrames.size
        }
        val spriteW = 64
        val spriteH = 128
        g2.drawImage(idleFrames[menuIdleFrame], w / 2 - spriteW / 2, h / 2 - spriteH / 2 + 220, spriteW, spriteH, null)

        drawCenteredOutlinedString(g2, "SPRINGCRAFT", Font("Monospaced", Font.BOLD, 96), h / 3 - 40, Color.WHITE, 3)
        drawCenteredOutlinedString(g2, "Press ENTER to start", Font("Monospaced", Font.PLAIN, 24), h - 80, Color.LIGHT_GRAY, 2)
    }

    private fun drawMenuLeaves(g2: Graphics2D, w: Int, h: Int) {
        val leafW = 32
        val leafH = 32
        val leafCount = 20
        for (i in 0..<leafCount) {
            val baseX = ((i + 1).toDouble() / (leafCount + 1) * w).toInt()
            val phase = i * 0.8
            val bobY = (sin((menuTick * 0.02) + phase) * 20).toInt()
            val baseY = h / 3 + 80 + i % 3 * 60
            g2.drawImage(leafImage, baseX - leafW / 2, baseY + bobY - leafH / 2, leafW, leafH, null)
        }
    }

    private fun paintGame(g2: Graphics2D) {
        val camera = renderService.camera
        val world = renderService.world
        val player = renderService.player

        g2.color = CAVE_BG_COLOR
        g2.fillRect(0, 0, gameSettings.windowWidth, gameSettings.windowHeight)

        // Render visible chunks
        val visibleChunks = world.getVisibleChunks(camera)
        for (chunk in visibleChunks) {
            val worldPixelX = chunk.chunkX * CHUNK_PIXEL_WIDTH
            val worldPixelY = chunk.chunkY * CHUNK_PIXEL_HEIGHT

            val screenX = camera.worldToScreenX(worldPixelX.toDouble()).toInt()
            val screenY = camera.worldToScreenY(worldPixelY.toDouble()).toInt()
            val scaledWidth = (CHUNK_PIXEL_WIDTH * camera.zoom).toInt()
            val scaledHeight = (CHUNK_PIXEL_HEIGHT * camera.zoom).toInt()

            g2.drawImage(chunk.getImage(), screenX, screenY, scaledWidth, scaledHeight, null)
        }

        val playerScreenX = camera.worldToScreenX(player.x).toInt()
        val playerScreenY = camera.worldToScreenY(player.y).toInt()
        val playerWidth = (player.widthInPixels * camera.zoom).toInt()
        val playerHeight = (player.heightInPixels * camera.zoom).toInt()

        drawPlayer(g2, playerScreenX, playerScreenY, playerWidth, playerHeight)
        drawSwingAnimation(g2, playerScreenX, playerScreenY, playerWidth, playerHeight)
        drawWeapon(g2, playerScreenX, playerScreenY, playerWidth, playerHeight)
        drawEnemies(g2)
        drawBullets(g2)
        drawPlayerMissiles(g2)
        drawEnemyMissiles(g2)
        drawPlayerExplosions(g2)
        drawEnemyExplosions(g2)
        drawCaveLabel(g2)
        drawDamageFlash(g2, player.lastDamageTime)
        drawHearts(g2, player.hp)
        drawHotbar(g2)
    }

    private fun drawPlayer(g2: Graphics2D, screenX: Int, screenY: Int, width: Int, height: Int) {
        val camera = renderService.camera
        val spriteExtraW = (5 * camera.zoom).toInt()
        val spriteExtraH = (8 * camera.zoom).toInt()
        val spriteW = width + spriteExtraW
        val spriteH = height + spriteExtraH
        g2.drawImage(
            renderService.player.getCurrentFrame(),
            screenX - (spriteW - width) / 2,
            screenY - (spriteH - height),
            spriteW,
            spriteH,
            null,
        )
    }

    private fun drawSwingAnimation(g2: Graphics2D, playerScreenX: Int, playerScreenY: Int, playerWidth: Int, playerHeight: Int) {
        val swingTime = renderService.swingTime
        if (swingTime <= 0) return

        val swingElapsed = System.currentTimeMillis() - swingTime
        val swingDuration = 200L
        if (swingElapsed >= swingDuration) return

        val toolSprite = hotbarIcons[renderService.hotbar.selectedItem] ?: return

        val progress = swingElapsed.toDouble() / swingDuration
        val playerCenterX = playerScreenX + playerWidth / 2
        val playerCenterY = playerScreenY + playerHeight / 2
        val targetX = renderService.swingTargetScreenX
        val targetY = renderService.swingTargetScreenY

        val angle = atan2((targetY - playerCenterY).toDouble(), (targetX - playerCenterX).toDouble())
        val swingAngle = angle + (1.0 - progress) * 0.8 - 0.4

        val dist = 25.0
        val pickX = (playerCenterX + cos(swingAngle) * dist).toInt()
        val pickY = (playerCenterY + sin(swingAngle) * dist).toInt()

        val oldTransform = g2.transform
        g2.translate(pickX, pickY)
        g2.rotate(swingAngle + Math.PI / 2)
        g2.drawImage(toolSprite, -8, -16, 16, 32, null)
        g2.transform = oldTransform
    }

    private fun drawWeapon(g2: Graphics2D, playerScreenX: Int, playerScreenY: Int, playerWidth: Int, playerHeight: Int) {
        val selectedItem = renderService.hotbar.selectedItem
        if (!selectedItem.isWeapon) return

        val weaponSprite = hotbarIcons[selectedItem] ?: return

        val playerCenterX = playerScreenX + playerWidth / 2
        val playerCenterY = playerScreenY + playerHeight / 2
        val mouseX = renderService.mouseScreenX
        val mouseY = renderService.mouseScreenY

        val angle = atan2((mouseY - playerCenterY).toDouble(), (mouseX - playerCenterX).toDouble())
        val facingLeft = mouseX < playerCenterX

        val dist = 3.0
        val weaponX = (playerCenterX + cos(angle) * dist).toInt()
        val weaponY = (playerCenterY + sin(angle) * dist).toInt()

        val oldTransform = g2.transform
        g2.translate(weaponX, weaponY)
        g2.rotate(angle)
        if (facingLeft) {
            g2.scale(1.0, -1.0)
        }
        g2.drawImage(weaponSprite, -4, -8, 28, 16, null)
        g2.transform = oldTransform
    }

    private fun drawEnemies(g2: Graphics2D) {
        val camera = renderService.camera
        val enemyService = renderService.enemyService
        val w = (enemyService.widthInPixels * camera.zoom).toInt() * 2
        val h = (enemyService.heightInPixels * camera.zoom).toInt()
        for (enemy in enemyService.enemies) {
            val sx = camera.worldToScreenX(enemy.x).toInt()
            val sy = camera.worldToScreenY(enemy.y).toInt()
            g2.drawImage(enemyImage, sx, sy, w, h, null)
        }
    }

    private fun drawBullets(g2: Graphics2D) {
        val camera = renderService.camera
        g2.color = BULLET_COLOR
        for (bullet in renderService.bulletService.bullets) {
            val sx = camera.worldToScreenX(bullet.x).toInt()
            val sy = camera.worldToScreenY(bullet.y).toInt()
            val size = (3 * camera.zoom).toInt().coerceAtLeast(2)
            g2.fillRect(sx - size / 2, sy - size / 2, size, size)
        }
    }

    private fun drawPlayerMissiles(g2: Graphics2D) {
        for (m in renderService.playerMissileService.missiles) drawMissile(g2, m.x, m.y, 4, Color.RED)
    }

    private fun drawEnemyMissiles(g2: Graphics2D) {
        for (m in renderService.enemyMissileService.missiles) drawMissile(g2, m.x, m.y, 3, ENEMY_MISSILE_COLOR)
    }

    private fun drawMissile(g2: Graphics2D, worldX: Double, worldY: Double, baseRadius: Int, color: Color) {
        val camera = renderService.camera
        val sx = camera.worldToScreenX(worldX).toInt()
        val sy = camera.worldToScreenY(worldY).toInt()
        val radius = (baseRadius * camera.zoom).toInt().coerceAtLeast(2)
        g2.color = Color.DARK_GRAY
        g2.fillOval(sx - radius, sy - radius, radius * 2, radius * 2)
        g2.color = color
        g2.fillOval(sx - radius + 1, sy - radius + 1, radius * 2 - 2, radius * 2 - 2)
    }

    private fun drawPlayerExplosions(g2: Graphics2D) {
        for (e in renderService.playerMissileService.explosions) drawExplosion(g2, e.worldX, e.worldY, e.startTime, 1.0)
    }

    private fun drawEnemyExplosions(g2: Graphics2D) {
        for (e in renderService.enemyMissileService.explosions) drawExplosion(g2, e.worldX, e.worldY, e.startTime, 0.5)
    }

    private fun drawExplosion(g2: Graphics2D, worldX: Double, worldY: Double, startTime: Long, scale: Double) {
        val elapsed = System.currentTimeMillis() - startTime
        val frameIndex = (elapsed / explosionFrameDuration).toInt()
        if (frameIndex >= explosionFrames.size) return

        val camera = renderService.camera
        val frame = explosionFrames[frameIndex]
        val sx = camera.worldToScreenX(worldX).toInt()
        val sy = camera.worldToScreenY(worldY).toInt()
        val w = (EXPLOSION_FRAME_WIDTH * scale * camera.zoom).toInt()
        val h = (EXPLOSION_FRAME_HEIGHT * scale * camera.zoom).toInt()
        g2.drawImage(frame, sx - w / 2, sy - h / 2, w, h, null)
    }

    private fun drawCaveLabel(g2: Graphics2D) {
        val labelText = renderService.caveLabelText ?: return
        val elapsed = System.currentTimeMillis() - renderService.caveLabelTime
        val displayDuration = 3000L
        if (elapsed >= displayDuration) return

        val alpha = if (elapsed > displayDuration - 1000) {
            ((displayDuration - elapsed) / 1000.0 * 255).toInt().coerceIn(0, 255)
        } else {
            255
        }
        val labelColor = Color(255, 255, 255, alpha)
        drawCenteredOutlinedString(g2, "Entering Cave:", Font("Monospaced", Font.BOLD, 28), 50, labelColor, 2)
        drawCenteredOutlinedString(g2, labelText, Font("Monospaced", Font.BOLD, 36), 90, labelColor, 2)
    }

    private fun drawDamageFlash(g2: Graphics2D, lastDamageTime: Long) {
        val elapsed = System.currentTimeMillis() - lastDamageTime
        val flashDuration = 300L
        if (elapsed >= flashDuration) return

        val alpha = ((1.0 - elapsed.toDouble() / flashDuration) * 80).toInt()
        g2.color = Color(255, 0, 0, alpha)
        g2.fillRect(0, 0, gameSettings.windowWidth, gameSettings.windowHeight)
    }

    private fun drawHearts(g2: Graphics2D, hp: Int) {
        val heartSize = 24
        val heartPadding = 4
        for (i in 0..<hp) {
            g2.drawImage(heartImage, 10 + i * (heartSize + heartPadding), 10, heartSize, heartSize, null)
        }
    }

    private fun drawHotbar(g2: Graphics2D) {
        val hotbar = renderService.hotbar
        val slotWidth = 52
        val slotHeight = 52
        val slotGap = 8
        val hotbarWidth = hotbar.items.size * slotWidth + (hotbar.items.size - 1) * slotGap
        val hotbarX = (gameSettings.windowWidth - hotbarWidth) / 2
        val hotbarY = gameSettings.windowHeight - slotHeight - 12

        for ((i, item) in hotbar.items.withIndex()) {
            val sx = hotbarX + i * (slotWidth + slotGap)
            val selected = i == hotbar.selectedIndex

            g2.color = if (selected) Color(255, 255, 255, 60) else Color(0, 0, 0, 120)
            g2.fillRect(sx, hotbarY, slotWidth, slotHeight)

            g2.color = if (selected) Color.WHITE else Color(100, 100, 100)
            g2.drawRect(sx, hotbarY, slotWidth, slotHeight)

            val icon = hotbarIcons[item]
            if (icon != null) {
                val iconW = 20
                val iconH = 40
                g2.drawImage(icon, sx + (slotWidth - iconW) / 2, hotbarY + (slotHeight - iconH) / 2, iconW, iconH, null)
            } else {
                g2.font = HOTBAR_LABEL_FONT
                g2.color = if (selected) Color.WHITE else Color.LIGHT_GRAY
                val labelWidth = g2.fontMetrics.stringWidth(item.label)
                g2.drawString(item.label, sx + (slotWidth - labelWidth) / 2, hotbarY + slotHeight / 2 + 4)
            }

            g2.font = HOTBAR_KEY_FONT
            g2.color = Color(200, 200, 200)
            g2.drawString("${i + 1}", sx + 3, hotbarY + 12)
        }

        val switchLabel = hotbar.switchLabel
        if (switchLabel != null) {
            g2.font = SWITCH_LABEL_FONT
            val labelWidth = g2.fontMetrics.stringWidth(switchLabel)
            val labelX = (gameSettings.windowWidth - labelWidth) / 2
            val labelY = hotbarY - 12
            g2.color = Color(0, 0, 0, 150)
            g2.fillRoundRect(labelX - 8, labelY - 16, labelWidth + 16, 22, 8, 8)
            g2.color = Color.WHITE
            g2.drawString(switchLabel, labelX, labelY)
        }
    }

    private fun paintMap(g2: Graphics2D) {
        val w = gameSettings.windowWidth
        val h = gameSettings.windowHeight

        val image = mapImage ?: generateMapImage(w, h).also { mapImage = it }
        g2.drawImage(image, 0, 0, null)

        // Player marker
        val player = renderService.player
        val markerX = (player.x / WORLD_WIDTH_PIXELS * w).toInt()
        val markerY = (player.y / WORLD_HEIGHT_PIXELS * h).toInt()
        g2.color = Color(50, 100, 220)
        g2.fillRect(markerX - 3, markerY - 3, 7, 7)
        g2.color = Color.WHITE
        g2.drawRect(markerX - 3, markerY - 3, 7, 7)

        drawCenteredOutlinedString(g2, "MAP - Press M to close", Font("Monospaced", Font.PLAIN, 20), h - 30, Color.WHITE, 2)
    }

    private fun paintSettings(g2: Graphics2D) {
        val w = gameSettings.windowWidth
        val h = gameSettings.windowHeight
        val selectedIndex = renderService.settingsSelectedIndex

        g2.color = Color(20, 15, 30, 220)
        g2.fillRect(0, 0, w, h)

        drawCenteredOutlinedString(g2, "SETTINGS", Font("Monospaced", Font.BOLD, 48), h / 4, Color.WHITE, 3)

        val labelFont = Font("Monospaced", Font.PLAIN, 22)
        val valueFont = Font("Monospaced", Font.BOLD, 22)
        val startY = h / 4 + 80
        val lineHeight = 40
        val labelX = w / 2 - 200
        val valueX = w / 2 + 100

        for ((i, entry) in gameSettings.editableSettings.withIndex()) {
            val y = startY + i * lineHeight
            val selected = i == selectedIndex
            val value = entry.get().toString()

            if (selected) {
                g2.color = Color(255, 255, 255, 30)
                g2.fillRect(labelX - 10, y - 22, valueX - labelX + 200, 30)
            }

            g2.font = labelFont
            g2.color = if (selected) Color.YELLOW else Color.LIGHT_GRAY
            g2.drawString(entry.label, labelX, y)

            g2.font = valueFont
            g2.color = if (selected) Color.YELLOW else Color.WHITE
            if (selected) {
                g2.drawString("< $value >", valueX, y)
            } else {
                g2.drawString(value, valueX, y)
            }
        }

        drawCenteredOutlinedString(g2, "Up/Down: select   Left/Right: change   ESC: save & close", Font("Monospaced", Font.PLAIN, 18), h - 60, Color.LIGHT_GRAY, 2)
    }

    private fun generateMapImage(w: Int, h: Int): BufferedImage {
        val world = renderService.world
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)

        for (py in 0..<h) {
            val tileY = (py.toDouble() / h * WORLD_HEIGHT_TILES).toInt()
            for (px in 0..<w) {
                val tileX = (px.toDouble() / w * WORLD_WIDTH_TILES).toInt()
                val tile = world[tileX, tileY]
                val color = tile.color ?: CAVE_BG_COLOR
                image.setRGB(px, py, color.rgb)
            }
        }

        return image
    }

    fun invalidateMap() {
        mapImage = null
    }

    private fun drawCenteredOutlinedString(g2: Graphics2D, text: String, font: Font, y: Int, fill: Color, thickness: Int) {
        g2.font = font
        val x = (gameSettings.windowWidth - g2.fontMetrics.stringWidth(text)) / 2
        drawOutlinedString(g2, text, x, y, fill, thickness)
    }

    private fun drawOutlinedString(g2: Graphics2D, text: String, x: Int, y: Int, fill: Color, thickness: Int) {
        g2.color = Color(0, 0, 0, fill.alpha)
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
}
