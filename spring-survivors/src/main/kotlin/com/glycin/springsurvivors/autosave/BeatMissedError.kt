package com.glycin.springsurvivors.autosave

class BeatMissedError : RuntimeException("Beat missed you n00b!") {
    override fun fillInStackTrace(): Throwable = this
}
