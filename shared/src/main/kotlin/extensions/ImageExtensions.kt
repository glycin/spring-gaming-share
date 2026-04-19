package com.glycin.extensions

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun BufferedImage.rotate(degrees: Double): BufferedImage {
    val radians = Math.toRadians(degrees)
    val sin = abs(sin(radians))
    val cos = abs(cos(radians))
    val newW = (width * cos + height * sin).toInt()
    val newH = (width * sin + height * cos).toInt()

    val transform = AffineTransform().apply {
        translate(newW / 2.0, newH / 2.0)
        rotate(radians)
        translate(-width / 2.0, -height / 2.0)
    }

    val op = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
    val dest = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
    op.filter(this, dest)
    return dest
}

fun BufferedImage.flipHorizontal(): BufferedImage {
    val transform = AffineTransform.getScaleInstance(-1.0, 1.0).apply {
        translate(-width.toDouble(), 0.0)
    }
    val op = AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
    val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    op.filter(this, dest)
    return dest
}