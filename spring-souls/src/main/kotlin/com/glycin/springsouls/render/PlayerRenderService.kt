package com.glycin.springsouls.render

import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.gameplay.PlayerState
import com.glycin.springsouls.render.animation.AnimationPlayer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.cos
import kotlin.math.sin

private const val MODEL_SCALE = 0.01f
private const val DODGE_DISTANCE_MULTIPLIER = 2f
private val DEFAULT_MATERIAL = MeshMaterial()

private val ANIMATION_FILES = listOf(
    "models/ninja_idle.fbx" to PlayerState.IDLE,
    "models/run_forward.fbx" to PlayerState.RUN_FORWARD,
    "models/run_back.fbx" to PlayerState.RUN_BACK,
    "models/run_left.fbx" to PlayerState.RUN_LEFT,
    "models/run_right.fbx" to PlayerState.RUN_RIGHT,
    "models/dodge_forward.fbx" to PlayerState.DODGE_FORWARD,
    "models/dodge_back.fbx" to PlayerState.DODGE_BACK,
    "models/dodge_left.fbx" to PlayerState.DODGE_LEFT,
    "models/dodge_right.fbx" to PlayerState.DODGE_RIGHT,
    "models/punch.fbx" to PlayerState.PUNCH,
    "models/kick.fbx" to PlayerState.KICK,
    "models/drink.fbx" to PlayerState.DRINK,
    "models/ninja_hit.fbx" to PlayerState.HIT,
    "models/ninja_death.fbx" to PlayerState.DEATH,
)

@Service
class PlayerRenderService(
    private val player: Player,
) {
    private val logger = LoggerFactory.getLogger(PlayerRenderService::class.java)

    private var meshes: List<Mesh> = emptyList()
    private var materials: List<MeshMaterial> = emptyList()
    private var animationPlayer: AnimationPlayer? = null
    private var animated = false

    private val stateToAnimIndex = mutableMapOf<PlayerState, Int>()
    private var currentState: PlayerState = PlayerState.IDLE
    private var rootBoneName: String? = null
    private var dodgeStartX = 0f
    private var dodgeStartZ = 0f

    fun initMeshes() {
        val result = ModelLoader.loadAnimatedFromResources("models/ninja_nopbr.fbx")
        meshes = result.meshes
        materials = result.materials

        val nodeNames = result.skeleton.nodes.map { it.name }.toSet()
        rootBoneName = result.skeleton.bones.firstOrNull { it.parentId == null }?.name
        val animations = result.animations.toMutableList()

        for ((file, state) in ANIMATION_FILES) {
            val anims = ModelLoader.loadAnimationsFromResources(file)
            if (anims.isNotEmpty()) {
                stateToAnimIndex[state] = animations.size
                animations.addAll(anims.map { anim ->
                    var a = anim.remapChannels(nodeNames)
                    // Strip root motion at load time for non-dodge animations;
                    // dodge animations use runtime root motion extraction instead
                    if (rootBoneName != null && !state.isDodge) a = a.stripRootMotion(rootBoneName!!)
                    a
                })
            }
        }

        if (animations.isNotEmpty()) {
            animationPlayer = AnimationPlayer(result.skeleton, animations)
            animated = true
            stateToAnimIndex[PlayerState.IDLE]?.let { animationPlayer!!.playAnimation(it, 0f) }
        }
    }

    fun update(deltaSeconds: Float, cameraYaw: Float = 0f) {
        val ap = animationPlayer ?: return

        // Check if a non-looping action animation finished (death stays permanent)
        if (currentState.isAction && currentState != PlayerState.DEATH && ap.isFinished) {
            player.state = PlayerState.IDLE
        }

        // Switch animation when state changes
        if (player.state != currentState) {
            currentState = player.state

            if (currentState.isDodge) {
                dodgeStartX = player.x
                dodgeStartZ = player.z
                ap.rootMotionBoneName = rootBoneName
            } else {
                ap.rootMotionBoneName = null
            }

            ap.speed = if (currentState == PlayerState.DRINK) 2f else 1f

            val animIndex = stateToAnimIndex[currentState]
            if (animIndex != null) {
                ap.looping = currentState.isLooping
                ap.playAnimation(animIndex, crossfadeDuration(currentState))
            }
        }

        ap.update(deltaSeconds)

        // Apply root motion offset to player position during dodge (with wall collision)
        // Rotate model-space root motion by the model's rotation (camera.yaw - 90°) to get world-space movement
        if (currentState.isDodge) {
            val modelRot = Math.toRadians((cameraYaw - 90f).toDouble())
            val cosR = cos(modelRot).toFloat()
            val sinR = sin(modelRot).toFloat()
            val localX = ap.rootMotionOffsetX * MODEL_SCALE * DODGE_DISTANCE_MULTIPLIER
            val localZ = ap.rootMotionOffsetZ * MODEL_SCALE * DODGE_DISTANCE_MULTIPLIER
            val worldX = localX * cosR - localZ * sinR
            val worldZ = localX * sinR + localZ * cosR
            player.tryMoveTo(dodgeStartX + worldX, dodgeStartZ + worldZ)
        }
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        shader.setMatrix4("model", camera.modelMatrix(player.x, player.y, player.z, MODEL_SCALE, camera.yaw - 90f))

        if (animated) {
            shader.setBool("animated", true)
            shader.setMatrix4Array("boneMatrices", animationPlayer!!.getBoneMatrices())
        }

        for (i in meshes.indices) {
            val mesh = meshes[i]
            val mat = materials.getOrNull(i) ?: DEFAULT_MATERIAL
            if (mat.texture != null) {
                mat.texture.bind(0)
                shader.setInt("diffuseTexture", 0)
                shader.setBool("useTexture", true)
            } else {
                shader.setBool("useTexture", false)
                shader.setVec3("objectColor", mat.diffuse.r, mat.diffuse.g, mat.diffuse.b)
            }
            mesh.draw()
        }

        shader.setBool("useTexture", false)
        if (animated) {
            shader.setBool("animated", false)
        }
    }

    fun cleanup() {
        meshes.forEach { it.cleanup() }
        materials.mapNotNull { it.texture }.distinct().forEach { it.cleanup() }
    }

    private fun crossfadeDuration(state: PlayerState): Float = when {
        state.isDodge -> 0.1f
        state == PlayerState.PUNCH || state == PlayerState.KICK || state == PlayerState.HIT || state == PlayerState.DEATH -> 0.1f
        state.isMovement -> 0.2f
        state == PlayerState.IDLE -> 0.25f
        else -> 0.2f
    }
}
