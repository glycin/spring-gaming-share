package com.glycin.extensions

fun Float.lerp(other: Float, t: Float): Float = this + (other - this) * t

fun Double.lerp(other: Double, t: Double): Double = this + (other - this) * t
