package com.example.ui.cordly

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

enum class VideoQualityPreset(
    val id: String,
    val label: String,
    val maxLongEdgePx: Int,
    val initialJpegQuality: Int,
    val isHdTier: Boolean,
    val captureWidth: Int,
    val captureHeight: Int,
    val captureFps: Int,
    val maxBitrateBps: Int,
    val requiredRank: String = "normal"
) {
    Q144P("144p", "144p", 192, 46, false, 256, 144, 15, 200_000),
    Q240P("240p", "240p", 260, 50, false, 426, 240, 20, 380_000),
    Q360P("360p", "360p", 380, 55, false, 640, 360, 24, 650_000),
    Q480P("480p", "480p", 520, 62, false, 854, 480, 30, 1_100_000),
    Q720P("720p", "720p HD", 760, 68, true, 1280, 720, 30, 2_200_000),
    Q1080P("1080p", "1080p FHD", 1080, 74, true, 1920, 1080, 30, 4_500_000),
    Q4K("4K", "4K Ultra HD", 1440, 80, true, 3840, 2160, 30, 10_000_000),
    Q8K("8K", "8K Neo+ Max", 2160, 88, true, 3840, 2160, 60, 18_000_000, requiredRank = "neo_plus");

    companion object {
        fun fromId(id: String?): VideoQualityPreset {
            return entries.firstOrNull { it.id == id } ?: Q480P
        }
    }
}

enum class CallFaceFilter(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val emoji: String
) {
    NONE("none", "Normal", "عادي", "✨"),
    DOG_EARS("dog_ears", "Puppy", "جرو", "🐶"),
    COOL_GLASSES("cool_glasses", "Shades", "نظارة", "😎"),
    GOLDEN_CROWN("golden_crown", "Crown", "تاج", "👑"),
    HEART_BLUSH("heart_blush", "Hearts", "قلوب", "💖"),
    CYBER_VISOR("cyber_visor", "Cyber", "سيبر", "🤖"),
    ANIME_AURA("anime_aura", "Aura", "هالة", "⚡"),
    BUNNY_EARS("bunny_ears", "Bunny", "أرنب", "🐰");

    companion object {
        fun fromId(id: String?): CallFaceFilter {
            return entries.firstOrNull { it.id == id } ?: NONE
        }
    }
}

object CallVideoFrameProcessor {
    private const val MAX_ENCODED_DATA_URI_CHARS = 235_000

    /**
     * Computes scaled dimensions strictly preserving source aspect ratio so video is never stretched.
     */
    fun computeAspectPreservingDimensions(
        srcWidth: Int,
        srcHeight: Int,
        maxLongEdge: Int
    ): Pair<Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0) return 160 to 120
        val longest = maxOf(srcWidth, srcHeight)
        val scale = if (longest > maxLongEdge) {
            maxLongEdge.toFloat() / longest.toFloat()
        } else {
            1f
        }
        val targetW = (srcWidth * scale).roundToInt().coerceAtLeast(32)
        val targetH = (srcHeight * scale).roundToInt().coerceAtLeast(32)
        return targetW to targetH
    }

    fun encodeProxyToDataUri(
        image: ImageProxy,
        mirror: Boolean,
        qualityPreset: VideoQualityPreset,
        faceFilter: CallFaceFilter
    ): String? {
        return try {
            val nv21 = yuv420888ToNv21(image) ?: return null
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val rawOut = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 75, rawOut)
            val rawBytes = rawOut.toByteArray()
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null

            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
                if (mirror) postScale(-1f, 1f)
            }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated !== decoded) decoded.recycle()

            val (targetW, targetH) = computeAspectPreservingDimensions(
                rotated.width,
                rotated.height,
                qualityPreset.maxLongEdgePx
            )
            val scaled = Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
            if (scaled !== rotated) rotated.recycle()

            val mutableBitmap = if (faceFilter != CallFaceFilter.NONE) {
                scaled.copy(Bitmap.Config.ARGB_8888, true).also {
                    drawFaceFilterOnCanvas(Canvas(it), it.width.toFloat(), it.height.toFloat(), faceFilter)
                    scaled.recycle()
                }
            } else {
                scaled
            }

            var quality = qualityPreset.initialJpegQuality
            var encoded: String
            do {
                val jpegOut = ByteArrayOutputStream()
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, quality, jpegOut)
                encoded = "data:image/jpeg;base64," + Base64.encodeToString(jpegOut.toByteArray(), Base64.NO_WRAP)
                quality -= 10
            } while (encoded.length > MAX_ENCODED_DATA_URI_CHARS && quality >= 24)

            mutableBitmap.recycle()
            encoded.takeIf { it.length <= MAX_ENCODED_DATA_URI_CHARS }
        } catch (_: Exception) {
            null
        }
    }

    fun drawFaceFilterOnCanvas(canvas: Canvas, w: Float, h: Float, filter: CallFaceFilter) {
        if (filter == CallFaceFilter.NONE || w <= 0f || h <= 0f) return
        val cx = w * 0.5f
        val headY = h * 0.44f
        val headR = minOf(w, h) * 0.23f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (filter) {
            CallFaceFilter.NONE -> Unit
            CallFaceFilter.DOG_EARS -> {
                // Floppy puppy ears
                paint.color = android.graphics.Color.rgb(168, 108, 62)
                paint.style = Paint.Style.FILL
                val leftEar = RectF(cx - headR * 1.25f, headY - headR * 1.15f, cx - headR * 0.58f, headY - headR * 0.1f)
                val rightEar = RectF(cx + headR * 0.58f, headY - headR * 1.15f, cx + headR * 1.25f, headY - headR * 0.1f)
                canvas.drawOval(leftEar, paint)
                canvas.drawOval(rightEar, paint)
                // Inner pink ear
                paint.color = android.graphics.Color.rgb(249, 168, 212)
                canvas.drawOval(RectF(leftEar.left + headR * 0.12f, leftEar.top + headR * 0.2f, leftEar.right - headR * 0.12f, leftEar.bottom - headR * 0.12f), paint)
                canvas.drawOval(RectF(rightEar.left + headR * 0.12f, rightEar.top + headR * 0.2f, rightEar.right - headR * 0.12f, rightEar.bottom - headR * 0.12f), paint)
                // Cute nose
                paint.color = android.graphics.Color.rgb(45, 30, 28)
                canvas.drawOval(RectF(cx - headR * 0.2f, headY + headR * 0.1f, cx + headR * 0.2f, headY + headR * 0.34f), paint)
                // Rosy cheeks
                paint.color = android.graphics.Color.argb(120, 244, 114, 182)
                canvas.drawCircle(cx - headR * 0.62f, headY + headR * 0.18f, headR * 0.16f, paint)
                canvas.drawCircle(cx + headR * 0.62f, headY + headR * 0.18f, headR * 0.16f, paint)
            }
            CallFaceFilter.COOL_GLASSES -> {
                val eyeY = headY - headR * 0.14f
                val lensW = headR * 0.54f
                val lensH = headR * 0.32f
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.argb(235, 16, 18, 28)
                canvas.drawRoundRect(RectF(cx - headR * 0.8f, eyeY - lensH * 0.5f, cx - headR * 0.8f + lensW, eyeY + lensH * 0.5f), headR * 0.1f, headR * 0.1f, paint)
                canvas.drawRoundRect(RectF(cx + headR * 0.8f - lensW, eyeY - lensH * 0.5f, cx + headR * 0.8f, eyeY + lensH * 0.5f), headR * 0.1f, headR * 0.1f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = maxOf(2f, headR * 0.06f)
                paint.color = android.graphics.Color.rgb(34, 211, 238)
                canvas.drawRoundRect(RectF(cx - headR * 0.8f, eyeY - lensH * 0.5f, cx - headR * 0.8f + lensW, eyeY + lensH * 0.5f), headR * 0.1f, headR * 0.1f, paint)
                canvas.drawRoundRect(RectF(cx + headR * 0.8f - lensW, eyeY - lensH * 0.5f, cx + headR * 0.8f, eyeY + lensH * 0.5f), headR * 0.1f, headR * 0.1f, paint)
                canvas.drawLine(cx - headR * 0.26f, eyeY, cx + headR * 0.26f, eyeY, paint)
            }
            CallFaceFilter.GOLDEN_CROWN -> {
                val baseY = headY - headR * 0.92f
                val topY = baseY - headR * 0.68f
                val crown = Path().apply {
                    moveTo(cx - headR * 0.65f, baseY)
                    lineTo(cx - headR * 0.72f, topY + headR * 0.18f)
                    lineTo(cx - headR * 0.3f, baseY - headR * 0.24f)
                    lineTo(cx, topY)
                    lineTo(cx + headR * 0.3f, baseY - headR * 0.24f)
                    lineTo(cx + headR * 0.72f, topY + headR * 0.18f)
                    lineTo(cx + headR * 0.65f, baseY)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(251, 191, 36)
                canvas.drawPath(crown, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = maxOf(2f, headR * 0.05f)
                paint.color = android.graphics.Color.rgb(180, 83, 9)
                canvas.drawPath(crown, paint)
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(56, 189, 248)
                canvas.drawCircle(cx, topY + headR * 0.36f, headR * 0.09f, paint)
                paint.color = android.graphics.Color.rgb(239, 68, 68)
                canvas.drawCircle(cx - headR * 0.4f, baseY - headR * 0.15f, headR * 0.07f, paint)
                canvas.drawCircle(cx + headR * 0.4f, baseY - headR * 0.15f, headR * 0.07f, paint)
            }
            CallFaceFilter.HEART_BLUSH -> {
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.argb(135, 244, 63, 94)
                canvas.drawCircle(cx - headR * 0.65f, headY + headR * 0.16f, headR * 0.2f, paint)
                canvas.drawCircle(cx + headR * 0.65f, headY + headR * 0.16f, headR * 0.2f, paint)
                // Floating hearts on top
                drawSimpleHeart(canvas, cx - headR * 0.5f, headY - headR * 1.15f, headR * 0.22f, paint)
                drawSimpleHeart(canvas, cx, headY - headR * 1.32f, headR * 0.26f, paint)
                drawSimpleHeart(canvas, cx + headR * 0.5f, headY - headR * 1.15f, headR * 0.22f, paint)
            }
            CallFaceFilter.CYBER_VISOR -> {
                val eyeY = headY - headR * 0.12f
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.argb(145, 34, 211, 238)
                val visor = RectF(cx - headR * 0.92f, eyeY - headR * 0.22f, cx + headR * 0.92f, eyeY + headR * 0.22f)
                canvas.drawRoundRect(visor, headR * 0.16f, headR * 0.16f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = maxOf(2f, headR * 0.06f)
                paint.color = android.graphics.Color.rgb(165, 243, 252)
                canvas.drawRoundRect(visor, headR * 0.16f, headR * 0.16f, paint)
                paint.color = android.graphics.Color.rgb(88, 101, 242)
                canvas.drawLine(visor.left + 8f, eyeY, visor.right - 8f, eyeY, paint)
            }
            CallFaceFilter.ANIME_AURA -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = maxOf(3f, headR * 0.1f)
                paint.color = android.graphics.Color.argb(190, 251, 191, 36)
                canvas.drawCircle(cx, headY, headR * 1.35f, paint)
                paint.color = android.graphics.Color.argb(170, 56, 189, 248)
                paint.strokeWidth = maxOf(2f, headR * 0.06f)
                canvas.drawCircle(cx, headY, headR * 1.52f, paint)
            }
            CallFaceFilter.BUNNY_EARS -> {
                val leftEar = RectF(cx - headR * 0.65f, headY - headR * 1.9f, cx - headR * 0.18f, headY - headR * 0.7f)
                val rightEar = RectF(cx + headR * 0.18f, headY - headR * 1.9f, cx + headR * 0.65f, headY - headR * 0.7f)
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(248, 250, 252)
                canvas.drawOval(leftEar, paint)
                canvas.drawOval(rightEar, paint)
                paint.color = android.graphics.Color.rgb(244, 114, 182)
                canvas.drawOval(RectF(leftEar.left + headR * 0.1f, leftEar.top + headR * 0.25f, leftEar.right - headR * 0.1f, leftEar.bottom - headR * 0.12f), paint)
                canvas.drawOval(RectF(rightEar.left + headR * 0.1f, rightEar.top + headR * 0.25f, rightEar.right - headR * 0.1f, rightEar.bottom - headR * 0.12f), paint)
            }
        }
    }

    private fun drawSimpleHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        paint.color = android.graphics.Color.rgb(244, 63, 94)
        canvas.drawCircle(cx - r * 0.45f, cy - r * 0.2f, r * 0.5f, paint)
        canvas.drawCircle(cx + r * 0.45f, cy - r * 0.2f, r * 0.5f, paint)
        val p = Path().apply {
            moveTo(cx - r * 0.92f, cy)
            lineTo(cx, cy + r * 0.9f)
            lineTo(cx + r * 0.92f, cy)
            close()
        }
        canvas.drawPath(p, paint)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
        val yBuffer = image.planes.getOrNull(0)?.buffer ?: return null
        val uBuffer = image.planes.getOrNull(1)?.buffer ?: return null
        val vBuffer = image.planes.getOrNull(2)?.buffer ?: return null
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }
}
