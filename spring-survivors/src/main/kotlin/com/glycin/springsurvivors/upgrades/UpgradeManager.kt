package com.glycin.springsurvivors.upgrades

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.image.SpringGameImage
import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.attacks.AirstrikeAttack
import com.glycin.springsurvivors.attacks.Attack
import com.glycin.springsurvivors.attacks.AttackManager
import com.glycin.springsurvivors.attacks.BaseAttack
import com.glycin.springsurvivors.attacks.DirectionMode
import com.glycin.springsurvivors.attacks.ExplosionAttack
import com.glycin.springsurvivors.attacks.LaserAttack
import com.glycin.springsurvivors.attacks.OrbitalAttack
import com.glycin.springsurvivors.effects.LightningEffect
import com.glycin.springsurvivors.effects.LightningFinishedEvent
import com.glycin.springsurvivors.enemies.EnemyManager
import com.glycin.springsurvivors.metrics.GameMetricsService
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.security.PlayerAuthentication
import com.glycin.springsurvivors.security.UpgradeGateService
import com.glycin.util.GridPos
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.security.access.AccessDeniedException
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.sin

private val NAME_FONT = Font("Monospaced", Font.BOLD, 14)
private val DESC_FONT = Font("Monospaced", Font.PLAIN, 11)
private const val BOB_AMPLITUDE = 4f
private const val BOB_SPEED = 0.05f

private val nameFontMetrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    .createGraphics().let { g -> g.font = NAME_FONT; val fm = g.fontMetrics; g.dispose(); fm }
private val descFontMetrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    .createGraphics().let { g -> g.font = DESC_FONT; val fm = g.fontMetrics; g.dispose(); fm }

@GameManager
class UpgradeManager(
    private val upgradeRepository: UpgradeRepository,
    private val gameState: GameState,
    private val player: Player,
    private val gameGrid: GameGrid,
    private val lightningEffect: LightningEffect,
    private val attackManager: AttackManager,
    private val enemyManager: EnemyManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val playerAuthentication: PlayerAuthentication,
    private val upgradeGateService: UpgradeGateService,
    private val metricsService: GameMetricsService,
) {

    private val log = LoggerFactory.getLogger(UpgradeManager::class.java)

    private val leafSprite: BufferedImage = scaleSprite("sprites/leaf.png")
    private val leafRedSprite: BufferedImage = scaleSprite("sprites/leaf_red.png")

    private fun scaleSprite(path: String): BufferedImage {
        val img = SpringGameImage(path).image
        val size = gameGrid.tileSize
        return BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).also { scaled ->
            val g = scaled.createGraphics()
            g.drawImage(img, 0, 0, size, size, null)
            g.dispose()
        }
    }

    init {
        // Repeatable upgrades (no tiers)
        upgradeRepository.save(UpgradeRecord(0L, "Kanban", "Projectiles move 10% faster", repeatable = true) { it.projectileSpeedMultiplier *= 1.1f })
        upgradeRepository.save(UpgradeRecord(0L, "Load Balancer", "Max HP +1", repeatable = true) { it.maxHp += 1f; player.hp += 1f })
        upgradeRepository.save(UpgradeRecord(0L, "Conference Visit", "Gain more XP from diamonds", repeatable = true) { it.xpGainMultiplier *= 1.2f })
        upgradeRepository.save(UpgradeRecord(0L, "New KPIs", "Enemies spawn faster", repeatable = true) { it.spawnIntervalMultiplier *= 0.7f })
        upgradeRepository.save(UpgradeRecord(0L, "Java Upgrade", "All damage +5%", repeatable = true) { it.damageMultiplier *= 1.05f })
        upgradeRepository.save(UpgradeRecord(0L, "Daily Standup", "Enemies skip +1 beat", repeatable = true) { it.extraBeatSkip++ })
        upgradeRepository.save(UpgradeRecord(0L, "Vacation", "Restore all HP", repeatable = true) { player.hp = it.maxHp })

        // === Base attack tiers (tier 1 is pre-unlocked) ===
        playerAuthentication.grant("UPGRADE_BASE_T1")

        upgradeRepository.save(UpgradeRecord(0L, "Postgres II", "Fire every 2 beats",
            tier = 2, upgradeGroup = "BASE",
        ) { attackManager.requireAttack<BaseAttack>().beatsPerFire = 2 })

        upgradeRepository.save(UpgradeRecord(0L, "Postgres III", "Fire in all 4 directions",
            tier = 3, upgradeGroup = "BASE",
        ) { attackManager.requireAttack<BaseAttack>().directionMode = DirectionMode.FOUR })

        upgradeRepository.save(UpgradeRecord(0L, "Postgres IV", "Fire every beat",
            tier = 4, upgradeGroup = "BASE",
        ) { attackManager.requireAttack<BaseAttack>().beatsPerFire = 1 })

        upgradeRepository.save(UpgradeRecord(0L, "Postgres V", "Fire in diagonal directions",
            tier = 5, upgradeGroup = "BASE",
        ) { attackManager.requireAttack<BaseAttack>().directionMode = DirectionMode.EIGHT })

        upgradeRepository.save(UpgradeRecord(0L, "Postgres VI", "Projectiles split into 3 on hit",
            tier = 6, upgradeGroup = "BASE",
        ) { attackManager.requireAttack<BaseAttack>().splitOnHit = true })

        // === Laser tiers ===
        upgradeRepository.save(UpgradeRecord(0L, "Security Panel", "Piercing laser beams every 5 beats",
            tier = 1, upgradeGroup = "LASER",
        ) { it.piercingShots = true; attackManager.unlock(LaserAttack(gameGrid)) })

        upgradeRepository.save(UpgradeRecord(0L, "Security Panel II", "Lasers fire every 4 beats",
            tier = 2, upgradeGroup = "LASER",
        ) { attackManager.requireAttack<LaserAttack>().beatsPerFire = 4 })

        upgradeRepository.save(UpgradeRecord(0L, "Security Panel III", "Diagonal lasers, damage +50%",
            tier = 3, upgradeGroup = "LASER",
        ) { attackManager.requireAttack<LaserAttack>().let { laser -> laser.diagonalLasers = true; laser.laserDamageMultiplier = 1.5f } })

        upgradeRepository.save(UpgradeRecord(0L, "Security Panel IV", "Lasers fire every 3 beats",
            tier = 4, upgradeGroup = "LASER",
        ) { attackManager.requireAttack<LaserAttack>().beatsPerFire = 3 })

        upgradeRepository.save(UpgradeRecord(0L, "Security Panel V", "Double width, damage +100%",
            tier = 5, upgradeGroup = "LASER",
        ) { attackManager.requireAttack<LaserAttack>().let { laser -> laser.laserWidth = 8f; laser.laserDamageMultiplier = 2.0f } })

        // === Orbital tiers ===
        upgradeRepository.save(UpgradeRecord(0L, "DevOps Team", "Orbitals damage nearby enemies",
            tier = 1, upgradeGroup = "ORBITAL",
        ) { attackManager.unlock(OrbitalAttack(gameGrid, player)) })

        upgradeRepository.save(UpgradeRecord(0L, "DevOps Team II", "+1 orbital",
            tier = 2, upgradeGroup = "ORBITAL",
        ) { attackManager.requireAttack<OrbitalAttack>().orbitalCount = 4 })

        upgradeRepository.save(UpgradeRecord(0L, "DevOps Team III", "Orbital damage +100%",
            tier = 3, upgradeGroup = "ORBITAL",
        ) { attackManager.requireAttack<OrbitalAttack>().orbitalDamage = 1.0f })

        upgradeRepository.save(UpgradeRecord(0L, "DevOps Team IV", "Larger orbit radius",
            tier = 4, upgradeGroup = "ORBITAL",
        ) { attackManager.requireAttack<OrbitalAttack>().orbitalRadiusTiles = 3.0f })

        upgradeRepository.save(UpgradeRecord(0L, "DevOps Team V", "+2 orbitals, faster rotation",
            tier = 5, upgradeGroup = "ORBITAL",
        ) { attackManager.requireAttack<OrbitalAttack>().let { orb -> orb.orbitalCount = 6; orb.rotationSpeed = 0.07f } })

        // === Explosion tiers ===
        upgradeRepository.save(UpgradeRecord(0L, "AI assisted development", "Enemies explode on death",
            tier = 1, upgradeGroup = "EXPLOSION",
        ) { attackManager.unlock(ExplosionAttack(gameState, gameGrid, enemyManager, eventPublisher)) })

        upgradeRepository.save(UpgradeRecord(0L, "AI assisted development II", "Explosion damage +100%",
            tier = 2, upgradeGroup = "EXPLOSION",
        ) { attackManager.requireAttack<ExplosionAttack>().explosionDamage = 1.0f })

        upgradeRepository.save(UpgradeRecord(0L, "AI assisted development III", "Larger blast radius",
            tier = 3, upgradeGroup = "EXPLOSION",
        ) { attackManager.requireAttack<ExplosionAttack>().maxRingRadiusTiles = 2.5f })

        upgradeRepository.save(UpgradeRecord(0L, "AI assisted development IV", "Explosion damage +200%",
            tier = 4, upgradeGroup = "EXPLOSION",
        ) { attackManager.requireAttack<ExplosionAttack>().explosionDamage = 1.5f })

        upgradeRepository.save(UpgradeRecord(0L, "AI assisted development V", "Massive blast radius",
            tier = 5, upgradeGroup = "EXPLOSION",
        ) { attackManager.requireAttack<ExplosionAttack>().let { exp -> exp.explosionDamage = 2.0f; exp.maxRingRadiusTiles = 3.5f } })

        // === Airstrike tiers ===
        upgradeRepository.save(UpgradeRecord(0L, "Rebuild from scratch", "Lightning strikes random enemies",
            tier = 1, upgradeGroup = "AIRSTRIKE",
        ) { attackManager.unlock(AirstrikeAttack(gameState, gameGrid, enemyManager, lightningEffect, eventPublisher)) })

        upgradeRepository.save(UpgradeRecord(0L, "Rebuild from scratch II", "Strike every 6 beats",
            tier = 2, upgradeGroup = "AIRSTRIKE",
        ) { attackManager.requireAttack<AirstrikeAttack>().beatsBetweenStrikes = 6 })

        upgradeRepository.save(UpgradeRecord(0L, "Rebuild from scratch III", "+2 strikes per wave",
            tier = 3, upgradeGroup = "AIRSTRIKE",
        ) { attackManager.requireAttack<AirstrikeAttack>().strikesPerWave = 5 })

        upgradeRepository.save(UpgradeRecord(0L, "Rebuild from scratch IV", "Strike damage +100%",
            tier = 4, upgradeGroup = "AIRSTRIKE",
        ) { attackManager.requireAttack<AirstrikeAttack>().strikeDamage = 6f })

        upgradeRepository.save(UpgradeRecord(0L, "Rebuild from scratch V", "Strike every 4 beats, +3 strikes",
            tier = 5, upgradeGroup = "AIRSTRIKE",
        ) { attackManager.requireAttack<AirstrikeAttack>().let { air -> air.beatsBetweenStrikes = 4; air.strikesPerWave = 8 } })
    }

    private val pendingUpgrades = mutableMapOf<GridPos, UpgradePickup>()
    private val pickups = mutableMapOf<GridPos, UpgradePickup>()
    private var bobTimer = 0f

    @EventListener
    fun onLevelUp(event: LevelUpEvent) {
        gameState.frozen = true

        val pos = player.gridPos
        val slots = listOf(
            GridPos(pos.col + 2, pos.row) to LabelAnchor.RIGHT,
            GridPos(pos.col, pos.row - 2) to LabelAnchor.ABOVE,
            GridPos(pos.col - 2, pos.row) to LabelAnchor.LEFT,
        ).filter { gameGrid.isInBounds(it.first) }

        val allUpgrades = upgradeRepository.findAll()
        val repeatablePool = allUpgrades.filter { it.repeatable }
        val tieredPool = allUpgrades.filter { !it.repeatable }
            .filter { upgrade ->
                val required = upgrade.requiredAuthority ?: return@filter true
                try {
                    upgradeGateService.requireAuthority(required)
                    true
                } catch (_: AccessDeniedException) {
                    false
                }
            }
            .groupBy { it.upgradeGroup }
            .mapNotNull { (_, tiers) -> tiers.minByOrNull { it.tier } }

        val normal = tieredPool.shuffled().take(1)
        val repeatable = repeatablePool.shuffled().take(1)
        val wildcardPool = (tieredPool + repeatablePool).filter { it !in normal && it !in repeatable }
        val wildcard = wildcardPool.shuffled().take(1)
        val choices = (normal + repeatable + wildcard).shuffled()

        if (choices.isEmpty()) {
            gameState.frozen = false
            return
        }

        for ((slot, choice) in slots.zip(choices)) {
            val (gridPos, anchor) = slot
            pendingUpgrades[gridPos] = UpgradePickup(choice, anchor)
            lightningEffect.strike(gridPos)
        }
    }

    @EventListener
    fun onLightningFinished(event: LightningFinishedEvent) {
        val pickup = pendingUpgrades.remove(event.gridPos) ?: return
        pickups[event.gridPos] = pickup
    }

    @Update(order = 5)
    fun update() {
        if (pickups.isEmpty()) return
        bobTimer += BOB_SPEED

        val pickup = pickups.remove(player.gridPos) ?: return
        val upgrade = pickup.record
        upgrade.effect(gameState)

        upgrade.grantedAuthority?.let { playerAuthentication.grant(it) }

        if (!upgrade.repeatable) {
            upgradeRepository.deleteById(upgrade.id)
        }
        pickups.clear()
        pendingUpgrades.clear()
        gameState.frozen = false
        metricsService.recordUpgradeChosen(upgrade.name)
        log.info("Picked up upgrade '{}' (id={}, tier={})", upgrade.name, upgrade.id, upgrade.tier)
    }

    @Renderer
    fun render(g: Graphics2D) {
        if (pickups.isEmpty()) return

        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        for ((i, entry) in pickups.entries.withIndex()) {
            val (gridPos, pickup) = entry
            val bob = (sin((bobTimer + i * 1.5f).toDouble()) * BOB_AMPLITUDE).toInt()
            val px = gameGrid.toPixelX(gridPos.col)
            val py = gameGrid.toPixelY(gridPos.row) - halfTile + bob
            val sprite = if (pickup.record.tier > 0) leafRedSprite else leafSprite
            g.drawImage(sprite, px, py, null)

            val centerX = px + halfTile
            val centerY = py + halfTile

            val (nameX, nameY, descX, descY) = when (pickup.anchor) {
                LabelAnchor.RIGHT -> {
                    val x = px + tileSize + 6
                    val y = centerY
                    LabelPos(x, y - 2, x, y + 14)
                }
                LabelAnchor.ABOVE -> {
                    val y = py - 6
                    LabelPos(centerX - pickup.nameWidth / 2, y - 12, centerX - pickup.descWidth / 2, y)
                }
                LabelAnchor.LEFT -> {
                    val y = centerY
                    LabelPos(px - pickup.nameWidth - 6, y - 2, px - pickup.descWidth - 6, y + 14)
                }
            }

            g.font = NAME_FONT
            g.color = Color.WHITE
            g.drawString(pickup.record.name, nameX, nameY)

            g.font = DESC_FONT
            g.color = Color.LIGHT_GRAY
            g.drawString(pickup.record.description, descX, descY)
        }
    }
}

private enum class LabelAnchor { RIGHT, ABOVE, LEFT }

private class UpgradePickup(val record: UpgradeRecord, val anchor: LabelAnchor) {
    val nameWidth: Int = nameFontMetrics.stringWidth(record.name)
    val descWidth: Int = descFontMetrics.stringWidth(record.description)
}

private data class LabelPos(val nameX: Int, val nameY: Int, val descX: Int, val descY: Int)

private inline fun <reified T : Attack> AttackManager.requireAttack(): T =
    getAttack(T::class.java) ?: error("Expected ${T::class.simpleName} to be unlocked")
