package com.glycin.springsurvivors.upgrades

import com.glycin.springsurvivors.GameState
import org.springframework.data.annotation.Id

class UpgradeRecord(
    @Id val id: Long,
    val name: String,
    val description: String,
    val repeatable: Boolean = false,
    val tier: Int = 0,
    val upgradeGroup: String? = null,
    val effect: (GameState) -> Unit,
) {
    val grantedAuthority: String?
        get() = upgradeGroup?.let { "UPGRADE_${it}_T$tier" }

    val requiredAuthority: String?
        get() = if (tier > 1) upgradeGroup?.let { "UPGRADE_${it}_T${tier - 1}" } else null
}
