package com.glycin.springsnake

import com.glycin.image.SpriteSheet
import com.glycin.util.Vec2
import com.glycin.extensions.flipHorizontal
import com.glycin.extensions.rotate
import java.awt.image.BufferedImage

class SnakeSprites {

    private val sheet = SpriteSheet("sheets/snake_spritesheet.png")

    private val headLeft = sheet.getSprite(40, 0, 9, 6)
    private val headOpenLeft = sheet.getSprite(38, 11, 11, 8)
    private val bodyHorizontal = sheet.getSprite(49, 0, 6, 6)
    private val turnUpToLeft = sheet.getSprite(55, 0, 6, 6)
    private val tailLeft = sheet.getSprite(55, 6, 6, 6)

    val headUp = headLeft.rotate(90.0)
    val headRight = headLeft.flipHorizontal()
    val headDown = headLeft.rotate(270.0)

    val headOpenUp = headOpenLeft.rotate(90.0)
    val headOpenRight = headOpenLeft.flipHorizontal()
    val headOpenDown = headOpenLeft.rotate(270.0)

    val bodyVertical = bodyHorizontal.rotate(90.0)

    val turnRightDown = turnUpToLeft.rotate(270.0)
    val turnRightUp = turnUpToLeft.rotate(180.0)
    val turnLeftUp = turnUpToLeft.rotate(90.0)

    val tailUp = tailLeft.rotate(90.0)
    val tailRight = tailLeft.flipHorizontal()
    val tailDown = tailLeft.rotate(270.0)

    private val foods = mapOf(
        FoodType.ROCK to sheet.getSprite(89, 39, 6, 6),
        FoodType.SHROOM to sheet.getSprite(88, 21, 8, 8),
        FoodType.STAR to sheet.getSprite(88, 12, 8, 8),
        FoodType.EGG to sheet.getSprite(88, 30, 8, 8),
    )

    val scorePanelLeft = sheet.getSprite(0, 72, 6, 20)
    val scorePanelMiddle = sheet.getSprite(6, 72, 48, 20)
    val scorePanelRight = sheet.getSprite(54, 72, 7, 20)

    private val digits = Array(10) { i -> sheet.getSprite(0 + i * 7, 31, 7, 9) }

    fun getDigitSprite(digit: Int): BufferedImage = digits[digit.coerceIn(0, 9)]

    fun getFoodSprite(type: FoodType): BufferedImage = foods[type] ?: foods[FoodType.EGG]!!

    fun getHeadSprite(direction: Vec2, mouthOpen: Boolean = false): BufferedImage = if (mouthOpen) {
        when (direction) {
            Vec2.up -> headOpenUp
            Vec2.down -> headOpenDown
            Vec2.left -> headOpenLeft
            Vec2.right -> headOpenRight
            else -> headOpenRight
        }
    } else {
        when (direction) {
            Vec2.up -> headUp
            Vec2.down -> headDown
            Vec2.left -> headLeft
            Vec2.right -> headRight
            else -> headRight
        }
    }

    fun getTailSprite(directionToBody: Vec2): BufferedImage = when (directionToBody) {
        Vec2.up -> tailUp
        Vec2.down -> tailDown
        Vec2.left -> tailLeft
        Vec2.right -> tailRight
        else -> tailRight
    }

    fun getBodySprite(fromDir: Vec2, toDir: Vec2): BufferedImage {
        if (fromDir.x != 0f && toDir.x != 0f) return bodyHorizontal
        if (fromDir.y != 0f && toDir.y != 0f) return bodyVertical

        return when {
            (fromDir == Vec2.left && toDir == Vec2.down) || (fromDir == Vec2.up && toDir == Vec2.right) -> turnRightDown
            (fromDir == Vec2.right && toDir == Vec2.down) || (fromDir == Vec2.up && toDir == Vec2.left) -> turnUpToLeft
            (fromDir == Vec2.left && toDir == Vec2.up) || (fromDir == Vec2.down && toDir == Vec2.right) -> turnRightUp
            (fromDir == Vec2.right && toDir == Vec2.up) || (fromDir == Vec2.down && toDir == Vec2.left) -> turnLeftUp
            else -> bodyHorizontal
        }
    }
}
