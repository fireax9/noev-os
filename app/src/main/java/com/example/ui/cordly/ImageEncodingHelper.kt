package com.example.ui.cordly

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ImageEncodingHelper {

    suspend fun encodePickedImageToDataUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 160,
        maxHeight: Int = 160,
        maxBase64Bytes: Int = 75_000
    ): String? = withContext(Dispatchers.IO) {
        val decoded = decodeSampledBitmap(context, uri, maxWidth * 2, maxHeight * 2) ?: return@withContext null
        try {
            var targetW = decoded.width.coerceAtLeast(1)
            var targetH = decoded.height.coerceAtLeast(1)
            val initialScale = min(
                maxWidth.toFloat() / targetW,
                maxHeight.toFloat() / targetH
            ).coerceAtMost(1f)
            targetW = max(1, (targetW * initialScale).toInt())
            targetH = max(1, (targetH * initialScale).toInt())

            var scaled = if (targetW != decoded.width || targetH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
            } else {
                decoded
            }

            try {
                var quality = 80
                var encodedBytes: ByteArray
                do {
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    encodedBytes = baos.toByteArray()
                    quality -= 10
                } while (encodedBytes.size > maxBase64Bytes && quality >= 25)

                // If still oversized (e.g., dense noise PNG), shrink dimensions once more
                if (encodedBytes.size > maxBase64Bytes && scaled.width > 64 && scaled.height > 64) {
                    val smaller = Bitmap.createScaledBitmap(
                        scaled,
                        max(48, scaled.width * 2 / 3),
                        max(48, scaled.height * 2 / 3),
                        true
                    )
                    if (scaled !== decoded) scaled.recycle()
                    scaled = smaller
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 55, baos)
                    encodedBytes = baos.toByteArray()
                }

                val b64 = Base64.encodeToString(encodedBytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            } finally {
                if (scaled !== decoded) scaled.recycle()
            }
        } catch (_: Exception) {
            null
        } finally {
            decoded.recycle()
        }
    }

    suspend fun savePickedWallpaperToInternalStorage(
        context: Context,
        uri: Uri,
        destFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val decoded = decodeSampledBitmap(context, uri, 1080, 1920) ?: return@withContext false
        try {
            val scale = min(
                1080f / decoded.width.coerceAtLeast(1),
                1920f / decoded.height.coerceAtLeast(1)
            ).coerceAtMost(1f)
            val targetW = max(1, (decoded.width * scale).toInt())
            val targetH = max(1, (decoded.height * scale).toInt())
            val scaled = if (targetW != decoded.width || targetH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
            } else {
                decoded
            }
            try {
                destFile.parentFile?.mkdirs()
                val tmpDest = File(destFile.parentFile, "${destFile.name}.tmp")
                FileOutputStream(tmpDest).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (destFile.exists()) destFile.delete()
                tmpDest.renameTo(destFile) || tmpDest.copyTo(destFile, overwrite = true).let {
                    tmpDest.delete()
                    true
                }
            } finally {
                if (scaled !== decoded) scaled.recycle()
            }
        } catch (_: Exception) {
            false
        } finally {
            decoded.recycle()
        }
    }

    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        // Strategy 1 (API 28+): ImageDecoder handles JPEG/PNG/WebP/HEIC/EXIF rotation in a single open
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val srcW = info.size.width.coerceAtLeast(1)
                    val srcH = info.size.height.coerceAtLeast(1)
                    val scale = min(reqWidth.toFloat() / srcW, reqHeight.toFloat() / srcH)
                        .coerceAtMost(1f)
                    val dstW = max(1, (srcW * scale).toInt())
                    val dstH = max(1, (srcH * scale).toInt())
                    decoder.setTargetSize(dstW, dstH)
                }
                if (bitmap != null) return bitmap
            } catch (_: Throwable) {
                // Fall through to single-copy File + BitmapFactory strategy
            }
        }

        // Strategy 2: Open InputStream ONCE into a temp cache file, then inspect bounds & decode reliably
        val tempFile = File(context.cacheDir, "pick_decode_${System.nanoTime()}.img")
        return try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
                true
            } ?: false

            if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                // Strategy 3: Try ParcelFileDescriptor if InputStream was null
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val raw = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                    return raw
                }
                return null
            }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null
            }

            val inSample = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions) ?: return null
            applyExifOrientationIfNeeded(tempFile, bitmap)
        } catch (_: Throwable) {
            null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun applyExifOrientationIfNeeded(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated !== bitmap) bitmap.recycle()
                rotated
            }
        } catch (_: Throwable) {
            bitmap
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

private val dataUriBitmapCache = object : LruCache<String, ImageBitmap>(32) {}

@Composable
fun rememberDecodedImageBitmap(source: String?): ImageBitmap? {
    val isDataUri = !source.isNullOrBlank() && source.startsWith("data:image/", ignoreCase = true)
    val cached = if (isDataUri) dataUriBitmapCache.get(source) else null
    val state by produceState<ImageBitmap?>(initialValue = cached, key1 = source) {
        if (!isDataUri || source == null) {
            value = null
            return@produceState
        }
        val hit = dataUriBitmapCache.get(source)
        if (hit != null) {
            value = hit
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val commaIdx = source.indexOf(',')
            if (commaIdx == -1) {
                null
            } else {
                try {
                    val bytes = Base64.decode(source.substring(commaIdx + 1), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.also { bmp ->
                        dataUriBitmapCache.put(source, bmp)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    return state
}

@Composable
fun rememberFileImageBitmap(filePath: String?, version: Long): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = filePath, key2 = version) {
        value = if (filePath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    if (!file.exists() || file.length() == 0L) null
                    else BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
    return state
}

@Composable
fun PresetBannerCanvas(
    preset: ProfileBannerPreset,
    modifier: Modifier = Modifier
) {
    val artRes = preset.artworkResId
    if (artRes != null) {
        Box(modifier = modifier) {
            Image(
                painter = painterResource(artRes),
                contentDescription = preset.titleEn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                (preset.colors.firstOrNull() ?: Color.Black).copy(alpha = 0.16f),
                                Color.Black.copy(alpha = 0.42f)
                            )
                        )
                    )
            )
        }
        return
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val primary = preset.colors.getOrElse(0) { Color(0xFF0C05AB) }
        val mid = preset.colors.getOrElse(1) { Color(0xFF5865F2) }
        val accent = preset.colors.getOrElse(2) { Color(0xFF22D3EE) }

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(primary, mid, accent),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        when (preset.pattern) {
            BannerPatternType.DISCORD_SPLASH -> {
                drawCircle(
                    color = accent.copy(alpha = 0.25f),
                    radius = h * 0.65f,
                    center = Offset(w * 0.85f, h * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = h * 0.45f,
                    center = Offset(w * 0.14f, h * 0.88f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = h * 0.55f,
                    center = Offset(w * 0.82f, h * 0.25f),
                    style = Stroke(width = 2.5f)
                )
            }
            BannerPatternType.HEX_CYBER -> {
                val r = (min(w, h) * 0.24f).coerceAtLeast(16f)
                val dx = r * 1.65f
                val dy = r * 1.45f
                var row = 0
                var y = r * 0.5f
                while (y < h + r) {
                    var x = if (row % 2 == 0) r * 0.6f else r * 0.6f + dx * 0.5f
                    while (x < w + r) {
                        val hexPath = Path().apply {
                            for (i in 0..5) {
                                val angle = (60 * i - 30) * PI / 180.0
                                val px = x + (r * 0.84f) * cos(angle).toFloat()
                                val py = y + (r * 0.84f) * sin(angle).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(
                            path = hexPath,
                            color = accent.copy(alpha = 0.34f),
                            style = Stroke(width = 1.8f)
                        )
                        x += dx
                    }
                    y += dy
                    row++
                }
            }
            BannerPatternType.AURORA_WAVE -> {
                for (waveIdx in 0..2) {
                    val baseY = h * (0.35f + waveIdx * 0.2f)
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseY)
                        cubicTo(
                            w * 0.3f, baseY - h * 0.35f,
                            w * 0.68f, baseY + h * 0.32f,
                            w, baseY - h * 0.12f
                        )
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = wavePath,
                        color = if (waveIdx % 2 == 0) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f)
                    )
                }
            }
            BannerPatternType.PRISM_SHARDS -> {
                val shards = listOf(
                    Triple(0.18f, 0.35f, 0.26f),
                    Triple(0.48f, 0.62f, 0.34f),
                    Triple(0.78f, 0.32f, 0.30f),
                    Triple(0.90f, 0.78f, 0.22f)
                )
                shards.forEachIndexed { idx, (cxF, cyF, sizeF) ->
                    val cx = w * cxF
                    val cy = h * cyF
                    val d = min(w, h) * sizeF
                    val diamond = Path().apply {
                        moveTo(cx, cy - d)
                        lineTo(cx + d * 0.8f, cy)
                        lineTo(cx, cy + d)
                        lineTo(cx - d * 0.8f, cy)
                        close()
                    }
                    rotate(degrees = (idx * 22f) - 18f, pivot = Offset(cx, cy)) {
                        drawPath(diamond, color = Color.White.copy(alpha = 0.16f))
                        drawPath(diamond, color = accent.copy(alpha = 0.52f), style = Stroke(width = 2f))
                    }
                }
            }
            BannerPatternType.SUNSET_RAYS -> {
                val sunCenter = Offset(w * 0.75f, h * 0.85f)
                drawCircle(
                    color = accent.copy(alpha = 0.55f),
                    radius = h * 0.45f,
                    center = sunCenter
                )
                for (i in 0..8) {
                    val angle = (-175f + i * 21f) * PI / 180.0
                    val endX = sunCenter.x + w * 0.9f * cos(angle).toFloat()
                    val endY = sunCenter.y + w * 0.9f * sin(angle).toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.16f),
                        start = sunCenter,
                        end = Offset(endX, endY),
                        strokeWidth = 6f
                    )
                }
            }
            BannerPatternType.SAKURA_PETALS -> {
                val petals = listOf(
                    Offset(0.15f, 0.25f), Offset(0.32f, 0.68f), Offset(0.52f, 0.30f),
                    Offset(0.70f, 0.72f), Offset(0.84f, 0.26f), Offset(0.92f, 0.58f)
                )
                petals.forEachIndexed { idx, p ->
                    val cx = w * p.x
                    val cy = h * p.y
                    val r = min(w, h) * 0.14f
                    rotate(degrees = idx * 35f, pivot = Offset(cx, cy)) {
                        drawOval(
                            color = Color.White.copy(alpha = 0.26f),
                            topLeft = Offset(cx - r, cy - r * 0.52f),
                            size = androidx.compose.ui.geometry.Size(r * 2f, r)
                        )
                    }
                }
            }
            BannerPatternType.MATRIX_GRID -> {
                val step = 28f
                var x = step
                while (x < w) {
                    drawLine(
                        color = accent.copy(alpha = 0.22f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.2f
                    )
                    x += step
                }
                var y = step
                while (y < h) {
                    drawLine(
                        color = accent.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.2f
                    )
                    y += step
                }
                drawCircle(
                    color = accent.copy(alpha = 0.35f),
                    radius = h * 0.42f,
                    center = Offset(w * 0.8f, h * 0.5f),
                    style = Stroke(width = 3f)
                )
            }
            BannerPatternType.NEBULA_STARS -> {
                val stars = listOf(
                    Triple(0.12f, 0.28f, 3.2f), Triple(0.24f, 0.72f, 2.5f),
                    Triple(0.38f, 0.20f, 4.0f), Triple(0.54f, 0.55f, 3.0f),
                    Triple(0.66f, 0.25f, 4.5f), Triple(0.78f, 0.74f, 3.5f),
                    Triple(0.89f, 0.36f, 5.0f), Triple(0.45f, 0.80f, 2.5f)
                )
                stars.forEach { (xf, yf, rad) ->
                    val cx = w * xf
                    val cy = h * yf
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = rad, center = Offset(cx, cy))
                    drawCircle(color = accent.copy(alpha = 0.32f), radius = rad * 2.6f, center = Offset(cx, cy))
                }
            }
        }
    }
}

@Composable
fun CordlyRemoteOrDataImage(
    source: String?,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val bannerPreset = ProfileBannerCatalog.findBySource(source)
    if (bannerPreset != null) {
        PresetBannerCanvas(
            preset = bannerPreset,
            modifier = modifier
        )
        return
    }
    val decodedBitmap = rememberDecodedImageBitmap(source)
    when {
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        !source.isNullOrBlank() -> {
            AsyncImage(
                model = source,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    }
}
