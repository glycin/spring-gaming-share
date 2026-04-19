package com.glycin.springaria.world.repositories

import com.glycin.springaria.world.WorldConstants
import org.springframework.data.annotation.Id

data class TileRecord(
    @Id val id: Long,
    val x: Int,
    val y: Int,
    val type: Tile,
) {
    companion object {
        fun packId(x: Int, y: Int): Long = WorldConstants.packCoords(x, y)
    }
}
