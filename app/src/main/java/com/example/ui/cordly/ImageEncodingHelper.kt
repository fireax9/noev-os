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
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

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
fun CordlyRemoteOrDataImage(
    source: String?,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
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
