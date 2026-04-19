package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.springsurvivors.player.InputService
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.rhythm.BeatScheduler
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

@GameManager
class GameRenderService(
    private val gameSettings: GameSettings,
    private val inputService: InputService,
    private val player: Player,
    private val beatScheduler: BeatScheduler,
    private val backgroundRenderer: BackgroundRenderer,
    private val gameRenderExecutor: GameRenderExecutor,
) {

    private var frame: JFrame? = null
    private var panel: GamePanel? = null

    fun initialize() {
        SwingUtilities.invokeAndWait {
            val gamePanel = GamePanel(player, beatScheduler, backgroundRenderer, gameRenderExecutor).apply {
                preferredSize = Dimension(gameSettings.windowWidth, gameSettings.windowHeight)

                addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) = inputService.keyPressed(e.keyCode)
                    override fun keyReleased(e: KeyEvent) = inputService.keyReleased(e.keyCode)
                })
            }

            frame = JFrame("Enterprise Survivors").apply {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                contentPane = gamePanel
                isResizable = false
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }

            gamePanel.requestFocusInWindow()
            panel = gamePanel
        }
    }

    fun render() {
        panel?.repaint()
    }
}
