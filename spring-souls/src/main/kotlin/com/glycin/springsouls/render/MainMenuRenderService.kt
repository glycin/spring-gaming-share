package com.glycin.springsouls.render

import com.glycin.springsouls.GameSettings
import com.glycin.springsouls.render.animation.AnimationPlayer
import org.springframework.stereotype.Service

private const val MODEL_SCALE = 0.01f
private const val PLAYER_ROTATION = -40f
private val DEFAULT_MATERIAL = MeshMaterial()

@Service
class MainMenuRenderService(
    private val gameSettings: GameSettings,
    private val hudRenderService: HudRenderService,
) {
    private var meshes: List<Mesh> = emptyList()
    private var materials: List<MeshMaterial> = emptyList()
    private var animationPlayer: AnimationPlayer? = null

    private val menuCamera = Camera3D(
        x = 1.5f, y = 1.2f, z = 2.5f,
        yaw = -90f, pitch = -5f,
    )

    fun initMeshes() {
        val result = ModelLoader.loadAnimatedFromResources("models/ninja_nopbr.fbx")
        meshes = result.meshes
        materials = result.materials

        val nodeNames = result.skeleton.nodes.map { it.name }.toSet()
        val rootBoneName = result.skeleton.bones.firstOrNull { it.parentId == null }?.name
        val animations = result.animations.toMutableList()

        val idleAnims = ModelLoader.loadAnimationsFromResources("models/ninja_idle.fbx")
        if (idleAnims.isNotEmpty()) {
            val idleIndex = animations.size
            animations.addAll(idleAnims.map { anim ->
                var a = anim.remapChannels(nodeNames)
                if (rootBoneName != null) a = a.stripRootMotion(rootBoneName)
                a
            })
            animationPlayer = AnimationPlayer(result.skeleton, animations)
            animationPlayer!!.playAnimation(idleIndex, 0f)
        } else if (animations.isNotEmpty()) {
            animationPlayer = AnimationPlayer(result.skeleton, animations)
            animationPlayer!!.playAnimation(0, 0f)
        }
    }

    fun update(deltaSeconds: Float) {
        animationPlayer?.update(deltaSeconds)
    }

    fun render(shader: ShaderProgram) {
        if (meshes.isEmpty()) return
        val ap = animationPlayer ?: return

        val aspect = gameSettings.windowWidth.toFloat() / gameSettings.windowHeight.toFloat()

        shader.setMatrix4("projection", menuCamera.projectionMatrix(aspect))
        shader.setMatrix4("view", menuCamera.viewMatrix())

        shader.setVec3("lightDir", -0.3f, -1.0f, -0.5f)
        shader.setVec3("lightColor", 1.0f, 0.95f, 0.85f)
        shader.setFloat("ambientStrength", 0.3f)
        shader.setVec3("viewPos", menuCamera.x, menuCamera.y, menuCamera.z)
        shader.setVec3("fogColor", 0.0f, 0.0f, 0.0f)
        shader.setFloat("fogNear", 50f)
        shader.setFloat("fogFar", 100f)

        shader.setMatrix4("model", menuCamera.modelMatrix(0f, 0f, 0f, MODEL_SCALE, PLAYER_ROTATION))

        shader.setBool("animated", true)
        shader.setMatrix4Array("boneMatrices", ap.getBoneMatrices())

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
        shader.setBool("animated", false)

        hudRenderService.renderMainMenuText()
    }

    fun cleanup() {
        meshes.forEach { it.cleanup() }
        materials.mapNotNull { it.texture }.distinct().forEach { it.cleanup() }
    }
}
