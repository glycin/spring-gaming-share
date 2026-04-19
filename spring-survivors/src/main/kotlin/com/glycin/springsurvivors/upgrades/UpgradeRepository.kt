package com.glycin.springsurvivors.upgrades

import org.springframework.data.repository.Repository

interface UpgradeRepository : Repository<UpgradeRecord, Long>, UpgradeRepositoryCustom

interface UpgradeRepositoryCustom {
    fun findAll(): List<UpgradeRecord>
    fun findById(id: Long): UpgradeRecord?
    fun findByName(name: String): UpgradeRecord?
    fun count(): Int
    fun save(record: UpgradeRecord): UpgradeRecord
    fun deleteById(id: Long): UpgradeRecord?
    fun deleteAll()
}

class UpgradeRepositoryCustomImpl : UpgradeRepositoryCustom {

    private val upgrades = mutableListOf<UpgradeRecord>()
    private var nextId = 1L

    override fun findAll(): List<UpgradeRecord> = upgrades.toList()

    override fun findById(id: Long): UpgradeRecord? = upgrades.firstOrNull { it.id == id }

    override fun findByName(name: String): UpgradeRecord? = upgrades.firstOrNull { it.name == name }

    override fun count(): Int = upgrades.size

    override fun save(record: UpgradeRecord): UpgradeRecord {
        val toSave = if (record.id == 0L) {
            UpgradeRecord(
                id = nextId++,
                name = record.name,
                description = record.description,
                repeatable = record.repeatable,
                tier = record.tier,
                upgradeGroup = record.upgradeGroup,
                effect = record.effect,
            )
        } else {
            record
        }
        upgrades.add(toSave)
        return toSave
    }

    override fun deleteById(id: Long): UpgradeRecord? {
        val index = upgrades.indexOfFirst { it.id == id }
        return if (index >= 0) upgrades.removeAt(index) else null
    }

    override fun deleteAll() {
        upgrades.clear()
    }
}
