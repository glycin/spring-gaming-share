package com.glycin.springsurvivors.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class UpgradeGateService {

    @PreAuthorize("hasAuthority(#authority)")
    fun requireAuthority(authority: String): Boolean = true
}
