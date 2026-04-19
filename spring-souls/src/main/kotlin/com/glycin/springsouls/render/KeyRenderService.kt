package com.glycin.springsouls.render

import com.glycin.springsouls.LevelService
import com.glycin.springsouls.TILE_EMPTY
import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.metrics.GameMetricsService
import com.glycin.springsouls.security.DoorService
import com.glycin.springsouls.security.PlayerAuthentication
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

private const val KEY_SCALE = 0.2f
private const val ROTATION_SPEED = 60f
private const val HOVER_HEIGHT = 0.5f
private const val PICKUP_RADIUS = 0.8f
private const val KEY_AUTHORITY = "KEY_GOLDEN"

@Service
class KeyRenderService(
    private val levelService: LevelService,
    private val levelRenderService: LevelRenderService,
    private val player: Player,
    private val playerAuthentication: PlayerAuthentication,
    private val doorService: DoorService,
    private val hudRenderService: HudRenderService,
    private val metricsService: GameMetricsService,
) {
    private val logger = LoggerFactory.getLogger(KeyRenderService::class.java)

    private var meshes: List<Mesh> = emptyList()
    private var materials: List<MeshMaterial> = emptyList()
    private val keyPositions = CopyOnWriteArrayList<Pair<Int, Int>>()
    private var rotationAngle = 0f
    var keysHeld = 0

    fun initMeshes() {
        val result = ModelLoader.loadAnimatedFromResources("models/goldkey.fbx")
        meshes = result.meshes
        materials = result.materials
        keyPositions.addAll(levelService.getKeyTiles())
    }

    fun update(deltaSeconds: Float) {
        rotationAngle = (rotationAngle + ROTATION_SPEED * deltaSeconds) % 360f

        val toRemove = mutableListOf<Pair<Int, Int>>()
        for ((kx, kz) in keyPositions) {
            val dx = player.x - (kx + 0.5f)
            val dz = player.z - (kz + 0.5f)
            if (dx * dx + dz * dz < PICKUP_RADIUS * PICKUP_RADIUS) {
                keysHeld++
                playerAuthentication.grant(KEY_AUTHORITY)
                levelService.setTile(kx, kz, TILE_EMPTY)
                toRemove.add(kx to kz)
                hudRenderService.showNotification("GRANTED GOLDEN KEY AUTHORITY", 3f, 1f, 0.85f, 0f)
            }
        }
        keyPositions.removeAll(toRemove)
    }

    fun useKey(doorX: Int, doorZ: Int): Boolean {
        try {
            doorService.openGoldenDoor(doorX, doorZ)
        } catch (e: AccessDeniedException) {
            logger.info("Cannot open door at ({}, {}): no '{}' authority", doorX, doorZ, KEY_AUTHORITY)
            hudRenderService.showNotification("YOU HAVE NO AUTHORITEH", 3f, 0.8f, 0.2f, 0.2f)
            metricsService.recordDoorDenied("golden_door")
            return false
        }

        metricsService.recordDoorOpened("golden_door")
        keysHeld = (keysHeld - 1).coerceAtLeast(0)
        levelService.setTile(doorX, doorZ, TILE_EMPTY)
        levelRenderService.removeDoor(doorX, doorZ)

        if (keysHeld <= 0) {
            playerAuthentication.revoke(KEY_AUTHORITY)
            hudRenderService.showNotification("GOLDEN KEY AUTHORITY REVOKED", 3f, 0.8f, 0.3f, 0.1f)
        } else {
            hudRenderService.showNotification("DOOR OPENED - KEY CONSUMED", 3f, 0.9f, 0.8f, 0.1f)
        }

        return true
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        if (meshes.isEmpty() || keyPositions.isEmpty()) return

        shader.setBool("animated", false)

        for ((kx, kz) in keyPositions) {
            shader.setMatrix4(
                "model",
                camera.modelMatrix(kx + 0.5f, HOVER_HEIGHT, kz + 0.5f, KEY_SCALE, rotationAngle, 90f),
            )
            for (i in meshes.indices) {
                val mat = materials.getOrNull(i)
                if (mat?.texture != null) {
                    mat.texture.bind(0)
                    shader.setInt("diffuseTexture", 0)
                    shader.setBool("useTexture", true)
                } else {
                    shader.setBool("useTexture", false)
                    shader.setVec3("objectColor", 1.0f, 0.85f, 0.0f)
                }
                meshes[i].draw()
            }
        }

        shader.setBool("useTexture", false)
    }

    fun cleanup() {
        meshes.forEach { it.cleanup() }
        materials.mapNotNull { it.texture }.distinct().forEach { it.cleanup() }
    }
}
