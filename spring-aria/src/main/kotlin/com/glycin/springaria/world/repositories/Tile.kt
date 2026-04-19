package com.glycin.springaria.world.repositories

import java.awt.Color

enum class Tile(val color: Color?, val solid: Boolean = true, val isLiquid: Boolean = false) {
    AIR(null, solid = false),
    DIRT(Color(139, 90, 43)),
    GRASS(Color(34, 139, 34)),
    STONE(Color(136, 136, 136)),
    SAND(Color(210, 180, 100)),
    WOOD(Color(160, 100, 50)),
    WATER(Color(30, 100, 200, 150), solid = false, isLiquid = true),
    LAVA(Color(220, 60, 20, 180), solid = false, isLiquid = true),
    CLAY(Color(160, 120, 100)),
    GRAVEL(Color(110, 105, 100)),
    DEEPSTONE(Color(80, 80, 90)),
    OBSIDIAN(Color(30, 20, 40)),
    COPPER_ORE(Color(180, 110, 60)),
    IRON_ORE(Color(160, 140, 130)),
    GOLD_ORE(Color(220, 190, 50)),
    CRYSTAL(Color(140, 200, 255)),
}

fun Byte.toTile() = Tile.entries[this.toInt()]