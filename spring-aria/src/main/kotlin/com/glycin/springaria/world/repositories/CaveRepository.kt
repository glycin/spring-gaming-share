package com.glycin.springaria.world.repositories

import org.springframework.data.repository.Repository
import kotlin.math.sqrt

interface CaveRepository : Repository<CaveRecord, String>, CaveRepositoryCustom

interface CaveRepositoryCustom {
    fun save(cave: CaveRecord)
    fun findAll(): List<CaveRecord>
    fun findByPlayerInRange(tileX: Int, tileY: Int): CaveRecord?
}

class CaveRepositoryCustomImpl : CaveRepositoryCustom {

    private val caves = mutableListOf<CaveRecord>()

    override fun save(cave: CaveRecord) {
        caves.add(cave)
    }

    override fun findAll(): List<CaveRecord> = caves.toList()

    override fun findByPlayerInRange(tileX: Int, tileY: Int): CaveRecord? {
        return caves.firstOrNull { cave ->
            val dx = tileX - cave.worldX
            val dy = tileY - cave.worldY
            sqrt((dx * dx + dy * dy).toDouble()) < cave.radius
        }
    }
}
