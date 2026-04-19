package com.glycin.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithLoopingSound(val value: String, val stopCondition: String = "", val volume: Float = 1.0f)
