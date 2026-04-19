package com.glycin.springaria.world.repositories

import org.springframework.data.annotation.Id

data class CaveRecord(
    @Id val name: String,
    val worldX: Int,
    val worldY: Int,
    val radius: Int,
    val isUserBean: Boolean,
)
