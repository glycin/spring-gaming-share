package com.glycin.springsouls.security

import com.glycin.annotations.WithLoopingSound
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

enum class DoorResult {
    OPENED,
}

@Service
class DoorService {

    private val logger = LoggerFactory.getLogger(DoorService::class.java)

    @WithLoopingSound("audio/boss_theme.wav", volume = 0.6f)
    @PreAuthorize("hasAuthority('BOSS')")
    fun traverseWhiteFogWall(): DoorResult {
        logger.info("White fog wall dissolved")
        return DoorResult.OPENED
    }

    @PreAuthorize("hasAuthority('KEY_GOLDEN')")
    fun openGoldenDoor(doorX: Int, doorZ: Int): DoorResult {
        logger.info("Golden door opened at ({}, {})", doorX, doorZ)
        return DoorResult.OPENED
    }
}
