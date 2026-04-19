package com.glycin.springsouls.render

import com.glycin.springsouls.LevelService
import com.glycin.springsouls.TILE_EMPTY
import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.gameplay.PotionRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

private const val SPHERE_RADIUS = 0.3f
private const val ROTATION_SPEED = 45f
private const val HOVER_HEIGHT = 0.5f
private const val PICKUP_RADIUS = 0.8f

@Service
class PotionRenderService(
    private val levelService: LevelService,
    private val player: Player,
    private val potionRepository: PotionRepository,
) {
    private var sphereMesh: Mesh? = null
    private val potionPositions = CopyOnWriteArrayList<Pair<Int, Int>>()
    private var rotationAngle = 0f

    fun initMeshes() {
        sphereMesh = Mesh.createSphere(SPHERE_RADIUS)
        potionPositions.addAll(levelService.getPotionTiles())
    }

    fun update(deltaSeconds: Float) {
        rotationAngle = (rotationAngle + ROTATION_SPEED * deltaSeconds) % 360f

        val toRemove = mutableListOf<Pair<Int, Int>>()
        for ((px, pz) in potionPositions) {
            val dx = player.x - (px + 0.5f)
            val dz = player.z - (pz + 0.5f)
            if (dx * dx + dz * dz < PICKUP_RADIUS * PICKUP_RADIUS && potionRepository.count() < player.maxPotionCharges) {
                potionRepository.pickup()
                levelService.setTile(px, pz, TILE_EMPTY)
                toRemove.add(px to pz)
            }
        }
        potionPositions.removeAll(toRemove)
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        val mesh = sphereMesh ?: return
        if (potionPositions.isEmpty()) return

        shader.setBool("animated", false)
        shader.setBool("useTexture", false)
        shader.setVec3("objectColor", 1.0f, 0.85f, 0.0f)

        for ((px, pz) in potionPositions) {
            shader.setMatrix4(
                "model",
                camera.modelMatrix(px + 0.5f, HOVER_HEIGHT, pz + 0.5f, 1f, rotationAngle),
            )
            mesh.draw()
        }
    }

    fun cleanup() {
        sphereMesh?.cleanup()
    }
}
