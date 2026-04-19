package com.glycin.springshooter

import com.glycin.sound.SoundClip
import com.glycin.sound.SoundPlayer
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.SwingUtilities

private const val EDGE_MARGIN = 20
private const val EDGE_SCROLL_SPEED = 8

@Service
class GameRenderService(
    private val inputService: InputService,
    private val playerService: PlayerService,
    private val enemyService: EnemyService,
    private val levelService: LevelService,
    private val springApplicationContextService: SpringApplicationContextService,
    environment: org.springframework.core.env.Environment,
) {

    val isTutorial: Boolean = "tutorial" in environment.activeProfiles

    var gameState: GameState = GameState.MENU
    var onStartGame: (() -> Unit)? = null

    private val menuMusic = SoundPlayer(SoundClip("audio/menu_theme.wav"))

    private var frame: JFrame? = null
    private var panel: GamePanel? = null
    private var lastMouseX: Int = -1
    private var lastMouseY: Int = -1

    fun initialize() {
        SwingUtilities.invokeAndWait {
            val gamePanel = GamePanel(this, playerService, enemyService, levelService)
            panel = gamePanel

            val gameFrame = JFrame("Spring: Enterprise Evolved")
            gameFrame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            gameFrame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    springApplicationContextService.closeContext()
                }
            })
            gameFrame.isResizable = false
            gameFrame.add(gamePanel)
            gameFrame.pack()
            gameFrame.setLocationRelativeTo(null)
            gameFrame.isVisible = true

            gamePanel.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    if (gameState != GameState.PLAYING) return

                    if (lastMouseX >= 0) {
                        val dx = e.x - lastMouseX
                        val dy = e.y - lastMouseY
                        if (dx != 0 || dy != 0) {
                            inputService.addMouseDelta(dx, dy)
                        }
                    }
                    lastMouseX = e.x
                    lastMouseY = e.y

                    updateEdgeScroll(gamePanel.width, gamePanel.height, e.x, e.y)
                }

                override fun mouseDragged(e: MouseEvent) = mouseMoved(e)
            })

            gamePanel.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (gameState == GameState.PLAYING) {
                        inputService.requestShoot()
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    if (gameState == GameState.PLAYING) {
                        // Keep scrolling in the direction the mouse left
                        updateEdgeScroll(gamePanel.width, gamePanel.height, e.x, e.y)
                    }
                }

                override fun mouseEntered(e: MouseEvent) {
                    if (gameState == GameState.PLAYING) {
                        // Reset lastMouse so we don't get a huge delta jump on re-entry
                        lastMouseX = e.x
                        lastMouseY = e.y
                        inputService.edgeScrollX = 0
                        inputService.edgeScrollY = 0
                    }
                }
            })

            gamePanel.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (gameState) {
                        GameState.MENU -> handleMenuKey(e)
                        GameState.PLAYING -> {
                            if (e.keyCode == KeyEvent.VK_P) {
                                springApplicationContextService.restartWithProfile("level1")
                            } else {
                                inputService.keyPressed(e.keyCode)
                            }
                        }
                    }
                }

                override fun keyReleased(e: KeyEvent) {
                    if (gameState == GameState.PLAYING) {
                        inputService.keyReleased(e.keyCode)
                    }
                }
            })
            gamePanel.requestFocusInWindow()

            frame = gameFrame
        }
    }

    @PreDestroy
    fun dispose() {
        menuMusic.stop()
        val cleanup = Runnable {
            frame?.dispose()
            frame = null
            panel = null
        }
        if (SwingUtilities.isEventDispatchThread()) {
            cleanup.run()
        } else {
            SwingUtilities.invokeAndWait(cleanup)
        }
    }

    fun renderMenu() {
        if (!menuMusic.isPlaying()) {
            menuMusic.loop()
        }
        panel?.repaint()
    }

    fun render() {
        panel?.repaint()
    }

    private fun handleMenuKey(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER) {
            menuMusic.stop()
            gameState = GameState.PLAYING
            hideCursor()
            onStartGame?.invoke()
        }
    }

    private fun updateEdgeScroll(panelWidth: Int, panelHeight: Int, mouseX: Int, mouseY: Int) {
        inputService.edgeScrollX = when {
            mouseX <= EDGE_MARGIN -> -EDGE_SCROLL_SPEED
            mouseX >= panelWidth - EDGE_MARGIN -> EDGE_SCROLL_SPEED
            else -> 0
        }
        inputService.edgeScrollY = when {
            mouseY <= EDGE_MARGIN -> -EDGE_SCROLL_SPEED
            mouseY >= panelHeight - EDGE_MARGIN -> EDGE_SCROLL_SPEED
            else -> 0
        }
    }

    private fun hideCursor() {
        val panel = panel ?: return
        val blankCursor = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
            java.awt.Point(0, 0),
            "blank",
        )
        panel.cursor = blankCursor
    }
}
