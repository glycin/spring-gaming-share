package com.glycin.image

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class SpriteSheet(
    resourcePath: String
) {

    val sheet: BufferedImage = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        ?.let { ImageIO.read(it) }
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    val width: Int get() = sheet.width
    val height: Int get() = sheet.height

    fun getSprite(x: Int, y: Int, width: Int, height: Int): BufferedImage =
        sheet.getSubimage(x, y, width, height)

    fun getGrid(spriteWidth: Int, spriteHeight: Int): Array<Array<BufferedImage>> {
        val cols = width / spriteWidth
        val rows = height / spriteHeight

        return Array(rows) { row ->
            Array(cols) { col ->
                sheet.getSubimage(
                    col * spriteWidth,
                    row * spriteHeight,
                    spriteWidth,
                    spriteHeight,
                )
            }
        }
    }

    fun getRow(spriteWidth: Int, spriteHeight: Int, row: Int = 0): Array<BufferedImage> =
        getGrid(spriteWidth, spriteHeight)[row]
}
