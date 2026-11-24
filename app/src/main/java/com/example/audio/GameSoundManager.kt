package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight, 100% offline procedural audio & haptics manager.
 * Synthesizes crisp casual-game SFX and gentle ambient pentatonic marimba music using PCM AudioTrack
 * without bloating the APK with external audio binaries.
 */
class GameSoundManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) startMusicLoop() else stopMusicLoop()
        }

    @Volatile
    var vibrationEnabled: Boolean = true

    fun playSlidePop(pitchStep: Int = 0) {
        if (!soundEnabled) return
        val semitoneRatio = Math.pow(2.0, (pitchStep % 8) / 12.0)
        val startFreq = 460.0 * semitoneRatio
        val endFreq = 780.0 * semitoneRatio
        playToneAsync(durationMs = 110, startFreq = startFreq, endFreq = endFreq, volume = 0.28)
    }

    fun playBlockedThud() {
        if (soundEnabled) {
            playToneAsync(durationMs = 140, startFreq = 190.0, endFreq = 110.0, volume = 0.30, triangle = true)
        }
        vibrate(durationMs = 45L, amplitude = 140)
    }

    fun playHintSparkle() {
        if (!soundEnabled) return
        scope.launch {
            playPcmSamples(synthesizeChime(listOf(659.25, 880.0, 1174.66), noteMs = 75, volume = 0.24))
        }
    }

    fun playUndoTick() {
        if (!soundEnabled) return
        playToneAsync(durationMs = 80, startFreq = 420.0, endFreq = 300.0, volume = 0.20)
    }

    fun playButtonClick() {
        if (soundEnabled) {
            playToneAsync(durationMs = 45, startFreq = 580.0, endFreq = 680.0, volume = 0.16)
        }
        vibrate(durationMs = 15L, amplitude = 70)
    }

    fun playWinFanfare() {
        if (soundEnabled) {
            scope.launch {
                playPcmSamples(
                    synthesizeChime(
                        listOf(523.25, 659.25, 783.99, 1046.50),
                        noteMs = 115,
                        volume = 0.30
                    )
                )
            }
        }
        vibrate(durationMs = 80L, amplitude = 180)
    }

    fun playCoinCollect() {
        if (!soundEnabled) return
        scope.launch {
            playPcmSamples(synthesizeChime(listOf(987.77, 1318.51), noteMs = 80, volume = 0.25))
        }
    }

    fun vibrateLight() {
        vibrate(durationMs = 18L, amplitude = 85)
    }

    fun onPause() {
        stopMusicLoop()
    }

    fun onResume() {
        if (musicEnabled) {
            startMusicLoop()
        }
    }

    fun release() {
        stopMusicLoop()
    }

    private fun startMusicLoop() {
        if (musicJob?.isActive == true) return
        musicJob = scope.launch {
            // Calm C-major pentatonic melody loop
            val melody = listOf(261.63, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66, 392.00)
            var index = 0
            while (isActive && musicEnabled) {
                val freq = melody[index % melody.size]
                val samples = synthesizeTone(
                    durationMs = 340,
                    startFreq = freq,
                    endFreq = freq,
                    volume = 0.06,
                    triangle = false
                )
                playPcmSamples(samples)
                index++
                delay(240L)
            }
        }
    }

    private fun stopMusicLoop() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playToneAsync(
        durationMs: Int,
        startFreq: Double,
        endFreq: Double,
        volume: Double,
        triangle: Boolean = false
    ) {
        scope.launch {
            playPcmSamples(synthesizeTone(durationMs, startFreq, endFreq, volume, triangle))
        }
    }

    private fun synthesizeTone(
        durationMs: Int,
        startFreq: Double,
        endFreq: Double,
        volume: Double,
        triangle: Boolean
    ): ShortArray {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / sampleRate
            val envelope = sin(PI * t).coerceAtLeast(0.0) * exp(-2.2 * t)
            val wave = if (triangle) {
                (2.0 / PI) * kotlin.math.asin(sin(phase))
            } else {
                sin(phase) + 0.25 * sin(2.0 * phase)
            }
            out[i] = (wave * envelope * volume * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    private fun synthesizeChime(notes: List<Double>, noteMs: Int, volume: Double): ShortArray {
        val sampleRate = 22050
        val perNote = (sampleRate * noteMs) / 1000
        val out = ShortArray(perNote * notes.size)
        notes.forEachIndexed { idx, freq ->
            val tone = synthesizeTone(noteMs, freq, freq * 1.01, volume, triangle = false)
            tone.copyInto(out, destinationOffset = idx * perNote)
        }
        return out
    }

    private fun playPcmSamples(samples: ShortArray) {
        if (samples.isEmpty()) return
        try {
            val sampleRate = 22050
            val byteSize = samples.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(byteSize, minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L) / sampleRate + 35L
            Thread.sleep(durationMs)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Gracefully ignore in JVM/Robolectric test environments without audio hardware
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (!vibrationEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                vibrator.vibrate(durationMs)
            }
        } catch (_: Throwable) {
            // Ignore safely if vibrator is unavailable
        }
    }
}
