package com.glycin.springsouls.render

import com.glycin.springsouls.LevelService
import com.glycin.springsouls.TILE_EMPTY
import com.glycin.springsouls.gameplay.Boss
import com.glycin.springsouls.gameplay.BossState
import com.glycin.springsouls.gameplay.Player
import com.glycin.springsouls.gameplay.PlayerState
import com.glycin.springsouls.metrics.GameMetricsService
import com.glycin.springsouls.render.animation.AnimationPlayer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

private const val MODEL_SCALE = 0.03f
private const val AGGRO_RANGE = 13f
private const val SHORT_RANGE = 5f
private const val ATTACK_COOLDOWN = 5.0f
private const val SPELL_SPEED = 10f
private const val SPELL_RADIUS = 1.05f
private const val SPELL_HIT_RADIUS = 0.8f
private const val SPELL_DAMAGE = 50
private const val MELEE_DAMAGE = 60
private const val SPELL_MAX_DISTANCE = 25f
private const val SPELL_HEIGHT = 1.5f
private const val SPELL_CAST_DELAY = 1.1f
private const val MELEE_HIT_DELAY = 0.8f
private const val MELEE_HIT_RADIUS = 10f
private val DEFAULT_MATERIAL = MeshMaterial()

private val ANIMATION_FILES = listOf(
    "models/boss/boss_idle.fbx" to BossState.IDLE,
    "models/boss/boss_short_range_attack.fbx" to BossState.SHORT_RANGE_ATTACK,
    "models/boss/boss_long_range_attack.fbx" to BossState.LONG_RANGE_ATTACK,
    "models/boss/boss_hit.fbx" to BossState.HIT,
    "models/boss/boss_death.fbx" to BossState.DEATH,
)

@Service
class BossRenderService(
    private val levelService: LevelService,
    private val levelRenderService: LevelRenderService,
    private val hudRenderService: HudRenderService,
    private val player: Player,
    private val metricsService: GameMetricsService,
    registry: MeterRegistry,
) {
    init {
        registry.gauge("boss.hp", this) { it.boss?.hp?.toDouble() ?: 0.0 }
    }

    private var meshes: List<Mesh> = emptyList()
    private var materials: List<MeshMaterial> = emptyList()
    private var spellMesh: Mesh? = null

    var boss: Boss? = null
    private val spells = CopyOnWriteArrayList<Spell>()

    private val stateToAnimIndex = mutableMapOf<BossState, Int>()

    private var attackCooldown = 0f
    private var pendingSpell: Float? = null
    private var pendingMelee: Float? = null
    private var meleeHitApplied = false
    private var bossWallsRemoved = false
    private var bossWallTiles = emptyList<Pair<Int, Int>>()
    private var bossEncounterActive = false
    private var bossFightSample: Timer.Sample? = null

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
        val result = ModelLoader.loadAnimatedFromResources("models/boss/boss.fbx")
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

        bossWallTiles = levelService.getBossWallTiles()
        val bossTiles = levelService.getBossTiles()
        if (bossTiles.isNotEmpty()) {
            val (tx, tz) = bossTiles.first()
            boss = Boss(tx + 0.5f, tz + 0.5f).apply {
                animationPlayer = AnimationPlayer(result.skeleton, animations)
                stateToAnimIndex[BossState.IDLE]?.let { animationPlayer!!.playAnimation(it, 0f) }
            }
        }
    }

    fun update(deltaSeconds: Float) {
        val b = boss ?: return
        val ap = b.animationPlayer ?: return

        // Activate boss encounter when boss walls have been removed
        if (!bossEncounterActive && bossWallTiles.isNotEmpty()) {
            // Check if the wall tiles have been cleared (by DoorService)
            val wallsGone = bossWallTiles.all { (bx, bz) -> levelService.getTile(bx, bz) == TILE_EMPTY }
            if (wallsGone) {
                bossEncounterActive = true
                bossFightSample = metricsService.startBossFight("Lord of Beans")
            }
        }

        if (bossEncounterActive) {
            hudRenderService.showBossHp = !b.isDead || !ap.isFinished
        }

        // Handle death animation finishing → remove boss walls
        if (b.isDead) {
            ap.update(deltaSeconds)
            if (ap.isFinished && !bossWallsRemoved) {
                levelRenderService.removeAllBossWalls()
                bossWallsRemoved = true
                bossFightSample?.let { metricsService.endBossFight(it, "Lord of Beans", "victory") }
                bossFightSample = null
            }
            return
        }

        // Handle action animations finishing
        if (b.state.isAction && ap.isFinished) {
            b.state = BossState.IDLE
        }

        // Cooldown
        attackCooldown = (attackCooldown - deltaSeconds).coerceAtLeast(0f)

        // Check distance to player
        val dx = player.x - b.x
        val dz = player.z - b.z
        val distSq = dx * dx + dz * dz

        if (distSq < AGGRO_RANGE * AGGRO_RANGE && player.isAlive) {
            b.faceToward(player.x, player.z)

            if (b.state == BossState.IDLE && attackCooldown <= 0f) {
                if (distSq < SHORT_RANGE * SHORT_RANGE) {
                    b.state = BossState.SHORT_RANGE_ATTACK
                    pendingMelee = MELEE_HIT_DELAY
                    meleeHitApplied = false
                } else {
                    b.state = BossState.LONG_RANGE_ATTACK
                    pendingSpell = SPELL_CAST_DELAY
                }
                attackCooldown = ATTACK_COOLDOWN
            }
        }

        // Pending spell (long range)
        val ps = pendingSpell
        if (ps != null) {
            val remaining = ps - deltaSeconds
            if (remaining <= 0f) {
                pendingSpell = null
                val sdx = player.x - b.x
                val sdz = player.z - b.z
                val dist = sqrt(sdx * sdx + sdz * sdz)
                if (dist > 0f) {
                    spells.add(Spell(
                        x = b.x,
                        y = SPELL_HEIGHT,
                        z = b.z,
                        dirX = sdx / dist,
                        dirZ = sdz / dist,
                        startX = b.x,
                        startZ = b.z,
                    ))
                }
            } else {
                pendingSpell = remaining
            }
        }

        // Pending melee (short range)
        val pm = pendingMelee
        if (pm != null) {
            val remaining = pm - deltaSeconds
            if (remaining <= 0f) {
                pendingMelee = null
                if (!meleeHitApplied) {
                    val mdx = player.x - b.x
                    val mdz = player.z - b.z
                    if (mdx * mdx + mdz * mdz < MELEE_HIT_RADIUS * MELEE_HIT_RADIUS
                        && player.isAlive && !player.state.isDodge) {
                        val dealt = player.takeDamage(MELEE_DAMAGE)
                        metricsService.recordDamageReceived(dealt.toDouble(), "boss_melee")
                        player.state = if (player.isAlive) PlayerState.HIT else PlayerState.DEATH
                        meleeHitApplied = true
                    }
                }
            } else {
                pendingMelee = remaining
            }
        }

        // Switch animation when state changes
        val animIndex = stateToAnimIndex[b.state]
        if (animIndex != null && animIndex != ap.currentAnimationIndex) {
            ap.looping = b.state.isLooping
            ap.playAnimation(animIndex, 0.15f)
        }

        ap.update(deltaSeconds)

        // Update spells
        val spellsToRemove = mutableListOf<Spell>()
        for (spell in spells) {
            spell.x += spell.dirX * SPELL_SPEED * deltaSeconds
            spell.z += spell.dirZ * SPELL_SPEED * deltaSeconds

            val sdx = player.x - spell.x
            val sdz = player.z - spell.z
            if (sdx * sdx + sdz * sdz < SPELL_HIT_RADIUS * SPELL_HIT_RADIUS
                && player.isAlive && !player.state.isDodge) {
                val dealt = player.takeDamage(SPELL_DAMAGE)
                metricsService.recordDamageReceived(dealt.toDouble(), "boss_spell")
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

    fun damageBossAt(worldX: Float, worldZ: Float, radius: Float, damage: Int): Boolean {
        val b = boss ?: return false
        if (b.isDead) return false
        val dx = worldX - b.x
        val dz = worldZ - b.z
        if (dx * dx + dz * dz < radius * radius) {
            val dealt = b.takeDamage(damage)
            metricsService.recordDamageDealt(dealt.toDouble(), "player", "boss")
            if (!b.isAlive) metricsService.recordEnemyKilled("boss")
            transitionBoss(b, if (b.isAlive) BossState.HIT else BossState.DEATH)
            return true
        }
        return false
    }

    private fun transitionBoss(boss: Boss, state: BossState) {
        boss.state = state
        val animIndex = stateToAnimIndex[state] ?: return
        boss.animationPlayer?.let {
            it.looping = state.isLooping
            it.playAnimation(animIndex, 0.1f)
        }
    }

    fun render(shader: ShaderProgram, camera: Camera3D) {
        val b = boss ?: return
        if (meshes.isEmpty()) return
        val ap = b.animationPlayer ?: return

        // Don't render after death animation completes
        if (b.isDead && ap.isFinished) return

        shader.setMatrix4("model", camera.modelMatrix(
            b.x, 0f, b.z, MODEL_SCALE, b.facingYaw - 90f,
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
        shader.setBool("animated", false)

        // Render spells
        val sm = spellMesh ?: return
        if (spells.isEmpty()) return

        shader.setVec3("objectColor", 0.8f, 0.0f, 0.0f)

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
