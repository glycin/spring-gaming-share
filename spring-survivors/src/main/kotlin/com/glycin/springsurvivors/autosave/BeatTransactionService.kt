package com.glycin.springsurvivors.autosave

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BeatTransactionService {

    @Transactional
    fun <T> executeOnBeat(action: () -> T): T = action()
}
