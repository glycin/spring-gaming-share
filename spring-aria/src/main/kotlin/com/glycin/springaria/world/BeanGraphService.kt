package com.glycin.springaria.world

import com.glycin.springaria.util.SimplexNoise
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service

@Service
class BeanGraphService(
    applicationContext: ConfigurableApplicationContext,
) {

    data class BeanCave(
        val name: String,
        val isUserBean: Boolean,
        val worldX: Int,
        val worldY: Int,
        val radius: Int,
        val dependencies: List<String>,
    )

    private val beanFactory = applicationContext.beanFactory
    private val beanNames = applicationContext.beanDefinitionNames.toSet()
    private val beanTypes = beanNames.associateWith { name ->
        try { applicationContext.getType(name) } catch (_: Exception) { null }
    }

    fun buildCaveLayout(seed: Long): List<BeanCave> {
        val dependencies = beanNames.associateWith { name ->
            try {
                beanFactory.getDependenciesForBean(name).filter { it in beanNames }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val isUser = beanNames.associateWith { name ->
            beanTypes[name]?.packageName?.startsWith("com.glycin") == true
        }

        val depths = computeDepths(dependencies)
        return layoutCaves(dependencies, depths, isUser, seed)
    }

    private fun computeDepths(dependencies: Map<String, List<String>>): Map<String, Int> {
        val allDeps = dependencies.values.flatten().toSet()
        val roots = beanNames.filter { it !in allDeps }

        val depths = mutableMapOf<String, Int>()

        fun dfs(name: String, depth: Int) {
            if ((depths[name] ?: -1) >= depth) return
            depths[name] = depth
            for (dep in dependencies[name] ?: emptyList()) {
                dfs(dep, depth + 1)
            }
        }

        for (root in roots) dfs(root, 0)
        for (name in beanNames) depths.putIfAbsent(name, 0)

        return depths
    }

    private fun layoutCaves(
        dependencies: Map<String, List<String>>,
        depths: Map<String, Int>,
        isUser: Map<String, Boolean>,
        seed: Long,
    ): List<BeanCave> {
        val noise = SimplexNoise(seed * 1337)
        val maxDepth = depths.values.maxOrNull() ?: 0

        val layers = beanNames.groupBy { depths[it] ?: 0 }
        val caveZoneTop = 100
        val caveZoneHeight = WORLD_HEIGHT_TILES - 200
        val layerSpacing = if (maxDepth > 0) caveZoneHeight / (maxDepth + 1) else caveZoneHeight

        return layers.flatMap { (depth, beans) ->
            val layerY = caveZoneTop + depth * layerSpacing + layerSpacing / 2
            val hSpacing = WORLD_WIDTH_TILES / (beans.size + 1)

            beans.mapIndexed { index, name ->
                val isUserBean = isUser[name] == true
                val baseX = (index + 1) * hSpacing
                val offsetX = ((noise.noise(name.hashCode() * 0.1f, 0f) - 0.5f) * hSpacing * 0.7).toInt()
                val offsetY = ((noise.noise(0f, name.hashCode() * 0.1f) - 0.5f) * layerSpacing * 0.9).toInt()

                val radius = if (isUserBean) {
                    70 + (noise.noise(name.hashCode() * 0.3f, 100f) * 40).toInt()
                } else {
                    30 + (noise.noise(name.hashCode() * 0.3f, 200f) * 20).toInt()
                }

                BeanCave(
                    name = name,
                    isUserBean = isUserBean,
                    worldX = (baseX + offsetX).coerceIn(100, WORLD_WIDTH_TILES - 100),
                    worldY = (layerY + offsetY).coerceIn(caveZoneTop + 50, caveZoneTop + caveZoneHeight - 50),
                    radius = radius,
                    dependencies = dependencies[name] ?: emptyList(),
                )
            }
        }
    }
}
