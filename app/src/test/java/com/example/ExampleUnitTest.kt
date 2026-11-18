package com.example

import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.UserPrivate
import com.example.data.repository.CordlyRepository
import com.example.ui.cordly.AppLanguageManager
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
    assertNotEquals(
      AppLanguageManager.stringsFor("en").voiceCallAction,
      AppLanguageManager.stringsFor("fr").voiceCallAction
    )
  }
}
