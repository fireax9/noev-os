package com.example

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.ui.cordly.AppAccentColor
import com.example.ui.cordly.AppAppearanceStore
import com.example.ui.cordly.AppThemeMode
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

  @Test
  fun `night day theme mode and app accent color persist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(AppThemeMode.NIGHT, AppAppearanceStore.loadThemeMode(context))
    assertEquals(AppAccentColor.BLURPLE, AppAppearanceStore.loadAccentColor(context))

    AppAppearanceStore.saveThemeMode(context, AppThemeMode.DAY)
    AppAppearanceStore.saveAccentColor(context, AppAccentColor.CRIMSON)

    assertEquals(AppThemeMode.DAY, AppAppearanceStore.loadThemeMode(context))
    assertEquals(AppAccentColor.CRIMSON, AppAppearanceStore.loadAccentColor(context))
  }

  @Test
  fun `firestore database id matches provisioned database and not default`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("ai-studio-android-cordly-00175bc8-627e-456d-b1c6-c796f1aff3b1", dbId)
  }

  @Test
  fun `neo plus app launcher logo and ui layout prefs persist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(com.example.ui.cordly.AppLogoVariant.DEFAULT, AppAppearanceStore.loadLogoVariant(context))
    AppAppearanceStore.applyAppLogoVariant(context, com.example.ui.cordly.AppLogoVariant.GOLD)
    assertEquals(com.example.ui.cordly.AppLogoVariant.GOLD, AppAppearanceStore.loadLogoVariant(context))

    val customLayout = com.example.ui.cordly.AppUiLayoutPrefs(
      railOnRight = true,
      compactMessageCards = true
    )
    AppAppearanceStore.saveLayoutPrefs(context, customLayout)
    assertEquals(customLayout, AppAppearanceStore.loadLayoutPrefs(context))
  }
}
