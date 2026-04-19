package com.glycin.util

data class GridPos(val col: Int, val row: Int) {
    operator fun plus(other: GridPos) = GridPos(col + other.col, row + other.row)

    companion object {
        val UP = GridPos(0, -1)
        val DOWN = GridPos(0, 1)
        val LEFT = GridPos(-1, 0)
        val RIGHT = GridPos(1, 0)
    }
}
