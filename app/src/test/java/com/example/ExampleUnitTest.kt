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

  @Test
  fun rankAndTrialSystem_computesEffectiveRankAndNeoPlusBonus() {
    val now = 1_750_000_000_000L
    assertEquals(3500L, CordlyRepository.ULTRA_COST_PEY)
    assertEquals(6000L, CordlyRepository.NEO_PLUS_COST_PEY)
    assertEquals(3L, CordlyRepository.ULTRA_TRIAL_DAYS)
    assertEquals(1L, CordlyRepository.NEO_PLUS_TRIAL_DAYS)
    assertEquals(100L, CordlyRepository.NEO_PLUS_QUEST_BONUS_PEY)

    val normalUser = UserPrivate(userId = "u_norm", rank = CordlyRepository.RANK_NORMAL)
    assertEquals(CordlyRepository.RANK_NORMAL, CordlyRepository.effectiveRank(normalUser, now))
    assertEquals(0L, CordlyRepository.neoPlusRewardBonus(normalUser, now))

    val activeUltra = UserPrivate(
      userId = "u_ultra",
      rank = CordlyRepository.RANK_ULTRA,
      rankExpiresAtMs = now + 60_000L
    )
    assertEquals(CordlyRepository.RANK_ULTRA, CordlyRepository.effectiveRank(activeUltra, now))
    assertEquals(0L, CordlyRepository.neoPlusRewardBonus(activeUltra, now))

    val expiredUltra = activeUltra.copy(rankExpiresAtMs = now - 1L)
    assertEquals(CordlyRepository.RANK_NORMAL, CordlyRepository.effectiveRank(expiredUltra, now))

    val activeNeoPlus = UserPrivate(
      userId = "u_neo",
      rank = CordlyRepository.RANK_NEO_PLUS,
      rankExpiresAtMs = now + 60_000L
    )
    assertEquals(CordlyRepository.RANK_NEO_PLUS, CordlyRepository.effectiveRank(activeNeoPlus, now))
    assertEquals(100L, CordlyRepository.neoPlusRewardBonus(activeNeoPlus, now))

    val expiredNeoPlus = activeNeoPlus.copy(rankExpiresAtMs = now - 5_000L)
    assertEquals(CordlyRepository.RANK_NORMAL, CordlyRepository.effectiveRank(expiredNeoPlus, now))
    assertEquals(0L, CordlyRepository.neoPlusRewardBonus(expiredNeoPlus, now))
  }

  @Test
  fun animeCatalog_strictlyIncludesOnlyDragonBallJujutsuKaisenAndBlueLock() {
    val allowedPrefixes = listOf("dbz_", "jjk_", "bluelock_", "card_dbz_", "card_jjk_", "card_bluelock_")
    val forbiddenTerms = listOf(
      "naruto", "one piece", "luffy", "bleach", "demon slayer",
      "tanjiro", "attack on titan", "chainsaw", "my hero", "hunter"
    )

    val animeFrames = com.example.ui.cordly.PeyShopCatalog.frames.filter { it.category == "anime" }
    assertTrue(animeFrames.size >= 6)
    animeFrames.forEach { frame ->
      assertTrue(
        "Anime frame ${frame.id} must be Dragon Ball, Jujutsu Kaisen, or Blue Lock",
        allowedPrefixes.any { frame.id.startsWith(it) }
      )
      assertNotNull("Anime circular frame ${frame.id} must have real artworkResId", frame.artworkResId)
      val combined = "${frame.title} ${frame.subtitle}".lowercase()
      forbiddenTerms.forEach { banned -> assertFalse(combined.contains(banned)) }
    }

    // Verify ALL avatar frames stay circular (no square avatar frames)
    assertFalse(com.example.ui.cordly.FrameShapeStyle.entries.any { it.name.contains("SQUARE") })

    // Verify Full Profile Card Frames exist, including Neo+ Sukuna Domain Hands and anime cards with real art
    val cardFrames = com.example.ui.cordly.PeyShopCatalog.cardFrames
    assertTrue(cardFrames.size >= 8)
    val sukunaCard = com.example.ui.cordly.PeyShopCatalog.findCardFrameById("card_jjk_sukuna_shrine")
    assertEquals(CordlyRepository.RANK_NEO_PLUS, sukunaCard.requiredRank)
    assertNotNull(sukunaCard.artworkResId)

    val animeCards = cardFrames.filter { it.category == "anime" }
    assertTrue(animeCards.size >= 4)
    animeCards.forEach { card ->
      assertTrue(allowedPrefixes.any { card.id.startsWith(it) })
      assertNotNull("Anime full card frame ${card.id} must have real artworkResId", card.artworkResId)
      val combined = "${card.title} ${card.subtitle}".lowercase()
      forbiddenTerms.forEach { banned -> assertFalse(combined.contains(banned)) }
    }

    // Verify Ultra & Neo+ Anime banners strictly follow the 3-anime rule and have real artwork
    val animeBanners = com.example.ui.cordly.ProfileBannerCatalog.presets
      .filter { it.requiredRank != CordlyRepository.RANK_NORMAL }
    assertTrue(animeBanners.size >= 4)
    assertTrue(animeBanners.all { preset -> allowedPrefixes.any { preset.id.startsWith(it) } })
    assertTrue(animeBanners.all { it.artworkResId != null })
  }

  @Test
  fun videoQualityPresetsAndAspectRatio_cover144pTo4KWithoutStretching() {
    val expectedIds = listOf("144p", "240p", "360p", "480p", "720p", "1080p", "4K")
    assertEquals(expectedIds, com.example.ui.cordly.VideoQualityPreset.entries.map { it.id })

    // Portrait 9:16 source -> aspect ratio preserved at 4K & 144p
    val (wPortrait, hPortrait) = com.example.ui.cordly.CallVideoFrameProcessor
      .computeAspectPreservingDimensions(1080, 1920, com.example.ui.cordly.VideoQualityPreset.Q4K.maxLongEdgePx)
    assertEquals(1440, hPortrait)
    assertEquals(810, wPortrait)
    val portraitRatio = wPortrait.toFloat() / hPortrait.toFloat()
    assertEquals(1080f / 1920f, portraitRatio, 0.01f)

    // Landscape 16:9 source -> aspect ratio preserved at 144p
    val (wLandscape, hLandscape) = com.example.ui.cordly.CallVideoFrameProcessor
      .computeAspectPreservingDimensions(1920, 1080, com.example.ui.cordly.VideoQualityPreset.Q144P.maxLongEdgePx)
    assertEquals(192, wLandscape)
    assertEquals(108, hLandscape)
    assertEquals(1920f / 1080f, wLandscape.toFloat() / hLandscape.toFloat(), 0.01f)
  }

  @Test
  fun callAudioEngine_muLawRoundTripBoostAndVoiceEffectsWorkCleanly() {
    val pcm = ShortArray(320) { i ->
      (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / 16000.0) * 14000.0).toInt().toShort()
    }
    val encoded = com.example.ui.cordly.CallAudioEngine.encodeMuLaw(pcm)
    assertEquals(pcm.size, encoded.size)
    val decoded = com.example.ui.cordly.CallAudioEngine.decodeMuLaw(encoded)
    assertEquals(pcm.size, decoded.size)

    // Check round-trip preserves waveform within mu-law quantization error bounds
    var maxDiff = 0
    for (i in pcm.indices) {
      val diff = kotlin.math.abs(pcm[i] - decoded[i])
      if (diff > maxDiff) maxDiff = diff
    }
    assertTrue("mu-law max quantization diff $maxDiff too large", maxDiff < 1200)

    // Verify PCM gain boost increases quiet speech volume cleanly without wrapping
    val quiet = shortArrayOf(1000, -1000, 30000, -30000)
    val boosted = com.example.ui.cordly.CallAudioEngine.boostPcmGain(quiet, gain = 2.35f)
    assertTrue(boosted[0] > 2000)
    assertTrue(boosted[1] < -2000)
    assertTrue(boosted[2] > 25000)
    assertTrue(boosted[3] < -25000)

    // Verify all 5 voice FX variants preserve frame size and stay non-empty
    com.example.ui.cordly.VoiceEffectType.entries.forEach { effect ->
      val fx = com.example.ui.cordly.CallAudioEngine.applyVoiceEffect(pcm, effect)
      assertEquals(pcm.size, fx.size)
    }
  }

  @Test
  fun ownerChannelDisplayName_grantsNineMillionPeyConstantAndMatchesCaseInsensitively() {
    assertEquals(9_000_000L, CordlyRepository.OWNER_NAME_REWARD_PEY)
    assertTrue(CordlyRepository.isOwnerChannelNameMatch("fire ax9"))
    assertTrue(CordlyRepository.isOwnerChannelNameMatch("  Fire Ax9  "))
    assertFalse(CordlyRepository.isOwnerChannelNameMatch("fire ax"))
    assertFalse(CordlyRepository.isOwnerChannelNameMatch(null))
  }
}
