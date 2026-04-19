package com.glycin.springsouls.render

import com.glycin.springsouls.LevelService
import com.glycin.springsouls.gameplay.Enemy
import com.glycin.springsouls.gameplay.EnemyState
import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.gameplay.PlayerState
import com.glycin.springsouls.metrics.GameMetricsService
import com.glycin.springsouls.render.animation.AnimationPlayer
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

private const val MODEL_SCALE = 0.01f
private const val AGGRO_RANGE = 8f
private const val ATTACK_COOLDOWN = 3f
private const val SPELL_SPEED = 12.5f
private const val SPELL_RADIUS = 0.25f
private const val SPELL_HIT_RADIUS = 0.6f
private const val SPELL_DAMAGE = 10
private const val SPELL_MAX_DISTANCE = 20f
private const val SPELL_HEIGHT = 1.5f
private const val SPELL_CAST_DELAY = 1.1f
private val DEFAULT_MATERIAL = MeshMaterial()

private val ANIMATION_FILES = listOf(
    "models/enemy/enemy_idle.fbx" to EnemyState.IDLE,
    "models/enemy/enemy_attack.fbx" to EnemyState.ATTACK,
    "models/enemy/enemy_hit.fbx" to EnemyState.HIT,
    "models/enemy/enemy_death.fbx" to EnemyState.DEATH,
)

@Service
class EnemyRenderService(
    private val levelService: LevelService,
    private val player: Player,
    private val metricsService: GameMetricsService,
) {
    private var meshes: List<Mesh> = emptyList()
    private var materials: List<MeshMaterial> = emptyList()
    private var spellMesh: Mesh? = null

    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val spells = CopyOnWriteArrayList<Spell>()

    private val stateToAnimIndex = mutableMapOf<EnemyState, Int>()

    var onAllEnemiesDefeated: (() -> Unit)? = null
    private var initialized = false

    private val attackCooldowns = mutableMapOf<Enemy, Float>()
    private val pendingSpells = mutableMapOf<Enemy, Float>()

    data class Spell(
        var x: Float,
        var y: Float,
        var z: Float,
        val dirX: Float,
        val dirZ: Float,
        val startX: Float,
        val startZ: Float,
    )

    fun initMeshes() {
        val result = ModelLoader.loadAnimatedFromResources("models/enemy/enemy.fbx")
        meshes = result.meshes
        materials = result.materials

        val nodeNames = result.skeleton.nodes.map { it.name }.toSet()
        val animations = result.animations.toMutableList()

        for ((file, state) in ANIMATION_FILES) {
            val anims = ModelLoader.loadAnimationsFromResources(file)
            if (anims.isNotEmpty()) {
                stateToAnimIndex[state] = animations.size
                animations.addAll(anims.map { it.remapChannels(nodeNames) })
            }
        }

        spellMesh = Mesh.createSphere(SPELL_RADIUS)

        for ((tx, tz) in levelService.getEnemyTiles()) {
            val enemy = Enemy(tx + 0.5f, tz + 0.5f, tx, tz).apply {
                animationPlayer = AnimationPlayer(result.skeleton, animations)
                stateToAnimIndex[EnemyState.IDLE]?.let { animationPlayer!!.playAnimation(it, 0f) }
            }
            enemies.add(enemy)
            attackCooldowns[enemy] = 0f
        }
        initialized = true
    }

    fun update(deltaSeconds: Float) {
        val deadToRemove = mutableSetOf<Enemy>()

        for (enemy in enemies) {
            val ap = enemy.animationPlayer ?: continue

            // Handle death animation finishing
            if (enemy.isDead) {
                ap.update(deltaSeconds)
                if (ap.isFinished) {
                    deadToRemove.add(enemy)
                }
                continue
            }

            // Handle action animations finishing
            if (enemy.state.isAction && ap.isFinished) {
                enemy.state = EnemyState.IDLE
            }

            // Cooldown
            val cooldown = ((attackCooldowns[enemy] ?: 0f) - deltaSeconds).coerceAtLeast(0f)
            attackCooldowns[enemy] = cooldown

            // Check distance to player
            val dx = player.x - enemy.x
            val dz = player.z - enemy.z
            val distSq = dx * dx + dz * dz

            if (distSq < AGGRO_RANGE * AGGRO_RANGE && player.isAlive) {
                enemy.faceToward(player.x, player.z)

                if (enemy.state == EnemyState.IDLE && cooldown <= 0f) {
                    enemy.state = EnemyState.ATTACK
                    attackCooldowns[enemy] = ATTACK_COOLDOWN
                    pendingSpells[enemy] = SPELL_CAST_DELAY
                }
            }

            val pending = pendingSpells[enemy]
            if (pending != null) {
                val remaining = pending - deltaSeconds
                if (remaining <= 0f) {
                    pendingSpells.remove(enemy)
                    val sdx = player.x - enemy.x
                    val sdz = player.z - enemy.z
                    val dist = sqrt(sdx * sdx + sdz * sdz)
                    if (dist > 0f) {
                        spells.add(Spell(
                            x = enemy.x - 1,
                            y = SPELL_HEIGHT,
                            z = enemy.z,
                            dirX = sdx / dist,
                            dirZ = sdz / dist,
                            startX = enemy.x,
                            startZ = enemy.z,
                        ))
                    }
                } else {
                    pendingSpells[enemy] = remaining
                }
            }

            // Switch animation when state changes
            val animIndex = stateToAnimIndex[enemy.state]
            if (animIndex != null && animIndex != ap.currentAnimationIndex) {
                ap.looping = enemy.state.isLooping
                ap.playAnimation(animIndex, 0.15f)
            }

            ap.update(deltaSeconds)
        }

        if (deadToRemove.isNotEmpty()) {
            enemies.removeAll(deadToRemove)
            attackCooldowns.keys.removeAll(deadToRemove)
            pendingSpells.keys.removeAll(deadToRemove)
            if (enemies.isEmpty()) {
                onAllEnemiesDefeated?.invoke()
                onAllEnemiesDefeated = null
            }
        }

        val spellsToRemove = mutableListOf<Spell>()
        for (spell in spells) {
            spell.x += spell.dirX * SPELL_SPEED * deltaSeconds
            spell.z += spell.dirZ * SPELL_SPEED * deltaSeconds

            // Check hit on player
            val dx = player.x - spell.x
            val dz = player.z - spell.z
            if (dx * dx + dz * dz < SPELL_HIT_RADIUS * SPELL_HIT_RADIUS && player.isAlive && !player.state.isDodge) {
                val dealt = player.takeDamage(SPELL_DAMAGE)
                metricsService.recordDamageReceived(dealt.toDouble(), "enemy_spell")
                player.state = if (player.isAlive) PlayerState.HIT else PlayerState.DEATH
                spellsToRemove.add(spell)
                continue
            }

            val tdx = spell.x - spell.startX
            val tdz = spell.z - spell.startZ
            if (tdx * tdx + tdz * tdz > SPELL_MAX_DISTANCE * SPELL_MAX_DISTANCE) {
                spellsToRemove.add(spell)
            }
        }
        spells.removeAll(spellsToRemove.toSet())
    }

    fun damageEnemyAt(worldX: Float, worldZ: Float, radius: Float, damage: Int): Boolean {
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dx = worldX - enemy.x
            val dz = worldZ - enemy.z
            if (dx * dx + dz * dz < radius * radius) {
                val dealt = enemy.takeDamage(damage)
                metricsService.recordDamageDealt(dealt.toDouble(), "player", "enemy")
                if (!enemy.isAlive) metricsService.recordEnemyKilled("enemy")
                transitionEnemy(enemy, if (enemy.isAlive) EnemyState.HIT else EnemyState.DEATH)
                return true
            }
        }
        return false
    }

    private fun transitionEnemy(enemy: Enemy, state: EnemyState) {
        enemy.state = state
        val animIndex = stateToAnimIndex[state] ?: return
        enemy.animationPlayer?.let {
            it.looping = state.isLooping
            it.playAnimation(animIndex, 0.1f)
        }
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        if (meshes.isEmpty()) return

        // Render enemies
        for (enemy in enemies) {
            val ap = enemy.animationPlayer ?: continue

            shader.setMatrix4("model", camera.modelMatrix(
                enemy.x, 0f, enemy.z, MODEL_SCALE, enemy.facingYaw - 90f,
            ))

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
        }

        shader.setBool("animated", false)

        // Render spells
        val sm = spellMesh ?: return
        if (spells.isEmpty()) return

        shader.setBool("useTexture", false)
        shader.setVec3("objectColor", 1.0f, 0.55f, 0.0f)

        for (spell in spells) {
            shader.setMatrix4("model", camera.modelMatrix(spell.x, spell.y, spell.z))
            sm.draw()
        }
    }

    fun cleanup() {
        meshes.forEach { it.cleanup() }
        materials.mapNotNull { it.texture }.distinct().forEach { it.cleanup() }
        spellMesh?.cleanup()
    }
}
