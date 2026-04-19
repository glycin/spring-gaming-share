package com.glycin.springaria.world

import com.glycin.annotations.WithLoopingSound
import com.glycin.springaria.util.SimplexNoise
import com.glycin.springaria.world.WorldConstants.CHUNK_SIZE
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import com.glycin.springaria.world.repositories.Tile
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.stream.IntStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

@Service
class WorldGeneratorService(
    private val world: World,
    private val beanGraphService: BeanGraphService,
    applicationContext: ApplicationContext,
) {

    private val logger = LoggerFactory.getLogger(WorldGeneratorService::class.java)

    private val seed = applicationContext.beanDefinitionCount.toLong()

    var generatedCaves: List<BeanGraphService.BeanCave> = emptyList()

    // Layer boundaries (noise-warped, these are base values)
    private val dirtLayer = WORLD_HEIGHT_TILES / 5           // 0..~1638: dirt/sand/clay
    private val stoneLayer = WORLD_HEIGHT_TILES * 2 / 5      // ~1638..~3276: stone/gravel/copper
    private val deepStoneLayer = WORLD_HEIGHT_TILES * 3 / 5  // ~3276..~4915: deepstone/iron
    private val obsidianLayer = WORLD_HEIGHT_TILES * 4 / 5   // ~4915..~6553: obsidian/gold/crystal
                                                              // ~6553..8192: obsidian/lava

    @WithLoopingSound("sounds/background-music.wav", volume = 0.2f)
    fun generate() {
        logger.info("Generating world with seed: {} (bean count)", seed)
        val start = System.currentTimeMillis()

        fillSolid()

        generatedCaves = beanGraphService.buildCaveLayout(seed)
        carveIoCCaves(generatedCaves)
        fillCaveFluids(generatedCaves)

        logger.info("World generated in {}ms ({} bean caves)", System.currentTimeMillis() - start, generatedCaves.size)
    }

    private fun fillSolid() {
        // Noise instances are stateless after construction — create once and share across threads
        val patchNoise = SimplexNoise(seed * 6271)
        val oreNoise = SimplexNoise(seed * 3541)
        val layerWarpNoise = SimplexNoise(seed * 8123)
        val detailNoise = SimplexNoise(seed * 2903)

        IntStream.range(0, world.chunksWide * world.chunksHigh).parallel().forEach { i ->
            val chunkX = i % world.chunksWide
            val chunkY = i / world.chunksWide
            val chunk = world.getChunk(chunkX, chunkY) ?: return@forEach
            fillChunkSolid(chunk, patchNoise, oreNoise, layerWarpNoise, detailNoise)
        }
    }

    private fun fillChunkSolid(chunk: Chunk, patchNoise: SimplexNoise, oreNoise: SimplexNoise, layerWarpNoise: SimplexNoise, detailNoise: SimplexNoise) {

        val worldXBase = chunk.chunkX * CHUNK_SIZE
        val worldYBase = chunk.chunkY * CHUNK_SIZE

        for (localY in 0..<CHUNK_SIZE) {
            val worldY = worldYBase + localY
            for (localX in 0..<CHUNK_SIZE) {
                val worldX = worldXBase + localX
                if (worldX >= WORLD_WIDTH_TILES || worldY >= WORLD_HEIGHT_TILES) continue

                val warp = (layerWarpNoise.noise(worldX * 0.005f, worldY * 0.01f) * 300).toInt()
                val warpedY = worldY + warp

                val tile = generateTile(worldX, worldY, warpedY, patchNoise, oreNoise, detailNoise)
                chunk.setTileAt(localX, localY, tile)
            }
        }
    }

    private fun generateTile(
        x: Int, y: Int, warpedY: Int,
        patchNoise: SimplexNoise, oreNoise: SimplexNoise, detailNoise: SimplexNoise,
    ): Tile {
        return when {
            warpedY < dirtLayer -> generateDirtTile(x, y, patchNoise, oreNoise, detailNoise)
            warpedY < stoneLayer -> generateStoneTile(x, y, patchNoise, oreNoise, detailNoise)
            warpedY < deepStoneLayer -> generateDeepStoneTile(x, y, patchNoise, oreNoise, detailNoise)
            warpedY < obsidianLayer -> generateObsidianTile(x, y, patchNoise, oreNoise, detailNoise)
            else -> generateDeepTile(x, y, patchNoise, oreNoise, detailNoise)
        }
    }

    private fun generateDirtTile(x: Int, y: Int, patch: SimplexNoise, ore: SimplexNoise, detail: SimplexNoise): Tile {
        val sand = patch.noise(x * 0.02f, y * 0.025f)
        val clay = detail.noise(x * 0.04f, y * 0.06f + 500f)
        val gravel = patch.noise(x * 0.08f, y * 0.07f + 1000f)
        val lava = ore.noise(x * 0.03f, y * 0.04f + 20000f)
        return when {
            lava < 0.03f -> Tile.LAVA
            sand > 0.72f -> Tile.SAND
            clay > 0.8f -> Tile.CLAY
            gravel > 0.85f -> Tile.GRAVEL
            else -> Tile.DIRT
        }
    }

    private fun generateStoneTile(x: Int, y: Int, patch: SimplexNoise, ore: SimplexNoise, detail: SimplexNoise): Tile {
        val gravel = patch.noise(x * 0.06f, y * 0.04f + 2000f)
        val dirt = detail.noise(x * 0.03f, y * 0.05f + 3000f)
        val copper = ore.noise(x * 0.12f, y * 0.1f + 4000f)
        val lava = ore.noise(x * 0.03f, y * 0.04f + 21000f)
        return when {
            lava < 0.04f -> Tile.LAVA
            copper > 0.88f -> Tile.COPPER_ORE
            gravel > 0.8f -> Tile.GRAVEL
            dirt > 0.83f -> Tile.DIRT
            else -> Tile.STONE
        }
    }

    private fun generateDeepStoneTile(x: Int, y: Int, patch: SimplexNoise, ore: SimplexNoise, detail: SimplexNoise): Tile {
        val iron = ore.noise(x * 0.1f, y * 0.13f + 6000f)
        val stone = patch.noise(x * 0.04f, y * 0.035f + 7000f)
        val gravel = detail.noise(x * 0.07f, y * 0.09f + 8000f)
        val lava = ore.noise(x * 0.03f, y * 0.04f + 22000f)
        return when {
            lava < 0.06f -> Tile.LAVA
            iron > 0.87f -> Tile.IRON_ORE
            stone > 0.82f -> Tile.STONE
            gravel > 0.88f -> Tile.GRAVEL
            else -> Tile.DEEPSTONE
        }
    }

    private fun generateObsidianTile(x: Int, y: Int, patch: SimplexNoise, ore: SimplexNoise, detail: SimplexNoise): Tile {
        val gold = ore.noise(x * 0.15f, y * 0.12f + 10000f)
        val crystal = detail.noise(x * 0.09f, y * 0.11f + 11000f)
        val deepstone = patch.noise(x * 0.035f, y * 0.04f + 12000f)
        val lava = ore.noise(x * 0.04f, y * 0.05f + 13000f)
        return when {
            gold > 0.9f -> Tile.GOLD_ORE
            crystal > 0.92f -> Tile.CRYSTAL
            lava < 0.08f -> Tile.LAVA
            deepstone > 0.83f -> Tile.DEEPSTONE
            else -> Tile.OBSIDIAN
        }
    }

    private fun generateDeepTile(x: Int, y: Int, patch: SimplexNoise, ore: SimplexNoise, detail: SimplexNoise): Tile {
        val lava = patch.noise(x * 0.03f, y * 0.04f + 15000f)
        val crystal = detail.noise(x * 0.11f, y * 0.08f + 16000f)
        val deepstone = ore.noise(x * 0.05f, y * 0.06f + 17000f)
        return when {
            lava < 0.18f -> Tile.LAVA
            crystal > 0.91f -> Tile.CRYSTAL
            deepstone > 0.82f -> Tile.DEEPSTONE
            else -> Tile.OBSIDIAN
        }
    }

    private fun carveIoCCaves(caves: List<BeanGraphService.BeanCave>) {
        val cavernNoise = SimplexNoise(seed * 9001)
        val tunnelNoise = SimplexNoise(seed * 4242)
        val caveMap = caves.associateBy { it.name }

        for (cave in caves) {
            carveCavern(cave.worldX, cave.worldY, cave.radius, cavernNoise)
        }

        for (cave in caves) {
            for (depName in cave.dependencies) {
                val dep = caveMap[depName] ?: continue
                val width = if (cave.isUserBean && dep.isUserBean) 7 else 4
                carveTunnel(cave.worldX, cave.worldY, dep.worldX, dep.worldY, width, tunnelNoise)
            }
        }
    }

    private fun carveCavern(centerX: Int, centerY: Int, baseRadius: Int, noise: SimplexNoise) {
        val scan = (baseRadius * 1.5).toInt()
        for (dy in -scan..scan) {
            for (dx in -scan..scan) {
                val x = centerX + dx
                val y = centerY + dy
                if (x !in 0..<WORLD_WIDTH_TILES || y !in 0..<WORLD_HEIGHT_TILES) continue

                val normalizedDist = sqrt((dx * dx + dy * dy).toDouble()) / baseRadius
                val noiseVal = noise.noise(x * 0.08f, y * 0.08f)
                val threshold = 1.0 + (noiseVal - 0.5) * 0.8

                if (normalizedDist < threshold) {
                    world[x, y] = Tile.AIR
                }
            }
        }
    }

    private fun carveTunnel(x1: Int, y1: Int, x2: Int, y2: Int, width: Int, noise: SimplexNoise) {
        val waypoints = buildTunnelWaypoints(x1, y1, x2, y2, noise)

        for (i in 0..<waypoints.size - 1) {
            val (ax, ay) = waypoints[i]
            val (bx, by) = waypoints[i + 1]
            carveSegment(ax, ay, bx, by, width, noise)
        }
    }

    private fun buildTunnelWaypoints(x1: Int, y1: Int, x2: Int, y2: Int, noise: SimplexNoise): List<Pair<Int, Int>> {
        val dx = (x2 - x1).toDouble()
        val dy = (y2 - y1).toDouble()
        val length = sqrt(dx * dx + dy * dy)
        if (length < 1) return listOf(x1 to y1, x2 to y2)

        val perpX = -dy / length
        val perpY = dx / length

        val segments = 3 + ((noise.noise(x1 * 0.01f, y1 * 0.01f) * 3).toInt().coerceIn(0, 2))
        val points = mutableListOf(x1 to y1)

        for (i in 1..<segments) {
            val t = i.toDouble() / segments
            val midX = x1 + dx * t
            val midY = y1 + dy * t

            val offsetScale = length * 0.15
            val offset = (noise.noise(i * 0.7f + x1 * 0.02f, i * 0.7f + y1 * 0.02f) - 0.5f) * 2 * offsetScale

            val wx = (midX + perpX * offset).toInt().coerceIn(0, WORLD_WIDTH_TILES - 1)
            val wy = (midY + perpY * offset).toInt().coerceIn(0, WORLD_HEIGHT_TILES - 1)
            points.add(wx to wy)
        }

        points.add(x2 to y2)
        return points
    }

    private fun carveSegment(x1: Int, y1: Int, x2: Int, y2: Int, width: Int, noise: SimplexNoise) {
        val dx = x2 - x1
        val dy = y2 - y1
        val steps = maxOf(abs(dx), abs(dy))
        if (steps == 0) return

        for (step in 0..steps) {
            val t = step.toDouble() / steps
            val cx = (x1 + dx * t).toInt()
            val cy = (y1 + dy * t).toInt()

            val wobble = ((noise.noise(step * 0.05f, (x1 * 31 + y1).toFloat()) - 0.5f) * width * 3).toInt()

            for (ry in -width..width) {
                for (rx in -width..width) {
                    if (rx * rx + ry * ry <= width * width) {
                        val tx = cx + rx + wobble
                        val ty = cy + ry
                        if (tx in 0..<WORLD_WIDTH_TILES && ty in 0..<WORLD_HEIGHT_TILES) {
                            world[tx, ty] = Tile.AIR
                        }
                    }
                }
            }
        }
    }

    private fun fillCaveFluids(caves: List<BeanGraphService.BeanCave>) {
        val fluidNoise = SimplexNoise(seed * 7719)

        for (cave in caves) {
            // Use noise to decide if this cave gets fluid (~60% of caves)
            val chance = fluidNoise.noise(cave.worldX * 0.01f, cave.worldY * 0.01f)
            if (chance < 0.4f) continue

            // Mostly water, lava becomes more likely the deeper you go
            val fluid = when {
                cave.worldY > deepStoneLayer -> Tile.LAVA
                cave.worldY > stoneLayer -> if (chance > 0.75f) Tile.LAVA else Tile.WATER
                cave.worldY > dirtLayer -> if (chance > 0.85f) Tile.LAVA else Tile.WATER
                else -> Tile.WATER
            }

            // Scan for the cave floor: find the lowest AIR tiles in the cave radius
            val scan = cave.radius
            val fluidDepth = max(1, (cave.radius * 0.15).toInt()) // 1-3 tiles deep for small caves

            for (dx in -scan..scan) {
                val x = cave.worldX + dx
                if (x !in 0..<WORLD_WIDTH_TILES) continue

                // Find the floor at this column (lowest AIR tile with solid below)
                for (dy in scan downTo -scan) {
                    val y = cave.worldY + dy
                    if (y !in 1..<WORLD_HEIGHT_TILES) continue

                    if (world[x, y] == Tile.AIR && world[x, y + 1].solid) {
                        // Fill upward from the floor
                        val localDepthVariation = (fluidNoise.noise(x * 0.1f, y * 0.1f) * 2).toInt()
                        val depth = (fluidDepth + localDepthVariation).coerceAtLeast(1)
                        for (fy in 0..<depth) {
                            val fy2 = y - fy
                            if (fy2 >= 0 && world[x, fy2] == Tile.AIR) {
                                world[x, fy2] = fluid
                            }
                        }
                        break
                    }
                }
            }
        }
    }
}
