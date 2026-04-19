package com.glycin.springsurvivors.autosave

import com.glycin.util.GridPos
import org.springframework.stereotype.Component

@Component
class AutoSaveGameState {
    var playerGridPos = GridPos(0, 0)
    var snapshotRequested = false
}
