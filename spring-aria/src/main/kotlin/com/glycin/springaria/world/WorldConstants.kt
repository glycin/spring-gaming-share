package com.glycin.springaria.world

object WorldConstants {
    const val TILE_SIZE = 6
    const val CHUNK_SIZE = 32
    const val CHUNK_TILE_COUNT = CHUNK_SIZE * CHUNK_SIZE
    const val CHUNK_PIXEL_WIDTH = CHUNK_SIZE * TILE_SIZE
    const val CHUNK_PIXEL_HEIGHT = CHUNK_SIZE * TILE_SIZE

    const val WORLD_WIDTH_TILES = 4_096
    const val WORLD_HEIGHT_TILES = 4_096

    const val WORLD_WIDTH_PIXELS = WORLD_WIDTH_TILES * TILE_SIZE
    const val WORLD_HEIGHT_PIXELS = WORLD_HEIGHT_TILES * TILE_SIZE

    fun packCoords(a: Int, b: Int): Long = (a.toLong() shl 32) or (b.toLong() and 0xFFFFFFFFL)
    fun unpackX(packed: Long): Int = (packed shr 32).toInt()
    fun unpackY(packed: Long): Int = packed.toInt()
}