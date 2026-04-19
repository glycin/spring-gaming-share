package com.glycin.sound

import java.io.ByteArrayInputStream
import java.util.logging.Logger
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class SoundClip(resourcePath: String) {

    private val audioBytes: ByteArray? = try {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        if (stream == null) {
            logger.severe("Audio resource not found: $resourcePath")
            null
        } else {
            stream.use { it.readAllBytes() }
        }
    } catch (e: Exception) {
        logger.severe("Failed to load audio '$resourcePath': ${e.message}")
        null
    }

    val clip: Clip? = createClip()

    fun createClip(): Clip? {
        val bytes = audioBytes ?: return null
        return try {
            val audioStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes).buffered())
            AudioSystem.getClip().apply { open(audioStream) }
        } catch (e: Exception) {
            logger.severe("Failed to create clip: ${e.message}")
            null
        }
    }

    companion object {
        private val logger: Logger = Logger.getLogger(SoundClip::class.java.name)
    }
}
