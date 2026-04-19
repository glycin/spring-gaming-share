package com.glycin.springsouls.render

import com.glycin.springsouls.EnvironmentSettings
import com.glycin.springsouls.LevelService
import com.glycin.springsouls.TILE_EMPTY
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

@Service
class LevelRenderService(
    private val levelService: LevelService,
    private val environmentSettings: EnvironmentSettings,
) {

    private lateinit var wallPositions: List<Pair<Int, Int>>
    private val doorPositions = CopyOnWriteArrayList<Pair<Int, Int>>()
    private val bossWallPositions = CopyOnWriteArrayList<Pair<Int, Int>>()
    private var groundMesh: Mesh? = null
    private var cubeMesh: Mesh? = null
    private var floorTexture: Texture? = null
    private var wallTexture: Texture? = null

    fun initMeshes() {
        this.cubeMesh = Mesh.createCube(
            environmentSettings.tileSizeX,
            environmentSettings.tileSizeZ,
            environmentSettings.tileSizeY,
        )
        val groundSize = maxOf(levelService.width, levelService.depth).toFloat()
        groundMesh = Mesh.createPlane(groundSize)

        floorTexture = Texture.fromResource("textures/floor.png")
        wallTexture = Texture.fromResource("textures/wall.png")

        wallPositions = levelService.getWallTiles()
        doorPositions.addAll(levelService.getDoorTiles())
        bossWallPositions.addAll(levelService.getBossWallTiles())
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        val halfW = levelService.width / 2f
        val halfD = levelService.depth / 2f

        // Floor
        shader.setBool("useTexture", true)
        floorTexture?.bind(0)
        shader.setInt("diffuseTexture", 0)
        shader.setMatrix4("model", camera.modelMatrix(halfW, 0f, halfD))
        groundMesh?.draw()

        // Walls
        wallTexture?.bind(0)
        val halfHeight = environmentSettings.tileSizeZ / 2f
        for ((wx, wz) in wallPositions) {
            shader.setMatrix4("model", camera.modelMatrix(wx + 0.5f, halfHeight, wz + 0.5f))
            cubeMesh?.draw()
        }

        // Doors (yellow, no texture)
        shader.setBool("useTexture", false)
        shader.setVec3("objectColor", 0.9f, 0.8f, 0.1f)
        for ((dx, dz) in doorPositions) {
            shader.setMatrix4("model", camera.modelMatrix(dx + 0.5f, halfHeight, dz + 0.5f))
            cubeMesh?.draw()
        }

        // Boss walls (white foggy)
        shader.setVec3("objectColor", 0.9f, 0.9f, 1.0f)
        for ((bx, bz) in bossWallPositions) {
            shader.setMatrix4("model", camera.modelMatrix(bx + 0.5f, halfHeight, bz + 0.5f))
            cubeMesh?.draw()
        }
    }

    fun removeDoor(tileX: Int, tileZ: Int) {
        doorPositions.remove(tileX to tileZ)
    }

    fun removeAllBossWalls() {
        for ((bx, bz) in bossWallPositions) {
            levelService.setTile(bx, bz, TILE_EMPTY)
        }
        bossWallPositions.clear()
    }

    fun cleanup() {
        groundMesh?.cleanup()
        cubeMesh?.cleanup()
        floorTexture?.cleanup()
        wallTexture?.cleanup()
    }
}
