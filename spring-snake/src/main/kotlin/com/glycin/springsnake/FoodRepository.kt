package com.glycin.springsnake

import com.glycin.util.Vec2
import org.springframework.stereotype.Repository
import kotlin.random.Random

@Repository
class FoodRepository {

    var food: Vec2 = Vec2.zero
    var foodType: FoodType = FoodType.EGG

    fun spawn(occupied: List<Vec2>): Vec2 {
        var pos: Vec2
        do {
            pos = Vec2(
                Random.nextInt(GRID_WIDTH).toFloat(),
                Random.nextInt(GRID_HEIGHT).toFloat(),
            )
        } while (pos in occupied)
        food = pos
        foodType = FoodType.entries.random()
        return food
    }

    companion object {
        const val GRID_WIDTH = 20
        const val GRID_HEIGHT = 15
    }
}
