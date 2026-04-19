package com.glycin.springaria.gameplay

import com.glycin.springaria.world.Camera
import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants
import com.glycin.springaria.world.WorldConstants.CHUNK_SIZE
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import com.glycin.springaria.world.repositories.Tile
import org.springframework.stereotype.Service
import kotlin.random.Random

private const val SIMULATION_RADIUS_CHUNKS = 3
private val NEIGHBORS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

@Service
class FluidSimulationService(
    private val world: World,
    private val camera: Camera,
) {

    private val fluidsByChunk = HashMap<Long, HashSet<Long>>()
    private val tickBuffer = ArrayList<Long>()

    fun registerFluids() {
        fluidsByChunk.clear()
        for (y in 0..<WORLD_HEIGHT_TILES) {
            for (x in 0..<WORLD_WIDTH_TILES) {
                if (world[x, y].isLiquid) {
                    addFluid(x, y)
                }
            }
        }
    }

    fun registerFluid(x: Int, y: Int) {
        addFluid(x, y)
    }

    fun registerAdjacentFluids(tileX: Int, tileY: Int) {
        for ((dx, dy) in NEIGHBORS) {
            val nx = tileX + dx
            val ny = tileY + dy
            if (world[nx, ny].isLiquid) {
                addFluid(nx, ny)
            }
        }
    }

    fun registerAdjacentFluids(centerX: Int, centerY: Int, radius: Int) {
        val r = radius + 1
        val rSq = r * r
        for (dy in -r..r) {
            for (dx in -r..r) {
                if (dx * dx + dy * dy > rSq) continue
                val nx = centerX + dx
                val ny = centerY + dy
                if (world[nx, ny].isLiquid) {
                    addFluid(nx, ny)
                }
            }
        }
    }

    fun tick() {
        val activeChunks = world.getActiveChunks(camera, SIMULATION_RADIUS_CHUNKS)
        if (activeChunks.isEmpty()) return

        tickBuffer.clear()
        for (chunk in activeChunks) {
            val chunkKey = WorldConstants.packCoords(chunk.chunkX, chunk.chunkY)
            val fluids = fluidsByChunk[chunkKey] ?: continue
            tickBuffer.addAll(fluids)
        }

        tickBuffer.sortByDescending { WorldConstants.unpackY(it) }

        for (packed in tickBuffer) {
            val x = WorldConstants.unpackX(packed)
            val y = WorldConstants.unpackY(packed)
            val tile = world[x, y]
            if (!tile.isLiquid) {
                removeFluid(x, y)
                continue
            }
            updateFluid(x, y, tile)
        }
    }

    private fun updateFluid(x: Int, y: Int, fluid: Tile) {
        // 1. Flow down
        if (y < WORLD_HEIGHT_TILES - 1) {
            val below = world[x, y + 1]
            if (below == Tile.AIR) {
                moveTo(x, y, x, y + 1, fluid)
                return
            }
            if (below.isLiquid && below != fluid) {
                solidify(x, y, x, y + 1)
                return
            }
        }

        // 2. Flow diagonally down
        val diagLeft = x > 0 && y < WORLD_HEIGHT_TILES - 1
        val diagRight = x < WORLD_WIDTH_TILES - 1 && y < WORLD_HEIGHT_TILES - 1

        val canDiagLeft = diagLeft && world[x - 1, y + 1] == Tile.AIR
        val canDiagRight = diagRight && world[x + 1, y + 1] == Tile.AIR
        val solidifyDiagLeft = diagLeft && isOpposite(fluid, x - 1, y + 1)
        val solidifyDiagRight = diagRight && isOpposite(fluid, x + 1, y + 1)

        if (solidifyDiagLeft || solidifyDiagRight) {
            val tx = pickSide(x, solidifyDiagLeft, solidifyDiagRight)
            solidify(x, y, tx, y + 1)
            return
        }

        if (canDiagLeft || canDiagRight) {
            val tx = pickSide(x, canDiagLeft, canDiagRight)
            moveTo(x, y, tx, y + 1, fluid)
            return
        }

        // 3. Flow sideways
        val canLeft = x > 0 && world[x - 1, y] == Tile.AIR
        val canRight = x < WORLD_WIDTH_TILES - 1 && world[x + 1, y] == Tile.AIR
        val solidifyLeft = x > 0 && isOpposite(fluid, x - 1, y)
        val solidifyRight = x < WORLD_WIDTH_TILES - 1 && isOpposite(fluid, x + 1, y)

        if (solidifyLeft || solidifyRight) {
            val tx = pickSide(x, solidifyLeft, solidifyRight)
            solidify(x, y, tx, y)
            return
        }

        if (canLeft || canRight) {
            val tx = pickSide(x, canLeft, canRight)
            moveTo(x, y, tx, y, fluid)
        }
    }

    private fun addFluid(x: Int, y: Int) {
        val chunkKey = WorldConstants.packCoords(x / CHUNK_SIZE, y / CHUNK_SIZE)
        fluidsByChunk.getOrPut(chunkKey) { HashSet() }.add(WorldConstants.packCoords(x, y))
    }

    private fun removeFluid(x: Int, y: Int) {
        val chunkKey = WorldConstants.packCoords(x / CHUNK_SIZE, y / CHUNK_SIZE)
        val set = fluidsByChunk[chunkKey] ?: return
        set.remove(WorldConstants.packCoords(x, y))
        if (set.isEmpty()) fluidsByChunk.remove(chunkKey)
    }

    private fun moveTo(fromX: Int, fromY: Int, toX: Int, toY: Int, fluid: Tile) {
        world[toX, toY] = fluid
        world[fromX, fromY] = Tile.AIR
        removeFluid(fromX, fromY)
        addFluid(toX, toY)
    }

    private fun solidify(x: Int, y: Int, targetX: Int, targetY: Int) {
        world[x, y] = Tile.STONE
        world[targetX, targetY] = Tile.STONE
        removeFluid(x, y)
        removeFluid(targetX, targetY)
    }

    private fun isOpposite(fluid: Tile, x: Int, y: Int): Boolean {
        val other = world[x, y]
        return other.isLiquid && other != fluid
    }

    private fun pickSide(x: Int, left: Boolean, right: Boolean) = when {
        left && right -> if (Random.nextBoolean()) x - 1 else x + 1
        left -> x - 1
        else -> x + 1
    }
}
