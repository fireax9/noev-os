package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.UserPrivate
import com.example.data.repository.CordlyRepository
import com.example.ui.cordly.AppAccentColor
import com.example.ui.cordly.AppLanguageManager
import com.example.ui.cordly.AppThemeMode
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyThemeState
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class ExampleUnitTest {

  @Test
  fun dailyReward_awards100ForFirstTwoDays_then50PerDay() {
    val startMs = 1_700_000_000_000L
    val createdAt = Timestamp(Date(startMs))

    val freshProfile = UserPrivate(
      userId = "u1",
      peyPoints = 200L,
      dailyClaimsCount = 0L,
      lastDailyClaimAtMs = 0L,
      createdAt = createdAt
    )

    // Before 24 hours pass: locked
    val early = CordlyRepository.computeDailyRewardStatus(freshProfile, startMs + 3_600_000L)
    assertFalse(early.isClaimable)
    assertEquals(100L, early.nextRewardAmount)
    assertTrue(early.remainingMs > 0L)

    // After 1 day passes: Claim #1 awards 100 P E Y
    val day1 = CordlyRepository.computeDailyRewardStatus(freshProfile, startMs + CordlyRepository.ONE_DAY_MS)
    assertTrue(day1.isClaimable)
    assertEquals(100L, day1.nextRewardAmount)

    // After 1 claim, before next day: locked, next amount still 100 P E Y
    val afterOneClaimProfile = freshProfile.copy(
      peyPoints = 300L,
      dailyClaimsCount = 1L,
      lastDailyClaimAtMs = startMs + CordlyRepository.ONE_DAY_MS
    )
    val day2Ready = CordlyRepository.computeDailyRewardStatus(
      afterOneClaimProfile,
      startMs + 2L * CordlyRepository.ONE_DAY_MS
    )
    assertTrue(day2Ready.isClaimable)
    assertEquals(100L, day2Ready.nextRewardAmount)

    // After 2 claims: next claims award 50 P E Y daily
    val afterTwoClaimsProfile = afterOneClaimProfile.copy(
      peyPoints = 400L,
      dailyClaimsCount = 2L,
      lastDailyClaimAtMs = startMs + 2L * CordlyRepository.ONE_DAY_MS
    )
    val day3Ready = CordlyRepository.computeDailyRewardStatus(
      afterTwoClaimsProfile,
      startMs + 3L * CordlyRepository.ONE_DAY_MS
    )
    assertTrue(day3Ready.isClaimable)
    assertEquals(50L, day3Ready.nextRewardAmount)
  }

  @Test
  fun creatorChannelName_isFireAx9() {
    assertEquals("fire ax9", CordlyRepository.CREATOR_CHANNEL_NAME)
    assertEquals(200L, CordlyRepository.CHANNEL_REWARD_PEY_POINTS)
  }

  @Test
  fun worldLanguages_supportsAllGlobalLocalesAndRtl() {
    assertTrue(AppLanguageManager.allWorldLanguages.size >= 50)
    assertEquals(LayoutDirection.Rtl, AppLanguageManager.layoutDirectionFor("ar"))
    assertEquals(LayoutDirection.Ltr, AppLanguageManager.layoutDirectionFor("en"))
    assertEquals("متجر P E Y", AppLanguageManager.stringsFor("ar").tabPeyShop)
    assertEquals("P E Y Shop", AppLanguageManager.stringsFor("en").tabPeyShop)
    assertEquals("وضع الليل", AppLanguageManager.stringsFor("ar").nightModeLabel)
    assertEquals("وضع النهار", AppLanguageManager.stringsFor("ar").dayModeLabel)
    assertEquals("لون التطبيق", AppLanguageManager.stringsFor("ar").appColorTitle)
    assertNotEquals(
      AppLanguageManager.stringsFor("en").voiceCallAction,
      AppLanguageManager.stringsFor("fr").voiceCallAction
    )
  }

  @Test
  fun themeState_switchesBetweenNightDayAndAppColors() {
    CordlyThemeState.themeMode = AppThemeMode.NIGHT
    CordlyThemeState.accentColor = AppAccentColor.BLURPLE
    assertEquals(Color(0xFF000000), CordlyCanvasBg)
    assertEquals(Color(0xFFF7F8FA), CordlyTextPrimary)
    assertEquals(AppAccentColor.BLURPLE.primary, CordlyBlurple)

    CordlyThemeState.themeMode = AppThemeMode.DAY
    CordlyThemeState.accentColor = AppAccentColor.EMERALD
    assertEquals(Color(0xFFFFFFFF), CordlyCanvasBg)
    assertEquals(Color(0xFF0F172A), CordlyTextPrimary)
    assertEquals(AppAccentColor.EMERALD.primary, CordlyBlurple)

    // Reset defaults
    CordlyThemeState.themeMode = AppThemeMode.NIGHT
    CordlyThemeState.accentColor = AppAccentColor.BLURPLE
  }

  @Test
  fun shopCatalog_includesUnlimitedVideoAndGeometricShapes() {
    val unlimited = com.example.ui.cordly.PeyShopCatalog.findEffectById("custom_video_unlimited")
    assertEquals("custom_video_unlimited", unlimited.id)
    assertEquals(1000L, unlimited.price)
    assertTrue(unlimited.isCustomUpload)
    assertTrue(unlimited.isUnlimitedDuration)

    val custom5s = com.example.ui.cordly.PeyShopCatalog.findEffectById("custom_video")
    assertEquals(200L, custom5s.price)
    assertFalse(custom5s.isUnlimitedDuration)

    assertTrue(com.example.ui.cordly.PeyShopCatalog.frames.size >= 12)
    val shapeCount = com.example.ui.cordly.PeyShopCatalog.frames.map { it.shapeStyle }.toSet().size
    assertTrue(shapeCount >= 5)

    assertTrue(com.example.ui.cordly.ProfileBannerCatalog.presets.size >= 8)
    assertNotNull(com.example.ui.cordly.ProfileBannerCatalog.findBySource("preset_banner:discord_splash"))

    assertTrue(com.example.ui.cordly.AppWallpaperMode.SHAPE_HEX.isShapeWallpaper())
    assertTrue(com.example.ui.cordly.AppWallpaperMode.SHAPE_AURORA.isShapeWallpaper())
    assertTrue(com.example.ui.cordly.AppWallpaperMode.SHAPE_WAVES.isShapeWallpaper())
    assertTrue(com.example.ui.cordly.AppWallpaperMode.SHAPE_GALAXY.isShapeWallpaper())
  }
}
