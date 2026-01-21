package app.grip_gains_companion.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Generates audio tones for target weight alerts
 */
object ToneGenerator {
    
    private const val SAMPLE_RATE = 44100
    
    /**
     * Play a warning tone (medium pitch) for general off-target alert
     */
    fun playWarningTone() {
        playTone(frequency = 880.0, durationMs = 150) // A5
    }
    
    /**
     * Play a high tone indicating weight is too heavy
     */
    fun playHighTone() {
        playTone(frequency = 1320.0, durationMs = 150) // E6
    }
    
    /**
     * Play a low tone indicating weight is too light
     */
    fun playLowTone() {
        playTone(frequency = 440.0, durationMs = 150) // A4
    }
    
    /**
     * Generate and play a sine wave tone
     */
    private fun playTone(frequency: Double, durationMs: Int) {
        Thread {
            try {
                val numSamples = (SAMPLE_RATE * durationMs / 1000)
                val samples = ShortArray(numSamples)
                val amplitude = 0.3 * Short.MAX_VALUE
                
                // Envelope attack/release time in samples
                val envelopeSamples = (SAMPLE_RATE * 0.01).toInt() // 10ms
                
                for (i in 0 until numSamples) {
                    val phase = 2.0 * PI * frequency * i / SAMPLE_RATE
                    
                    // Apply envelope to avoid clicks
                    val envelope = when {
                        i < envelopeSamples -> i.toDouble() / envelopeSamples
                        i > numSamples - envelopeSamples -> (numSamples - i).toDouble() / envelopeSamples
                        else -> 1.0
                    }
                    
                    samples[i] = (sin(phase) * envelope * amplitude).toInt().toShort()
                }
                
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                
                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                
                // Wait for playback to complete
                Thread.sleep(durationMs.toLong() + 50)
                
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }.start()
    }
}
