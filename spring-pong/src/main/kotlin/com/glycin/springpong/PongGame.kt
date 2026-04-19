package com.glycin.springpong

import com.glycin.util.Vec2
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

class PongGame : JPanel() {

    companion object {
        private const val WIDTH = 1200
        private const val HEIGHT = 800
        private const val PADDLE_WIDTH = 15f
        private const val PADDLE_HEIGHT = 90f
        private const val BALL_SIZE = 15f
        private const val PADDLE_SPEED = 6f
        private const val AI_SPEED = 2.75f
        private const val AI_REACTION_DISTANCE = 350f
        private const val INITIAL_BALL_SPEED = 10f
        private const val BALL_SPEED_INCREMENT = 0.5f
        private const val WINNING_SCORE = 3

        fun play(): Boolean {
            val latch = CountDownLatch(1)
            var playerWon = false
            val game = PongGame()

            SwingUtilities.invokeAndWait {
                val frame = JFrame("Decide Your Bean!").apply {
                    defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
                    isResizable = false
                    add(game)
                    pack()
                    setLocationRelativeTo(null)
                    isVisible = true
                    requestFocusInWindow()
                }
                game.requestFocusInWindow()

                game.onGameEnd = { result ->
                    playerWon = result
                    CoroutineScope(Dispatchers.Swing).launch {
                        delay(2000)
                        frame.dispose()
                        latch.countDown()
                    }
                }

                game.startGameLoop()
            }

            latch.await()
            return playerWon
        }
    }

    private var playerPaddle = Vec2(30f, HEIGHT / 2f - PADDLE_HEIGHT / 2f)
    private var aiPaddle = Vec2(WIDTH - 30f - PADDLE_WIDTH, HEIGHT / 2f - PADDLE_HEIGHT / 2f)
    private var ballPos = Vec2(WIDTH / 2f, HEIGHT / 2f)
    private var ballVelocity = Vec2.zero
    private var ballSpeed = INITIAL_BALL_SPEED

    private var playerScore = 0
    private var aiScore = 0
    private var gameOver = false
    private var resultText = ""

    private var wPressed = false
    private var sPressed = false

    private var onGameEnd: ((Boolean) -> Unit)? = null

    init {
        preferredSize = Dimension(WIDTH, HEIGHT)
        background = Color.BLACK
        isFocusable = true

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_W -> wPressed = true
                    KeyEvent.VK_S -> sPressed = true
                }
            }
            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_W -> wPressed = false
                    KeyEvent.VK_S -> sPressed = false
                }
            }
        })

        resetBall()
    }

    private fun resetBall() {
        ballPos = Vec2(WIDTH / 2f, HEIGHT / 2f)
        ballSpeed = INITIAL_BALL_SPEED + (playerScore + aiScore) * BALL_SPEED_INCREMENT
        val angle = Random.nextFloat() * 0.8f - 0.4f
        val dirX = if (Random.nextBoolean()) 1f else -1f
        ballVelocity = Vec2(dirX, angle).normalized() * ballSpeed
    }

    private fun startGameLoop() {
        CoroutineScope(Dispatchers.Swing).launch {
            while (!gameOver) {
                update()
                repaint()
                delay(16) // ~60fps
            }
            repaint()
        }
    }

    private fun update() {
        if (gameOver) return

        if (wPressed) {
            playerPaddle = playerPaddle.copy(y = (playerPaddle.y - PADDLE_SPEED).coerceIn(0f, HEIGHT - PADDLE_HEIGHT))
        }
        if (sPressed) {
            playerPaddle = playerPaddle.copy(y = (playerPaddle.y + PADDLE_SPEED).coerceIn(0f, HEIGHT - PADDLE_HEIGHT))
        }

        if (ballVelocity.x > 0 && ballPos.x > WIDTH - AI_REACTION_DISTANCE) {
            val aiCenter = aiPaddle.y + PADDLE_HEIGHT / 2f
            val diff = ballPos.y - aiCenter
            if (abs(diff) > AI_SPEED) {
                aiPaddle = aiPaddle.copy(y = (aiPaddle.y + AI_SPEED * diff.sign).coerceIn(0f, HEIGHT - PADDLE_HEIGHT))
            }
        }

        ballPos += ballVelocity

        if (ballPos.y <= 0f) {
            ballPos = ballPos.copy(y = 0f)
            ballVelocity = ballVelocity.copy(y = abs(ballVelocity.y))
        }
        if (ballPos.y + BALL_SIZE >= HEIGHT) {
            ballPos = ballPos.copy(y = HEIGHT - BALL_SIZE)
            ballVelocity = ballVelocity.copy(y = -abs(ballVelocity.y))
        }

        if (ballVelocity.x < 0 &&
            ballPos.x <= playerPaddle.x + PADDLE_WIDTH &&
            ballPos.x + BALL_SIZE >= playerPaddle.x &&
            ballPos.y + BALL_SIZE >= playerPaddle.y &&
            ballPos.y <= playerPaddle.y + PADDLE_HEIGHT
        ) {
            ballPos = ballPos.copy(x = playerPaddle.x + PADDLE_WIDTH)
            val hitPos = (ballPos.y + BALL_SIZE / 2f - playerPaddle.y) / PADDLE_HEIGHT // 0..1
            val deflection = (hitPos - 0.5f) * 2f // -1..1
            ballVelocity = Vec2(abs(ballVelocity.x), deflection * ballSpeed * 0.8f)
        }

        if (ballVelocity.x > 0 &&
            ballPos.x + BALL_SIZE >= aiPaddle.x &&
            ballPos.x <= aiPaddle.x + PADDLE_WIDTH &&
            ballPos.y + BALL_SIZE >= aiPaddle.y &&
            ballPos.y <= aiPaddle.y + PADDLE_HEIGHT
        ) {
            ballPos = ballPos.copy(x = aiPaddle.x - BALL_SIZE)
            val hitPos = (ballPos.y + BALL_SIZE / 2f - aiPaddle.y) / PADDLE_HEIGHT
            val deflection = (hitPos - 0.5f) * 2f
            ballVelocity = Vec2(-abs(ballVelocity.x), deflection * ballSpeed * 0.8f)
        }

        if (ballPos.x + BALL_SIZE < 0) {
            aiScore++
            if (aiScore >= WINNING_SCORE) {
                gameOver = true
                resultText = "YOU LOSE! Registering KindaCoolService..."
                onGameEnd?.invoke(false)
            } else {
                resetBall()
            }
        }
        if (ballPos.x > WIDTH) {
            playerScore++
            if (playerScore >= WINNING_SCORE) {
                gameOver = true
                resultText = "YOU WIN! Registering SuperCoolService!"
                onGameEnd?.invoke(true)
            } else {
                resetBall()
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = Color.WHITE

        // Center line
        g2.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(10f), 0f)
        g2.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT)
        g2.stroke = BasicStroke(1f)

        // Paddles
        g2.color = Color.GREEN
        g2.fillRect(playerPaddle.x.toInt(), playerPaddle.y.toInt(), PADDLE_WIDTH.toInt(), PADDLE_HEIGHT.toInt())
        g2.color = Color.RED
        g2.fillRect(aiPaddle.x.toInt(), aiPaddle.y.toInt(), PADDLE_WIDTH.toInt(), PADDLE_HEIGHT.toInt())

        g2.color = Color.CYAN
        // Ball
        g2.fillOval(ballPos.x.toInt(), ballPos.y.toInt(), BALL_SIZE.toInt(), BALL_SIZE.toInt())

        // Score
        g2.font = Font("Monospaced", Font.BOLD, 48)
        val fm = g2.fontMetrics
        g2.drawString("$playerScore", WIDTH / 2 - fm.stringWidth("$playerScore") - 30, 60)
        g2.drawString("$aiScore", WIDTH / 2 + 30, 60)

        // Labels
        g2.font = Font("Monospaced", Font.PLAIN, 14)
        g2.drawString("Player (W/S)", 20, HEIGHT - 20)
        g2.drawString("AI", WIDTH - 50, HEIGHT - 20)

        // Game over text
        if (gameOver) {
            g2.font = Font("Monospaced", Font.BOLD, 36)
            val resultFm = g2.fontMetrics
            val textWidth = resultFm.stringWidth(resultText)
            g2.color = if (resultText.startsWith("YOU WIN")) Color.GREEN else Color.RED
            g2.drawString(resultText, (WIDTH - textWidth) / 2, HEIGHT / 2)
        }
    }
}
