package com.example.ui.cordly

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.tanh

enum class VoiceEffectType(
    val id: String,
    val titleEn: String,
    val titleAr: String
) {
    NORMAL("normal", "Natural", "طبيعي"),
    DEEP("deep", "Deep Boss", "صوت عميق"),
    HIGH_PITCH("high_pitch", "Helium", "صوت رفيع"),
    ROBOT("robot", "Cyber Robot", "روبوت"),
    ECHO("echo", "Stadium Echo", "صدى");

    companion object {
        fun fromId(id: String?): VoiceEffectType {
            return entries.firstOrNull { it.id == id } ?: NORMAL
        }
    }
}

data class AudioRoutingSnapshot(
    val prevMode: Int,
    val prevSpeakerOn: Boolean,
    val focusRequest: AudioFocusRequest?
)

/**
 * Real-time two-way call audio capture, µ-law compression, PCM voice gain boost,
 * voice changer DSP, non-blocking background AudioTrack playback, and WAV call recording.
 */
class CallAudioEngine(private val context: Context) {

    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val lastPlayedSeqBySender = ConcurrentHashMap<String, Long>()
    private val playbackQueue = Channel<ShortArray>(capacity = 24)
    private val playbackLock = Any()

    @Volatile
    var isRecordingToFile: Boolean = false
        private set

    private var recordedStream: ByteArrayOutputStream? = null
    private var recordingFilePath: String? = null

    fun startPlaybackTrack(scope: CoroutineScope? = null) {
        synchronized(playbackLock) {
            if (audioTrack == null) {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(SAMPLE_RATE / 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 3)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                try {
                    track.setVolume(AudioTrack.getMaxVolume())
                    track.play()
                    audioTrack = track
                } catch (_: Exception) {
                    track.release()
                }
            }
        }
        if (scope != null && (playbackJob == null || playbackJob?.isActive != true)) {
            playbackJob = scope.launch(Dispatchers.IO) {
                for (pcm in playbackQueue) {
                    if (!isActive) break
                    synchronized(playbackLock) {
                        val track = audioTrack
                        if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.write(pcm, 0, pcm.size)
                        }
                    }
                }
            }
        }
    }

    fun enqueueRemoteAudioPacket(senderId: String, audioDataBase64: String?, audioSeq: Long) {
        if (audioDataBase64.isNullOrBlank() || audioSeq <= 0L) return
        val prevSeq = lastPlayedSeqBySender[senderId] ?: 0L
        if (audioSeq <= prevSeq) return
        lastPlayedSeqBySender[senderId] = audioSeq

        val ulawBytes = try {
            Base64.decode(audioDataBase64, Base64.DEFAULT)
        } catch (_: Exception) {
            return
        }
        if (ulawBytes.isEmpty()) return
        val pcm = boostPcmGain(decodeMuLaw(ulawBytes), gain = 1.45f)
        appendToRecording(pcm)
        playbackQueue.trySend(pcm)
    }

    @Deprecated("Use enqueueRemoteAudioPacket with background playback loop")
    fun playRemoteAudioPacket(senderId: String, audioDataBase64: String?, audioSeq: Long) {
        enqueueRemoteAudioPacket(senderId, audioDataBase64, audioSeq)
    }

    @SuppressLint("MissingPermission")
    fun startCaptureLoop(
        scope: CoroutineScope,
        isMutedProvider: () -> Boolean,
        voiceEffectProvider: () -> VoiceEffectType,
        onPacketReady: (encodedAudioBase64: String?, amplitude: Double, seq: Long) -> Unit
    ) {
        startPlaybackTrack(scope)
        recordJob?.cancel()
        recordJob = scope.launch(Dispatchers.IO) {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_SAMPLES * 2)

            val recorder = createBestAudioRecord(minBuf) ?: return@launch
            var aec: AcousticEchoCanceler? = null
            var ns: NoiseSuppressor? = null
            var agc: AutomaticGainControl? = null
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
                }
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true }
                }
                if (AutomaticGainControl.isAvailable()) {
                    agc = AutomaticGainControl.create(recorder.audioSessionId)?.apply { enabled = true }
                }
            } catch (_: Exception) {
            }

            val chunkBuffer = ShortArray(FRAME_SAMPLES)
            var seq = System.currentTimeMillis().coerceAtLeast(1L)
            try {
                recorder.startRecording()
                while (isActive) {
                    if (isMutedProvider()) {
                        withContext(Dispatchers.Main) {
                            onPacketReady(null, 0.0, seq)
                        }
                        delay(200L)
                        continue
                    }

                    var totalRead = 0
                    while (totalRead < FRAME_SAMPLES && isActive) {
                        val r = recorder.read(chunkBuffer, totalRead, FRAME_SAMPLES - totalRead)
                        if (r <= 0) break
                        totalRead += r
                    }

                    if (totalRead > 0) {
                        val validSamples = if (totalRead == FRAME_SAMPLES) {
                            chunkBuffer.copyOf()
                        } else {
                            chunkBuffer.copyOf(totalRead)
                        }
                        val amplified = boostPcmGain(validSamples, gain = 2.35f)
                        var peak = 0
                        for (sample in amplified) {
                            val a = abs(sample.toInt())
                            if (a > peak) peak = a
                        }
                        val amplitude = (peak / 9500.0).coerceIn(0.0, 1.0)
                        val processed = applyVoiceEffect(amplified, voiceEffectProvider())
                        appendToRecording(processed)

                        val ulaw = encodeMuLaw(processed)
                        val encoded = Base64.encodeToString(ulaw, Base64.NO_WRAP)
                        val currentSeq = seq++
                        withContext(Dispatchers.Main) {
                            onPacketReady(encoded, amplitude, currentSeq)
                        }
                    } else {
                        delay(60L)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    aec?.release()
                    ns?.release()
                    agc?.release()
                    recorder.stop()
                } catch (_: Exception) {
                }
                recorder.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createBestAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (source in sources) {
            val rec = try {
                AudioRecord(
                    source,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
            } catch (_: Exception) {
                null
            }
            if (rec != null && rec.state == AudioRecord.STATE_INITIALIZED) {
                return rec
            }
            rec?.release()
        }
        return null
    }

    fun startCallRecording(callId: String) {
        val dir = File(context.filesDir, "call_recordings").apply { mkdirs() }
        val file = File(dir, "neov_call_${callId.take(12)}_${System.currentTimeMillis()}.wav")
        recordingFilePath = file.absolutePath
        recordedStream = ByteArrayOutputStream()
        isRecordingToFile = true
    }

    fun stopCallRecording(): String? {
        isRecordingToFile = false
        val pcmBytes = recordedStream?.toByteArray() ?: return null
        recordedStream = null
        val path = recordingFilePath ?: return null
        return try {
            FileOutputStream(path).use { fos ->
                writeWavHeader(fos, pcmBytes.size, SAMPLE_RATE, 1, 16)
                fos.write(pcmBytes)
            }
            path
        } catch (_: Exception) {
            null
        }
    }

    private fun appendToRecording(pcm: ShortArray) {
        if (!isRecordingToFile) return
        val stream = recordedStream ?: return
        val bb = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) bb.putShort(s)
        synchronized(stream) {
            stream.write(bb.array())
        }
    }

    fun release() {
        recordJob?.cancel()
        recordJob = null
        playbackJob?.cancel()
        playbackJob = null
        synchronized(playbackLock) {
            try {
                audioTrack?.stop()
            } catch (_: Exception) {
            }
            audioTrack?.release()
            audioTrack = null
        }
        lastPlayedSeqBySender.clear()
        if (isRecordingToFile) {
            stopCallRecording()
        }
    }

    companion object {
        const val SAMPLE_RATE = 12000 // Crisp voice bandwidth while keeping Firestore packets compact (<3.5KB)
        private const val FRAME_SAMPLES = 2400 // 200ms per packet -> 2400 bytes µ-law -> ~3.2KB Base64

        fun configureAudioRouting(context: Context, speakerOn: Boolean): AudioRoutingSnapshot {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val prevMode = am?.mode ?: AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            val prevSpeaker = am?.isSpeakerphoneOn ?: false
            var focusReq: AudioFocusRequest? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .build()
                    am?.requestAudioFocus(req)
                    focusReq = req
                }
                am?.mode = AudioManager.MODE_IN_COMMUNICATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am != null) {
                    val desiredType = if (speakerOn) {
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    } else {
                        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    val targetDevice = am.availableCommunicationDevices
                        .firstOrNull { it.type == desiredType }
                    if (targetDevice != null) {
                        am.setCommunicationDevice(targetDevice)
                    } else {
                        @Suppress("DEPRECATION")
                        am.isSpeakerphoneOn = speakerOn
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am?.isSpeakerphoneOn = speakerOn
                }
            } catch (_: Exception) {
            }
            return AudioRoutingSnapshot(prevMode, prevSpeaker, focusReq)
        }

        fun restoreAudioRouting(context: Context, prev: AudioRoutingSnapshot) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am?.clearCommunicationDevice()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && prev.focusRequest != null) {
                    am?.abandonAudioFocusRequest(prev.focusRequest)
                }
                am?.mode = prev.prevMode
                @Suppress("DEPRECATION")
                am?.isSpeakerphoneOn = prev.prevSpeakerOn
            } catch (_: Exception) {
            }
        }

        /**
         * Boosts quiet voice audio with soft-knee limiting so speech is loud and intelligible without harsh clipping.
         */
        fun boostPcmGain(samples: ShortArray, gain: Float = 2.2f): ShortArray {
            if (samples.isEmpty() || gain == 1f) return samples
            val out = ShortArray(samples.size)
            for (i in samples.indices) {
                val amplified = samples[i].toFloat() * gain
                val limited = if (abs(amplified) > 22000f) {
                    (tanh((amplified / 32767f).toDouble()) * 32000.0).toInt()
                } else {
                    amplified.toInt()
                }
                out[i] = limited.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        fun applyVoiceEffect(samples: ShortArray, effect: VoiceEffectType): ShortArray {
            return when (effect) {
                VoiceEffectType.NORMAL -> samples
                VoiceEffectType.DEEP -> {
                    val out = ShortArray(samples.size)
                    for (i in out.indices) {
                        val src = (i * 0.76f).toInt().coerceIn(0, samples.lastIndex)
                        out[i] = samples[src]
                    }
                    out
                }
                VoiceEffectType.HIGH_PITCH -> {
                    val out = ShortArray(samples.size)
                    for (i in out.indices) {
                        val src = ((i * 1.35f).toInt()) % samples.size.coerceAtLeast(1)
                        out[i] = samples[src]
                    }
                    out
                }
                VoiceEffectType.ROBOT -> {
                    val out = ShortArray(samples.size)
                    for (i in samples.indices) {
                        val mod = sin(2.0 * Math.PI * 95.0 * i / SAMPLE_RATE)
                        out[i] = (samples[i] * (0.45 + 0.55 * mod)).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    out
                }
                VoiceEffectType.ECHO -> {
                    val delaySamples = (SAMPLE_RATE * 0.12).toInt()
                    val out = ShortArray(samples.size)
                    for (i in samples.indices) {
                        val dry = samples[i].toInt()
                        val wet = if (i >= delaySamples) (samples[i - delaySamples] * 0.55).toInt() else 0
                        out[i] = (dry + wet).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    out
                }
            }
        }

        fun encodeMuLaw(pcm: ShortArray): ByteArray {
            val out = ByteArray(pcm.size)
            for (i in pcm.indices) {
                out[i] = linearToMuLawSample(pcm[i].toInt())
            }
            return out
        }

        fun decodeMuLaw(ulaw: ByteArray): ShortArray {
            val out = ShortArray(ulaw.size)
            for (i in ulaw.indices) {
                out[i] = muLawToLinearSample(ulaw[i])
            }
            return out
        }

        private fun linearToMuLawSample(sampleIn: Int): Byte {
            var sample = sampleIn
            val sign = if (sample < 0) {
                sample = -sample
                0x80
            } else {
                0x00
            }
            if (sample > 32635) sample = 32635
            sample += 0x84
            var exponent = 7
            var expMask = 0x4000
            while ((sample and expMask) == 0 && exponent > 0) {
                exponent--
                expMask = expMask shr 1
            }
            val mantissa = (sample shr (exponent + 3)) and 0x0F
            return (sign or (exponent shl 4) or mantissa).inv().toByte()
        }

        private fun muLawToLinearSample(uVal: Byte): Short {
            val inv = uVal.toInt().inv() and 0xFF
            val sign = inv and 0x80
            val exponent = (inv and 0x70) shr 4
            val mantissa = inv and 0x0F
            var sample = ((mantissa shl 3) + 0x84) shl exponent
            sample -= 0x84
            return (if (sign != 0) -sample else sample).toShort()
        }

        private fun writeWavHeader(
            out: FileOutputStream,
            pcmDataBytes: Int,
            sampleRate: Int,
            channels: Int,
            bitsPerSample: Int
        ) {
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + pcmDataBytes)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1.toShort())
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((channels * bitsPerSample / 8).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(pcmDataBytes)
            out.write(header.array())
        }
    }
}

/**
 * Helper for recording & playing back amplified voice messages inside Direct Messages & Server Channels.
 */
object VoiceNoteHelper {
    const val RECORD_SAMPLE_RATE = 12000

    @SuppressLint("MissingPermission")
    suspend fun recordVoiceClip(
        shouldContinue: () -> Boolean,
        maxDurationMs: Long = 20_000L,
        onDurationTick: (Long) -> Unit
    ): Pair<String, Long>? = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            RECORD_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val recorder = listOf(MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .firstNotNullOfOrNull { src ->
                try {
                    AudioRecord(
                        src,
                        RECORD_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                    ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
                } catch (_: Exception) {
                    null
                }
            } ?: return@withContext null

        val collected = ByteArrayOutputStream()
        val buf = ShortArray(minBuf / 2)
        val startMs = System.currentTimeMillis()
        try {
            recorder.startRecording()
            while (shouldContinue()) {
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed >= maxDurationMs) break
                val read = recorder.read(buf, 0, buf.size)
                if (read > 0) {
                    val rawSlice = if (read == buf.size) buf else buf.copyOf(read)
                    val boosted = CallAudioEngine.boostPcmGain(rawSlice, gain = 2.35f)
                    val ulaw = CallAudioEngine.encodeMuLaw(boosted)
                    collected.write(ulaw)
                }
                withContext(Dispatchers.Main) {
                    onDurationTick(elapsed)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                recorder.stop()
            } catch (_: Exception) {
            }
            recorder.release()
        }

        val totalMs = (System.currentTimeMillis() - startMs).coerceAtLeast(400L)
        val raw = collected.toByteArray()
        if (raw.isEmpty()) return@withContext null
        val encoded = Base64.encodeToString(raw.take(320_000).toByteArray(), Base64.NO_WRAP)
        "data:audio/ulaw;base64,$encoded" to totalMs
    }

    private val activeTrackLock = Any()
    private val playbackGeneration = java.util.concurrent.atomic.AtomicLong(0L)

    @Volatile
    private var activeTrack: AudioTrack? = null

    fun stopPlayback() {
        playbackGeneration.incrementAndGet()
        val trackToStop: AudioTrack? = synchronized(activeTrackLock) {
            val current = activeTrack
            activeTrack = null
            current
        }
        if (trackToStop != null) {
            try {
                trackToStop.pause()
                trackToStop.flush()
                trackToStop.stop()
            } catch (_: Exception) {
            }
            try {
                trackToStop.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun playVoiceClip(mediaData: String, onFinished: () -> Unit) = withContext(Dispatchers.IO) {
        stopPlayback()
        val myGeneration = playbackGeneration.incrementAndGet()

        val clean = mediaData.substringAfter("base64,", mediaData)
        val ulaw = try {
            Base64.decode(clean, Base64.DEFAULT)
        } catch (_: Exception) {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { onFinished() }
            return@withContext
        }
        val decoded = CallAudioEngine.decodeMuLaw(ulaw)
        if (decoded.isEmpty()) {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { onFinished() }
            return@withContext
        }
        val pcm = CallAudioEngine.boostPcmGain(decoded, gain = 1.6f)

        val minBuf = AudioTrack.getMinBufferSize(
            RECORD_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RECORD_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val registered = synchronized(activeTrackLock) {
            if (playbackGeneration.get() != myGeneration) {
                false
            } else {
                activeTrack = track
                true
            }
        }
        if (!registered) {
            try {
                track.release()
            } catch (_: Exception) {
            }
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { onFinished() }
            return@withContext
        }

        try {
            track.setVolume(AudioTrack.getMaxVolume())
            track.play()
            var offset = 0
            while (isActive && playbackGeneration.get() == myGeneration && offset < pcm.size) {
                val len = (pcm.size - offset).coerceAtMost(512)
                val written = track.write(pcm, offset, len)
                if (written <= 0) break
                offset += written
            }
            if (isActive && playbackGeneration.get() == myGeneration) {
                delay(180L)
            }
        } catch (_: Exception) {
        } finally {
            val shouldReleaseHere = synchronized(activeTrackLock) {
                if (activeTrack === track) {
                    activeTrack = null
                    true
                } else {
                    false
                }
            }
            if (shouldReleaseHere) {
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                } catch (_: Exception) {
                }
                try {
                    track.release()
                } catch (_: Exception) {
                }
            }
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                onFinished()
            }
        }
    }
}
