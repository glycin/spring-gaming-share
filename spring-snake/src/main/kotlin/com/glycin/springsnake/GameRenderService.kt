package com.glycin.springsnake

import com.glycin.springsnake.FoodRepository.Companion.GRID_HEIGHT
import com.glycin.springsnake.FoodRepository.Companion.GRID_WIDTH
import com.glycin.util.Vec2
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Service
class GameRenderService(
    private val snakeService: SnakeService,
) {

    private var frame: JFrame? = null
    private var panel: GamePanel? = null

    var gameState: GameState = GameState.MENU
    var onStartGame: (() -> Unit)? = null

    companion object {
        private const val CELL_SIZE = 30
        private const val SCORE_PANEL_HEIGHT = 80
        private const val WINDOW_WIDTH = GRID_WIDTH * CELL_SIZE
        private const val GAME_HEIGHT = GRID_HEIGHT * CELL_SIZE
        private const val WINDOW_HEIGHT = GAME_HEIGHT + SCORE_PANEL_HEIGHT

        private const val TITLE_HEIGHT = 60
        private const val SEPARATOR_HEIGHT = 3
        private const val MENU_ITEM_HEIGHT = 40
        private const val MENU_TOP = TITLE_HEIGHT + SEPARATOR_HEIGHT
        private val MENU_ITEMS = listOf("New game", "High scores", "Settings", "Instructions")
    }

    fun initialize() {
        SwingUtilities.invokeAndWait {
            val sprites = SnakeSprites()
            val gamePanel = GamePanel(sprites, this)
            panel = gamePanel

            frame = JFrame("Snake XE2 :: Spring Edition").apply {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                isResizable = false
                add(gamePanel)
                pack()
                setLocationRelativeTo(null)
                isVisible = true

                addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        when (gameState) {
                            GameState.MENU -> handleMenuKey(e)
                            GameState.PLAYING -> handleGameKey(e)
                            GameState.GAME_OVER -> {}
                        }
                    }
                })

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (gameState != GameState.MENU) return
                        val y = e.y - insets.top
                        val itemIndex = (y - MENU_TOP) / MENU_ITEM_HEIGHT
                        if (itemIndex in MENU_ITEMS.indices) {
                            gamePanel.selectedMenuItem = itemIndex
                            if (itemIndex == 0) {
                                selectMenuItem()
                            }
                            gamePanel.repaint()
                        }
                    }
                })
            }
        }
    }

    private fun handleMenuKey(e: KeyEvent) {
        val p = panel ?: return
        when (e.keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_UP -> {
                p.selectedMenuItem = (p.selectedMenuItem - 1 + MENU_ITEMS.size) % MENU_ITEMS.size
                p.repaint()
            }
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                p.selectedMenuItem = (p.selectedMenuItem + 1) % MENU_ITEMS.size
                p.repaint()
            }
            KeyEvent.VK_ENTER -> selectMenuItem()
        }
    }

    private fun selectMenuItem() {
        val p = panel ?: return
        if (p.selectedMenuItem == 0) {
            gameState = GameState.PLAYING
            onStartGame?.invoke()
        }
    }

    private fun handleGameKey(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_UP -> snakeService.changeDirection(Vec2.up)
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> snakeService.changeDirection(Vec2.down)
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> snakeService.changeDirection(Vec2.left)
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> snakeService.changeDirection(Vec2.right)
        }
    }

    fun renderMenu() {
        panel?.repaint()
    }

    fun render(snakeBody: List<Vec2>, food: Vec2, foodType: FoodType, score: Int, gameOver: Boolean, mouthOpen: Boolean) {
        panel?.let {
            it.updateState(snakeBody, food, foodType, score, gameOver, mouthOpen)
            it.repaint()
        }
    }

    private class GamePanel(
        private val sprites: SnakeSprites,
        private val renderService: GameRenderService,
    ) : JPanel() {

        private var snakeBody = emptyList<Vec2>()
        private var food = Vec2.zero
        private var foodType = FoodType.EGG
        private var score = 0
        private var gameOver = false
        private var mouthOpen = false
        var selectedMenuItem = 0

        init {
            preferredSize = Dimension(WINDOW_WIDTH, WINDOW_HEIGHT)
            background = Color(238, 221, 187)
        }

        fun updateState(snakeBody: List<Vec2>, food: Vec2, foodType: FoodType, score: Int, gameOver: Boolean, mouthOpen: Boolean) {
            this.snakeBody = snakeBody
            this.food = food
            this.foodType = foodType
            this.score = score
            this.gameOver = gameOver
            this.mouthOpen = mouthOpen
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            when (renderService.gameState) {
                GameState.MENU -> paintMenu(g2)
                GameState.PLAYING, GameState.GAME_OVER -> paintGame(g2)
            }
        }

        private fun paintMenu(g2: Graphics2D) {
            g2.color = Color(0xE0, 0xE0, 0xE0)
            g2.fillRect(0, 0, WINDOW_WIDTH, TITLE_HEIGHT)
            g2.color = Color.BLACK
            g2.font = Font("Monospaced", Font.BOLD, 28)
            val titleFm = g2.fontMetrics
            val title = "Snake EX2"
            g2.drawString(title, (WINDOW_WIDTH - titleFm.stringWidth(title)) / 2, TITLE_HEIGHT / 2 + titleFm.ascent / 2)

            g2.color = Color(0x00, 0x00, 0xCC)
            g2.fillRect(0, TITLE_HEIGHT, WINDOW_WIDTH, SEPARATOR_HEIGHT)

            g2.color = Color(0xFF, 0xD7, 0x00)
            g2.fillRect(0, MENU_TOP, WINDOW_WIDTH, WINDOW_HEIGHT - MENU_TOP)

            g2.font = Font("Monospaced", Font.BOLD, 24)
            val itemFm = g2.fontMetrics
            for ((i, item) in MENU_ITEMS.withIndex()) {
                g2.color = if (i == selectedMenuItem) Color.RED else Color.BLACK
                val y = MENU_TOP + i * MENU_ITEM_HEIGHT + (MENU_ITEM_HEIGHT + itemFm.ascent) / 2 - 2
                g2.drawString(item, 20, y)
            }
        }

        private fun paintGame(g2: Graphics2D) {
            g2.drawImage(
                sprites.getFoodSprite(foodType),
                (food.x * CELL_SIZE).toInt(),
                (food.y * CELL_SIZE).toInt(),
                CELL_SIZE, CELL_SIZE,
                null,
            )

            for (i in snakeBody.indices) {
                val segment = snakeBody[i]
                val dx = (segment.x * CELL_SIZE).toInt()
                val dy = (segment.y * CELL_SIZE).toInt()

                val sprite = when (i) {
                    0 -> {
                        val direction = if (snakeBody.size > 1) segment - snakeBody[1] else Vec2.right
                        sprites.getHeadSprite(direction, mouthOpen)
                    }
                    snakeBody.lastIndex -> {
                        val dirToBody = snakeBody[i - 1] - segment
                        sprites.getTailSprite(dirToBody)
                    }
                    else -> {
                        val fromDir = segment - snakeBody[i + 1]
                        val toDir = snakeBody[i - 1] - segment
                        sprites.getBodySprite(fromDir, toDir)
                    }
                }

                g2.drawImage(sprite, dx, dy, CELL_SIZE, CELL_SIZE, null)
            }

            val left = sprites.scorePanelLeft
            val mid = sprites.scorePanelMiddle
            val right = sprites.scorePanelRight
            val scaledLeftW = left.width * 2
            val scaledMidW = mid.width * 2
            val scaledRightW = right.width * 2
            g2.drawImage(left, 0, GAME_HEIGHT, scaledLeftW, SCORE_PANEL_HEIGHT, null)
            var panelX = scaledLeftW
            while (panelX < WINDOW_WIDTH - scaledRightW) {
                g2.drawImage(mid, panelX, GAME_HEIGHT, scaledMidW, SCORE_PANEL_HEIGHT, null)
                panelX += scaledMidW
            }
            g2.drawImage(right, WINDOW_WIDTH - scaledRightW, GAME_HEIGHT, scaledRightW, SCORE_PANEL_HEIGHT, null)

            val digitScale = 4
            val digitW = 7 * digitScale
            val digitH = 9 * digitScale
            val digitY = GAME_HEIGHT + (SCORE_PANEL_HEIGHT - digitH) / 2
            val maxDigits = WINDOW_WIDTH / digitW
            val totalW = maxDigits * digitW
            val startX = (WINDOW_WIDTH - totalW) / 2
            val scoreStr = score.toString().padStart(maxDigits, '0')
            for ((i, ch) in scoreStr.withIndex()) {
                val digit = ch - '0'
                g2.drawImage(sprites.getDigitSprite(digit), startX + i * digitW, digitY, digitW, digitH, null)
            }

            if (gameOver) {
                g2.color = Color(0, 0, 0, 150)
                g2.fillRect(0, 0, WINDOW_WIDTH, GAME_HEIGHT)
                g2.color = Color.RED
                g2.font = Font("Monospaced", Font.BOLD, 36)
                val text = "GAME OVER"
                val fm = g2.fontMetrics
                g2.drawString(text, (WINDOW_WIDTH - fm.stringWidth(text)) / 2, GAME_HEIGHT / 2)
                g2.color = Color.WHITE
                g2.font = Font("Monospaced", Font.PLAIN, 18)
                val scoreText = "Final Score: $score"
                val sfm = g2.fontMetrics
                g2.drawString(scoreText, (WINDOW_WIDTH - sfm.stringWidth(scoreText)) / 2, GAME_HEIGHT / 2 + 40)
            }
        }
    }
}
