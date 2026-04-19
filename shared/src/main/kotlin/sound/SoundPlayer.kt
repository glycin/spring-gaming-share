package com.glycin.sound

import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

class SoundPlayer(private val soundClip: SoundClip, poolSize: Int = 4) {

    private val clip: Clip? get() = soundClip.clip
    private var currentVolume: Float? = null

    private val clipPool: List<Clip> = (0 until poolSize).mapNotNull { soundClip.createClip() }
    private var poolIndex: Int = 0

    fun setVolume(volume: Float) {
        if (volume == currentVolume) return
        val allClips = listOfNotNull(clip) + clipPool
        for (c in allClips) {
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gain = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val dB = (20.0 * log10(volume.toDouble().coerceIn(0.0001, 1.0))).toFloat()
                gain.value = dB.coerceIn(gain.minimum, gain.maximum)
            }
        }
        currentVolume = volume
    }

    fun play() {
        if (clipPool.isEmpty()) return
        val c = clipPool[poolIndex]
        poolIndex = (poolIndex + 1) % clipPool.size
        c.stop()
        c.framePosition = 0
        c.start()
    }

    fun loop() {
        val c = clip ?: return
        c.stop()
        c.framePosition = 0
        c.loop(Clip.LOOP_CONTINUOUSLY)
    }

    fun isPlaying(): Boolean = clip?.isRunning == true

    fun stop() {
        clip?.stop()
    }
}
