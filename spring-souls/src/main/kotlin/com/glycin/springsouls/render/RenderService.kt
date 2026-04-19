package com.glycin.springsouls.render

import com.glycin.springsouls.IS_MAC_OS
import com.glycin.springsouls.GameSettings
import com.glycin.springsouls.GameState
import com.glycin.springsouls.input.InputService
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil.NULL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

@Service
class RenderService(
    private val gameSettings: GameSettings,
    private val inputService: InputService,
    private val levelRenderService: LevelRenderService,
    private val playerRenderService: PlayerRenderService,
    private val potionRenderService: PotionRenderService,
    private val keyRenderService: KeyRenderService,
    private val enemyRenderService: EnemyRenderService,
    private val bossRenderService: BossRenderService,
    private val hudRenderService: HudRenderService,
    private val mainMenuRenderService: MainMenuRenderService,
) {
    private val logger = LoggerFactory.getLogger(RenderService::class.java)

    private val isMacOs = IS_MAC_OS

    private var window: Long = NULL
    private var renderThread: Thread? = null
    private val initialized = CountDownLatch(1)
    private val startRequested = CountDownLatch(1)
    private val running = AtomicBoolean(false)
    private val windowClosed = AtomicBoolean(false)

    val camera = Camera3D()
    @Volatile var gameState: GameState = GameState.MENU

    private val stateLock = ReentrantLock()
    private val renderCommands = ConcurrentLinkedQueue<() -> Unit>()

    private var shader: ShaderProgram? = null

    @Volatile private var cursorReleased = false

    private fun applyCursorMode() {
        val mode = if (cursorReleased) GLFW_CURSOR_NORMAL else GLFW_CURSOR_DISABLED
        glfwSetInputMode(window, GLFW_CURSOR, mode)
    }

    fun initialize() {
        camera.fov = gameSettings.fov
        camera.nearPlane = gameSettings.nearPlane
        camera.farPlane = gameSettings.farPlane

        if (isMacOs) {
            startRequested.countDown()
        } else {
            renderThread = Thread(::renderLoop, "glfw-render").apply {
                isDaemon = true
                start()
            }
        }
        initialized.await()
        logger.info("Render service initialized ({}x{})", gameSettings.windowWidth, gameSettings.windowHeight)
    }

    fun awaitAndRunOnMainThread() {
        startRequested.await()
        renderLoop()
    }

    fun isRunning() = running.get()

    fun isWindowClosed() = windowClosed.get()

    fun submit(command: () -> Unit) {
        renderCommands.add(command)
    }

    fun shutdown() {
        running.set(false)
    }

     fun <T> withStateLock(block: () -> T): T {
        stateLock.lock()
        try {
            return block()
        } finally {
            stateLock.unlock()
        }
    }

    private fun renderLoop() {
        initGlfw()
        initOpenGL()
        loadDefaultScene()

        running.set(true)
        initialized.countDown()

        while (running.get()) {
            if (glfwWindowShouldClose(window)) {
                windowClosed.set(true)
                break
            }
            generateSequence { renderCommands.poll() }.forEach { it() }

            withStateLock { render() }
            glfwSwapBuffers(window)
            glfwPollEvents()
        }

        cleanup()
    }

    private fun initGlfw() {
        GLFWErrorCallback.createPrint(System.err).set()

        check(glfwInit()) { "Failed to initialize GLFW" }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)

        window = glfwCreateWindow(
            gameSettings.windowWidth,
            gameSettings.windowHeight,
            gameSettings.title,
            NULL,
            NULL,
        )
        check(window != NULL) { "Failed to create GLFW window" }

        val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()!!)!!
        glfwSetWindowPos(
            window,
            (vidMode.width() - gameSettings.windowWidth) / 2,
            (vidMode.height() - gameSettings.windowHeight) / 2,
        )

        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            when (action) {
                GLFW_PRESS -> {
                    if (key == GLFW_KEY_F) {
                        cursorReleased = !cursorReleased
                        applyCursorMode()
                    }
                    inputService.keyPressed(key)
                }
                GLFW_RELEASE -> inputService.keyReleased(key)
            }
        }

        glfwSetWindowFocusCallback(window) { _, focused ->
            val mode = if (focused && !cursorReleased) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL
            glfwSetInputMode(window, GLFW_CURSOR, mode)
        }

        glfwSetCursorPosCallback(window) { _, xPos, yPos ->
            inputService.mouseMoved(xPos.toFloat(), yPos.toFloat())
        }

        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            when (action) {
                GLFW_PRESS -> inputService.mousePressed(button)
                GLFW_RELEASE -> inputService.mouseReleased(button)
            }
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1)
        glfwShowWindow(window)
        applyCursorMode()
    }

    private fun initOpenGL() {
        GL.createCapabilities()
        glViewport(0, 0, gameSettings.windowWidth, gameSettings.windowHeight)
        glEnable(GL_DEPTH_TEST)
        glClearColor(0.05f, 0.05f, 0.08f, 1.0f)
    }

    private fun loadDefaultScene() {
        shader = ShaderProgram.fromResources("shaders/vertex.glsl", "shaders/fragment.glsl")
        levelRenderService.initMeshes()
        playerRenderService.initMeshes()
        potionRenderService.initMeshes()
        keyRenderService.initMeshes()
        enemyRenderService.initMeshes()
        bossRenderService.initMeshes()
        mainMenuRenderService.initMeshes()
        hudRenderService.init()
    }

    private fun render() {
        when (gameState) {
            GameState.MENU -> glClearColor(0f, 0f, 0f, 1f)
            GameState.PLAYING -> glClearColor(0.05f, 0.05f, 0.08f, 1f)
        }
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val s = shader ?: return
        s.use()

        when (gameState) {
            GameState.MENU -> mainMenuRenderService.render(s)
            GameState.PLAYING -> renderGame(s)
        }
    }

    private fun renderGame(s: ShaderProgram) {
        val aspect = gameSettings.windowWidth.toFloat() / gameSettings.windowHeight.toFloat()
        s.setMatrix4("projection", camera.projectionMatrix(aspect))
        s.setMatrix4("view", camera.viewMatrix())

        s.setVec3("lightDir", -0.3f, -1.0f, -0.5f)
        s.setVec3("lightColor", 1.0f, 0.9f, 0.8f)
        s.setFloat("ambientStrength", 0.15f)
        s.setVec3("viewPos", camera.x, camera.y, camera.z)
        s.setVec3("fogColor", 0.05f, 0.05f, 0.08f)
        s.setFloat("fogNear", 15f)
        s.setFloat("fogFar", 50f)

        levelRenderService.render(s, camera)
        potionRenderService.render(s, camera)
        keyRenderService.render(s, camera)
        enemyRenderService.render(s, camera)
        bossRenderService.render(s, camera)
        playerRenderService.render(s, camera)

        hudRenderService.render()
    }

    private fun cleanup() {
        levelRenderService.cleanup()
        potionRenderService.cleanup()
        keyRenderService.cleanup()
        enemyRenderService.cleanup()
        bossRenderService.cleanup()
        playerRenderService.cleanup()
        mainMenuRenderService.cleanup()
        hudRenderService.cleanup()
        shader?.cleanup()

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()

        logger.info("Render service shut down")
    }
}
