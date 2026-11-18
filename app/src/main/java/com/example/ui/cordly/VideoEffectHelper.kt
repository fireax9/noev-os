package com.example.ui.cordly

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

sealed interface ProfileVideoSource {
    data class RawRes(val resId: Int) : ProfileVideoSource
    data class LocalFile(val file: File) : ProfileVideoSource
}

data class ProcessedProfileVideo(
    val localFile: File,
    val dataUri: String?,
    val durationMs: Long,
    val cloudSynced: Boolean
)

object VideoEffectHelper {
    const val MAX_PROFILE_VIDEO_MS = 5_000L
    private const val MAX_CLOUD_VIDEO_RAW_BYTES = 520_000 // ~693 KB Base64 (< 750 KB rule cap)
    private const val DATA_VIDEO_PREFIX = "data:video/mp4;base64,"

    suspend fun processPickedVideoTo5Seconds(
        context: Context,
        sourceUri: Uri,
        uid: String
    ): Result<ProcessedProfileVideo> = withContext(Dispatchers.IO) {
        try {
            val destFile = AppAppearanceStore.customProfileVideoFile(context, uid)
            val tempCopy = File(context.cacheDir, "temp_picked_video_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempCopy.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected video."))

            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            try {
                retriever.setDataSource(tempCopy.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }

            if (durationMs <= 0L) {
                tempCopy.delete()
                return@withContext Result.failure(IllegalArgumentException("Selected file is not a valid video."))
            }

            val trimOk = trimVideoTo5Seconds(tempCopy, destFile, maxDurationUs = MAX_PROFILE_VIDEO_MS * 1000L)
            if (!trimOk) {
                // Fallback if track copy fails on unusual container: copy if <= 5s
                if (durationMs <= 5_500L) {
                    tempCopy.copyTo(destFile, overwrite = true)
                } else {
                    tempCopy.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Could not trim this video format to 5 seconds. Try an MP4 clip.")
                    )
                }
            }
            tempCopy.delete()

            val finalDuration = durationMs.coerceAtMost(MAX_PROFILE_VIDEO_MS)
            val bytes = destFile.readBytes()
            val dataUri = if (bytes.size <= MAX_CLOUD_VIDEO_RAW_BYTES) {
                DATA_VIDEO_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }

            Result.success(
                ProcessedProfileVideo(
                    localFile = destFile,
                    dataUri = dataUri,
                    durationMs = finalDuration,
                    cloudSynced = dataUri != null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processPickedFullVideo(
        context: Context,
        sourceUri: Uri,
        uid: String
    ): Result<ProcessedProfileVideo> = withContext(Dispatchers.IO) {
        try {
            val destFile = AppAppearanceStore.customProfileUnlimitedVideoFile(context, uid)
            if (destFile.exists()) destFile.delete()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected video."))

            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            try {
                retriever.setDataSource(destFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }

            if (durationMs <= 0L || destFile.length() <= 0L) {
                destFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Selected file is not a valid video."))
            }

            val dataUri = if (destFile.length() <= MAX_CLOUD_VIDEO_RAW_BYTES) {
                val bytes = destFile.readBytes()
                DATA_VIDEO_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }

            Result.success(
                ProcessedProfileVideo(
                    localFile = destFile,
                    dataUri = dataUri,
                    durationMs = durationMs,
                    cloudSynced = dataUri != null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun trimVideoTo5Seconds(
        inputFile: File,
        outputFile: File,
        maxDurationUs: Long
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(inputFile.absolutePath)
            var videoTrackIndex = -1
            var rotationDegrees = 0
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                        rotationDegrees = format.getInteger(MediaFormat.KEY_ROTATION)
                    }
                    break
                }
            }
            if (videoTrackIndex < 0) return false

            extractor.selectTrack(videoTrackIndex)
            val trackFormat = extractor.getTrackFormat(videoTrackIndex)
            if (outputFile.exists()) outputFile.delete()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotationDegrees)
            val dstTrack = muxer.addTrack(trackFormat)
            muxer.start()

            val maxInputSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(256 * 1024)
            } else {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var firstSampleTimeUs = -1L
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (firstSampleTimeUs < 0L) {
                    firstSampleTimeUs = sampleTimeUs
                }
                val relativeUs = sampleTimeUs - firstSampleTimeUs
                if (relativeUs > maxDurationUs) {
                    break
                }

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = relativeUs.coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            outputFile.exists() && outputFile.length() > 0L
        } catch (_: Exception) {
            false
        } finally {
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    fun resolveVideoSource(
        context: Context,
        uid: String,
        equippedEffect: String?,
        profileVideoData: String?
    ): ProfileVideoSource? {
        val effect = PeyShopCatalog.findEffectById(equippedEffect)
        if (effect.id == "none") return null

        if (effect.rawResId != null) {
            return ProfileVideoSource.RawRes(effect.rawResId)
        }

        if (effect.id == "custom_video_unlimited") {
            val unlimitedFile = AppAppearanceStore.customProfileUnlimitedVideoFile(context, uid)
            if (unlimitedFile.exists() && unlimitedFile.length() > 0L) {
                return ProfileVideoSource.LocalFile(unlimitedFile)
            }
            if (!profileVideoData.isNullOrBlank() && profileVideoData.startsWith(DATA_VIDEO_PREFIX)) {
                val cached = materializeDataUriToCache(context, uid, profileVideoData)
                if (cached != null && cached.exists()) {
                    return ProfileVideoSource.LocalFile(cached)
                }
            }
        }

        if (effect.id == "custom_video") {
            if (!profileVideoData.isNullOrBlank() && profileVideoData.startsWith(DATA_VIDEO_PREFIX)) {
                val cached = materializeDataUriToCache(context, uid, profileVideoData)
                if (cached != null && cached.exists()) {
                    return ProfileVideoSource.LocalFile(cached)
                }
            }
            val localFile = AppAppearanceStore.customProfileVideoFile(context, uid)
            if (localFile.exists() && localFile.length() > 0L) {
                return ProfileVideoSource.LocalFile(localFile)
            }
        }

        return null
    }

    private fun materializeDataUriToCache(
        context: Context,
        uid: String,
        dataUri: String
    ): File? {
        return try {
            val hash = dataUri.hashCode().toUInt().toString(16)
            val cachedFile = File(context.cacheDir, "prof_vid_${uid}_${hash}.mp4")
            if (!cachedFile.exists() || cachedFile.length() == 0L) {
                val rawBase64 = dataUri.removePrefix(DATA_VIDEO_PREFIX)
                val decoded = Base64.decode(rawBase64, Base64.DEFAULT)
                cachedFile.writeBytes(decoded)
            }
            cachedFile
        } catch (_: Exception) {
            null
        }
    }
}
