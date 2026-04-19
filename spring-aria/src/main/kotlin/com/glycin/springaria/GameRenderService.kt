package com.glycin.springaria

import com.glycin.springaria.gameplay.BulletService
import com.glycin.springaria.gameplay.EnemyMissileService
import com.glycin.springaria.gameplay.EnemyService
import com.glycin.springaria.gameplay.Hotbar
import com.glycin.springaria.gameplay.PlayerMissileService
import com.glycin.springaria.gameplay.Player
import com.glycin.springaria.world.Camera
import com.glycin.springaria.world.World
import org.springframework.stereotype.Service
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.SwingUtilities

@Service
class GameRenderService(
    private val inputService: InputService,
    private val gameSettings: GameSettings,
    val world: World,
    val camera: Camera,
    val player: Player,
    val hotbar: Hotbar,
    val bulletService: BulletService,
    val playerMissileService: PlayerMissileService,
    val enemyMissileService: EnemyMissileService,
    val enemyService: EnemyService,
) {

    var gameState: GameState = GameState.MENU

    @Volatile var caveLabelText: String? = null
    @Volatile var caveLabelTime: Long = 0
    @Volatile var settingsSelectedIndex: Int = 0
    @Volatile var swingTime: Long = 0
    @Volatile var swingTargetScreenX: Int = 0
    @Volatile var swingTargetScreenY: Int = 0

    private var frame: JFrame? = null
    private var panel: GamePanel? = null

    fun initialize() {
        SwingUtilities.invokeAndWait {
            val gamePanel = GamePanel(gameSettings, this)
            panel = gamePanel

            val gameFrame = JFrame("SpringCraft").apply {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                isResizable = false
                add(gamePanel)
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }

            gamePanel.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    inputService.keyPressed(e.keyCode)
                }

                override fun keyReleased(e: KeyEvent) {
                    inputService.keyReleased(e.keyCode)
                }
            })
            gamePanel.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON1) {
                        inputService.mouseClicked(e.x, e.y)
                    }
                }
            })
            gamePanel.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    inputService.mouseMoved(e.x, e.y)
                }

                override fun mouseDragged(e: MouseEvent) {
                    inputService.mouseMoved(e.x, e.y)
                }
            })
            gamePanel.requestFocusInWindow()

            frame = gameFrame
        }
    }

    fun triggerSwing(screenX: Int, screenY: Int) {
        swingTargetScreenX = screenX
        swingTargetScreenY = screenY
        swingTime = System.currentTimeMillis()
    }

    fun showCaveLabel(name: String) {
        caveLabelText = name
        caveLabelTime = System.currentTimeMillis()
    }

    val mouseScreenX: Int get() = inputService.mouseScreenX
    val mouseScreenY: Int get() = inputService.mouseScreenY

    fun render() {
        if (player.mining) {
            triggerSwing(inputService.mouseScreenX, inputService.mouseScreenY)
        }
        panel?.repaint()
    }
}
