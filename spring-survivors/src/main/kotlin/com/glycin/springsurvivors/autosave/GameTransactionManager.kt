package com.glycin.springsurvivors.autosave

import com.glycin.springsurvivors.player.Player
import com.glycin.annotations.GameManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus

// TODO: add attacks to save game state
@GameManager
class GameTransactionManager(
    private val player: Player,
    private val autoSaveGameState: AutoSaveGameState,
) : AbstractPlatformTransactionManager() {

    private val transactionObject = Any()

    override fun doGetTransaction(): Any = transactionObject

    override fun doBegin(transaction: Any, definition: TransactionDefinition) {}

    override fun doCommit(status: DefaultTransactionStatus) {
        if (autoSaveGameState.snapshotRequested) {
            autoSaveGameState.snapshotRequested = false
            autoSaveGameState.playerGridPos = player.gridPos
        }
    }

    override fun doRollback(status: DefaultTransactionStatus) {
        player.moveTo(autoSaveGameState.playerGridPos)
    }
}
