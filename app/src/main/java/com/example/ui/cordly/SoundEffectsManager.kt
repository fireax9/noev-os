package com.example.ui.cordly

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

enum class AppSoundEffect {
    ENTRANCE_CHIME,
    MESSAGE_IN,
    MESSAGE_OUT,
    CALL_CONNECT,
    CALL_END,
    OUTGOING_RINGBACK,
    INCOMING_RINGTONE
}

/**
 * Lightweight synthesized PCM sound effects manager — requires no external audio files
 * and safely handles playback off the UI thread.
 */
object SoundEffectsManager {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ringtoneJob: Job? = null

    fun play(effect: AppSoundEffect) {
        scope.launch {
            when (effect) {
                AppSoundEffect.ENTRANCE_CHIME -> {
                    playToneSequence(
                        notesHz = listOf(440.0, 554.37, 659.25, 880.0),
                        noteMs = 110,
                        volume = 0.22
                    )
                }
                AppSoundEffect.MESSAGE_IN -> {
                    playToneSequence(
                        notesHz = listOf(783.99, 1046.50),
                        noteMs = 75,
                        volume = 0.24
                    )
                }
                AppSoundEffect.MESSAGE_OUT -> {
                    playToneSequence(
                        notesHz = listOf(622.25, 830.61),
                        noteMs = 60,
                        volume = 0.18
                    )
                }
                AppSoundEffect.CALL_CONNECT -> {
                    playToneSequence(
                        notesHz = listOf(523.25, 659.25, 783.99),
                        noteMs = 95,
                        volume = 0.25
                    )
                }
                AppSoundEffect.CALL_END -> {
                    playToneSequence(
                        notesHz = listOf(523.25, 392.00, 293.66),
                        noteMs = 90,
                        volume = 0.24
                    )
                }
                AppSoundEffect.OUTGOING_RINGBACK -> {
                    playToneSequence(notesHz = listOf(440.0), noteMs = 360, volume = 0.14)
                }
                AppSoundEffect.INCOMING_RINGTONE -> {
                    playToneSequence(
                        notesHz = listOf(659.25, 880.0, 659.25, 987.77),
                        noteMs = 140,
                        volume = 0.26
                    )
                }
            }
        }
    }

    fun startRingingLoop(incoming: Boolean) {
        if (ringtoneJob?.isActive == true) return
        ringtoneJob = scope.launch {
            while (isActive) {
                if (incoming) {
                    playToneSequence(listOf(659.25, 880.0, 659.25, 987.77), noteMs = 135, volume = 0.24)
                    delay(1400L)
                } else {
                    playToneSequence(listOf(440.0), noteMs = 420, volume = 0.14)
                    delay(1800L)
                }
            }
        }
    }

    fun stopRingingLoop() {
        ringtoneJob?.cancel()
        ringtoneJob = null
    }

    private fun playToneSequence(notesHz: List<Double>, noteMs: Int, volume: Double) {
        val samplesPerNote = (SAMPLE_RATE * noteMs) / 1000
        val totalSamples = samplesPerNote * notesHz.size
        if (totalSamples <= 0) return

        val pcm = ShortArray(totalSamples)
        var cursor = 0
        for (freq in notesHz) {
            for (i in 0 until samplesPerNote) {
                val progress = i.toDouble() / samplesPerNote.toDouble()
                val attack = (progress * 12.0).coerceAtMost(1.0)
                val decay = exp(-3.5 * progress)
                val env = attack * decay
                val wave = sin(2.0 * Math.PI * freq * i / SAMPLE_RATE)
                pcm[cursor++] = (wave * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        val byteCount = pcm.size * 2
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (_: Exception) {
            null
        } ?: return

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((notesHz.size * noteMs + 120).toLong())
        } catch (_: Exception) {
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
    }
}
