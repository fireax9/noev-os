package com.example.ui.cordly

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageEncodingHelperTest {

    private fun createSampleImageFile(context: Context, name: String, width: Int, height: Int): File {
        val file = File(context.cacheDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(32, 80, 180))
        val paint = Paint().apply { color = Color.CYAN }
        canvas.drawCircle(width / 2f, height / 2f, width / 3f, paint)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        return file
    }

    @Test
    fun `decodeSampledBitmap decodes real file URI instead of returning null`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = createSampleImageFile(context, "sample_avatar.png", 512, 512)
        val uri = Uri.fromFile(sourceFile)

        val decoded = ImageEncodingHelper.decodeSampledBitmap(context, uri, 160, 160)
        assertNotNull("Decoded bitmap must not be null for valid image URI", decoded)
        assertTrue("Decoded width should be > 0", decoded!!.width > 0)
        assertTrue("Decoded height should be > 0", decoded.height > 0)
        decoded.recycle()
    }

    @Test
    fun `encodePickedImageToDataUri produces valid base64 data URI within Firestore limits`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = createSampleImageFile(context, "sample_banner.png", 800, 400)
        val uri = Uri.fromFile(sourceFile)

        val dataUri = ImageEncodingHelper.encodePickedImageToDataUri(
            context = context,
            uri = uri,
            maxWidth = 400,
            maxHeight = 160,
            maxBase64Bytes = 55_000
        )

        assertNotNull("Encoded data URI must not be null", dataUri)
        assertTrue(dataUri!!.startsWith("data:image/jpeg;base64,"))
        assertTrue("Data URI length (${dataUri.length}) must fit Firestore rule (<120000)", dataUri.length <= 120_000)

        val base64Part = dataUri.substringAfter(",")
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        val redecoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("Data URI must decode cleanly back into Bitmap", redecoded)
        redecoded!!.recycle()
    }

    @Test
    fun `savePickedWallpaperToInternalStorage saves and overwrites wallpaper file`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = createSampleImageFile(context, "sample_wallpaper.png", 1080, 1920)
        val destFile = AppAppearanceStore.customWallpaperFile(context)

        val firstSave = ImageEncodingHelper.savePickedWallpaperToInternalStorage(
            context = context,
            uri = Uri.fromFile(sourceFile),
            destFile = destFile
        )
        assertTrue("First wallpaper save should succeed", firstSave)
        assertTrue("Wallpaper file should exist and be non-empty", destFile.exists() && destFile.length() > 0L)

        val secondSave = ImageEncodingHelper.savePickedWallpaperToInternalStorage(
            context = context,
            uri = Uri.fromFile(sourceFile),
            destFile = destFile
        )
        assertTrue("Overwriting existing wallpaper file should succeed", secondSave)
        val bmp = BitmapFactory.decodeFile(destFile.absolutePath)
        assertNotNull("Saved wallpaper file should decode cleanly", bmp)
        bmp!!.recycle()
    }
}
