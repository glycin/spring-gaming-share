package com.glycin.image

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class SpringGameImage(resourcePath: String) {

    val image: BufferedImage = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        ?.let { ImageIO.read(it) }
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    val width: Int get() = image.width
    val height: Int get() = image.height
}
