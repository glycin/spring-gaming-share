package com.glycin.springsouls.gameplay

import org.springframework.data.repository.Repository

interface PotionRepository : Repository<PotionRecord, Long>, PotionRepositoryCustom

interface PotionRepositoryCustom {
    fun findAll(): List<PotionRecord>
    fun findById(id: Long): PotionRecord?
    fun count(): Int
    fun save(potion: PotionRecord)
    fun deleteById(id: Long): PotionRecord?
    fun deleteAll()
    fun drink(player: Player): Boolean
    fun pickup(healPercent: Float = 0.5f): PotionRecord
}

class PotionRepositoryCustomImpl(
    private val player: Player,
) : PotionRepositoryCustom {

    private val potions = mutableListOf<PotionRecord>()
    private var nextId = 1L

    init {
        repeat(player.potionCharges) {
            potions.add(PotionRecord(nextId++, 0.5f))
        }
    }

    override fun findAll(): List<PotionRecord> = potions.toList()

    override fun findById(id: Long): PotionRecord? = potions.firstOrNull { it.id == id }

    override fun count(): Int = potions.size

    override fun save(potion: PotionRecord) {
        potions.add(potion)
        player.potionCharges = potions.size
        player.maxPotionCharges = player.maxPotionCharges.coerceAtLeast(potions.size)
    }

    override fun deleteById(id: Long): PotionRecord? {
        val index = potions.indexOfFirst { it.id == id }
        if (index < 0) return null
        val potion = potions.removeAt(index)
        player.potionCharges = potions.size
        return potion
    }

    override fun deleteAll() {
        potions.clear()
        player.potionCharges = 0
    }

    override fun drink(player: Player): Boolean {
        val potion = potions.firstOrNull() ?: return false
        potions.removeFirst()
        player.potionCharges = potions.size
        player.heal((player.maxHp * potion.healPercent).toInt())
        return true
    }

    override fun pickup(healPercent: Float): PotionRecord {
        val potion = PotionRecord(nextId++, healPercent)
        save(potion)
        return potion
    }
}
