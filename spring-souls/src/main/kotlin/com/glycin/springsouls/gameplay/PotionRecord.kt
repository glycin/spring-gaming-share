package com.glycin.springsouls.gameplay

import org.springframework.data.annotation.Id

data class PotionRecord(
    @Id val id: Long,
    val healPercent: Float,
)
