package com.glycin.springsurvivors.xp

import com.glycin.springsurvivors.enemies.EnemyType
import com.glycin.util.GridPos

data class EnemyDiedEvent(val gridPos: GridPos, val xpValue: Int, val enemyType: EnemyType)
