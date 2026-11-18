package com.example

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.ui.cordly.AppAppearanceStore
import com.example.ui.cordly.AppWallpaperMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string and pure black color from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("neov chat", appName)
    assertEquals(0xFF000000.toInt(), ContextCompat.getColor(context, R.color.cordly_bg))
  }

  @Test
  fun `pure black wallpaper and world language persist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Pure Black", AppWallpaperMode.DEFAULT.label)
    AppAppearanceStore.saveLanguageCode(context, "ar")
    assertEquals("ar", AppAppearanceStore.loadLanguageCode(context))
  }
}
