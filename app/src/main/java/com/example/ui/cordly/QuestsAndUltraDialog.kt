package com.example.ui.cordly

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomQuest
import com.example.data.model.DeveloperTicket
import com.example.data.model.PublicProfile
import com.example.data.model.UserPrivate
import com.example.data.repository.CordlyRepository
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlyRailBg
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextMuted
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun QuestsAndUltraDialog(
    profile: UserPrivate?,
    acceptedFriendsCount: Int,
    ultraDirectoryMembers: List<PublicProfile>,
    customQuests: List<CustomQuest>,
    developerTickets: List<DeveloperTicket>,
    isBusy: Boolean,
    onClaimDailyReward: () -> Unit,
    onClaimChannelReward: (String) -> Unit,
    onCreateCustomQuest: (title: String, desc: String, type: String, target: Long, rewardPerStep: Long) -> Unit,
    onClaimCustomQuest: (CustomQuest) -> Unit,
    onSendDeveloperMessage: (subject: String, message: String) -> Unit,
    onOpenMemberProfile: (PublicProfile) -> Unit,
    onOpenShopForRanks: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    val langCode = LocalSelectedLanguageCode.current
    val isArabic = remember(langCode) {
        AppLanguageManager.resolveEffectiveCode(langCode).startsWith("ar")
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Quests, 1 = Ultra Directory, 2 = Dev Contact
    var channelNameInput by remember { mutableStateOf("") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val currentRank = remember(profile?.rank, profile?.rankExpiresAtMs) {
        CordlyRepository.effectiveRank(profile)
    }
    val neoBonus = remember(profile?.rank, profile?.rankExpiresAtMs) {
        CordlyRepository.neoPlusRewardBonus(profile)
    }

    LaunchedEffect(profile?.lastDailyClaimAtMs) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val dailyStatus = remember(profile?.dailyClaimsCount, profile?.lastDailyClaimAtMs, profile?.createdAt, nowMs) {
        CordlyRepository.computeDailyRewardStatus(profile, nowMs)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = CordlySurfaceDark,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CordlyCyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth(0.95f)
                .widthIn(max = 580.dp)
                .padding(vertical = 14.dp)
                .testTag("quests_and_ultra_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.questsTabTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.peyPoints ?: 200} P E Y" + if (neoBonus > 0) " • +100 Neo+ Bonus Active" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (neoBonus > 0) CordlyAmber else CordlyCyan
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = CordlyTextSecondary)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    QuestModalTab(
                        icon = Icons.Rounded.AddTask,
                        label = strings.questsSubTab,
                        selected = activeTab == 0,
                        testTag = "quests_subtab_quests",
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    QuestModalTab(
                        icon = Icons.Rounded.Diamond,
                        label = strings.ultraDirectoryTab,
                        selected = activeTab == 1,
                        testTag = "quests_subtab_ultra",
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    QuestModalTab(
                        icon = Icons.Rounded.SupportAgent,
                        label = strings.devContactTab,
                        selected = activeTab == 2,
                        testTag = "quests_subtab_dev",
                        onClick = { activeTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CordlyDivider)

                when (activeTab) {
                    0 -> QuestsListTab(
                        profile = profile,
                        currentRank = currentRank,
                        neoBonus = neoBonus,
                        acceptedFriendsCount = acceptedFriendsCount,
                        dailyStatus = dailyStatus,
                        channelNameInput = channelNameInput,
                        onChannelInputChange = { channelNameInput = it },
                        customQuests = customQuests,
                        isBusy = isBusy,
                        isArabic = isArabic,
                        strings = strings,
                        onCopyFireAx9 = {
                            clipboard.setText(AnnotatedString(CordlyRepository.CREATOR_CHANNEL_NAME))
                            channelNameInput = CordlyRepository.CREATOR_CHANNEL_NAME
                            onClaimChannelReward(CordlyRepository.CREATOR_CHANNEL_NAME)
                        },
                        onClaimDailyReward = onClaimDailyReward,
                        onClaimChannelReward = { onClaimChannelReward(channelNameInput) },
                        onCreateCustomQuest = onCreateCustomQuest,
                        onClaimCustomQuest = onClaimCustomQuest
                    )
                    1 -> UltraDirectoryTab(
                        members = ultraDirectoryMembers,
                        isArabic = isArabic,
                        onOpenMemberProfile = onOpenMemberProfile
                    )
                    else -> DeveloperContactTab(
                        currentRank = currentRank,
                        tickets = developerTickets,
                        isBusy = isBusy,
                        isArabic = isArabic,
                        onSendDeveloperMessage = onSendDeveloperMessage,
                        onOpenShopForRanks = onOpenShopForRanks
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestsListTab(
    profile: UserPrivate?,
    currentRank: String,
    neoBonus: Long,
    acceptedFriendsCount: Int,
    dailyStatus: CordlyRepository.DailyRewardStatus,
    channelNameInput: String,
    onChannelInputChange: (String) -> Unit,
    customQuests: List<CustomQuest>,
    isBusy: Boolean,
    isArabic: Boolean,
    strings: AppStrings,
    onCopyFireAx9: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onClaimChannelReward: () -> Unit,
    onCreateCustomQuest: (String, String, String, Long, Long) -> Unit,
    onClaimCustomQuest: (CustomQuest) -> Unit
) {
    var questTitle by remember { mutableStateOf("") }
    var questTargetFriends by remember { mutableStateOf("5") }
    var questRewardPerFriend by remember { mutableStateOf("300") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Daily Check-in Quest
        item {
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (dailyStatus.isClaimable) CordlyEmerald else CordlyDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = CordlyEmerald, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val totalDaily = dailyStatus.nextRewardAmount + neoBonus
                        Text(
                            text = "${strings.dailyRewardTitle} (+$totalDaily P E Y)",
                            style = MaterialTheme.typography.titleSmall,
                            color = CordlyTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        val hrs = dailyStatus.remainingMs / 3_600_000L
                        val mins = (dailyStatus.remainingMs % 3_600_000L) / 60_000L
                        val secs = (dailyStatus.remainingMs % 60_000L) / 1000L
                        Text(
                            text = if (dailyStatus.isClaimable) {
                                if (dailyStatus.claimsCompleted < 2L) strings.dailyRewardFirstSub else strings.dailyRewardAfterSub
                            } else {
                                "${strings.nextClaimIn} ${String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyStatus.isClaimable) CordlyEmerald else CordlyTextSecondary
                        )
                    }
                    Button(
                        onClick = onClaimDailyReward,
                        enabled = dailyStatus.isClaimable && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = CordlyEmerald, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("quest_claim_daily_button")
                    ) {
                        Text("${strings.claimNowButton} +${dailyStatus.nextRewardAmount + neoBonus}")
                    }
                }
            }
        }

        // Channel Copy Quest (fire ax9 -> +200)
        item {
            val alreadyClaimed = profile?.channelRewardClaimed == true
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (alreadyClaimed) CordlyDivider else CordlyCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${strings.channelRewardTitle}" + if (neoBonus > 0) " (+100 Neo+)" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = CordlyTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(strings.channelRewardSubtitle, style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
                    if (alreadyClaimed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.channelRewardClaimedLabel, color = CordlyEmerald, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = onCopyFireAx9,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = CordlyCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.copyChannelNameButton, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = channelNameInput,
                                onValueChange = onChannelInputChange,
                                label = { Text(strings.channelNameInputLabel) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onClaimChannelReward,
                                enabled = channelNameInput.trim().equals(CordlyRepository.CREATOR_CHANNEL_NAME, ignoreCase = true) && !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black)
                            ) {
                                Text(strings.claimNowButton)
                            }
                        }
                    }
                }
            }
        }

        // Neo Plus Custom Quest Builder
        if (currentRank == CordlyRepository.RANK_NEO_PLUS) {
            item {
                Surface(
                    color = CordlyElevatedSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CordlyAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "إنشاء مهمة جديدة (نيو بلس)" else "Create Custom Quest (Neo Plus)",
                                style = MaterialTheme.typography.titleSmall,
                                color = CordlyAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = questTitle,
                            onValueChange = { questTitle = it },
                            label = { Text(if (isArabic) "عنوان المهمة (مثال: أضف 5 أصدقاء)" else "Quest title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = questTargetFriends,
                                onValueChange = { questTargetFriends = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (isArabic) "عدد الأصدقاء" else "Friends target") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = questRewardPerFriend,
                                onValueChange = { questRewardPerFriend = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (isArabic) "نقاط كل صديق" else "P E Y / friend") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                val t = questTargetFriends.toLongOrNull()?.coerceIn(1L, 100L) ?: 5L
                                val r = questRewardPerFriend.toLongOrNull()?.coerceIn(50L, 5000L) ?: 300L
                                val titleClean = questTitle.trim().ifBlank { "Add $t Friends" }
                                onCreateCustomQuest(titleClean, "Add $t friends -> +$r P E Y each", "add_friends", t, r)
                                questTitle = ""
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth().testTag("create_custom_quest_button")
                        ) {
                            Text(if (isArabic) "نشر المهمة" else "Publish Quest", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Community Custom Quests
        if (customQuests.isNotEmpty()) {
            item {
                Text(
                    text = if (isArabic) "مهام المجتمع المخصصة" else "Community Quests",
                    style = MaterialTheme.typography.titleSmall,
                    color = CordlyCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            items(customQuests, key = { it.id }) { q ->
                val isClaimed = profile?.claimedCustomQuests?.contains(q.id) == true
                val progress = if (q.questType == "add_friends") acceptedFriendsCount.toLong() else 1L
                val isComplete = progress >= q.targetCount
                val ratio = (progress.toFloat() / q.targetCount.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val totalWithNeoBonus = q.totalReward + neoBonus

                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isClaimed) CordlyDivider else CordlyBlurple.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(q.title, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("+$totalWithNeoBonus P E Y", color = CordlyAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${q.creatorName} • ${q.description}", style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (isComplete) CordlyEmerald else CordlyBlurple,
                            trackColor = CordlyDivider,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("$progress / ${q.targetCount}", style = MaterialTheme.typography.labelSmall, color = CordlyTextMuted)
                            if (isClaimed) {
                                Text(if (isArabic) "تم الاستلام" else "Claimed", color = CordlyEmerald, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Button(
                                    onClick = { onClaimCustomQuest(q) },
                                    enabled = isComplete && !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = CordlyEmerald, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(strings.claimNowButton)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UltraDirectoryTab(
    members: List<PublicProfile>,
    isArabic: Boolean,
    onOpenMemberProfile: (PublicProfile) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Text(
                text = if (isArabic) "أعضاء ألترا ونيو بلس (أعضاء نيو بلس مثبتون في الأعلى)" else "Ultra & Neo Plus Members (Neo Plus pinned at top)",
                style = MaterialTheme.typography.labelMedium,
                color = CordlyCyan
            )
        }
        if (members.isEmpty()) {
            item {
                EmptyPlaceholder(
                    icon = Icons.Rounded.Diamond,
                    title = if (isArabic) "لا يوجد أعضاء ألترا بعد" else "No Ultra members yet",
                    subtitle = if (isArabic) "فعّل تجربة ألترا أو نيو بلس من المتجر لتظهر هنا!" else "Activate Ultra or Neo Plus in the Shop to appear here!"
                )
            }
        } else {
            items(members, key = { it.userId }) { member ->
                val isNeoPlus = CordlyRepository.effectiveRank(member) == CordlyRepository.RANK_NEO_PLUS
                Surface(
                    color = if (isNeoPlus) CordlyAmber.copy(alpha = 0.14f) else CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(if (isNeoPlus) 1.5.dp else 1.dp, if (isNeoPlus) CordlyAmber else CordlyBlurple.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenMemberProfile(member) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        UserAvatar(
                            displayName = member.displayName,
                            photoUrl = member.photoUrl,
                            status = member.status,
                            frameId = member.equippedFrame,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(member.displayName, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (isNeoPlus) CordlyAmber else CordlyCyan,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isNeoPlus) "NEO+" else "ULTRA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = member.customStatus?.takeIf { it.isNotBlank() } ?: member.friendCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperContactTab(
    currentRank: String,
    tickets: List<DeveloperTicket>,
    isBusy: Boolean,
    isArabic: Boolean,
    onSendDeveloperMessage: (String, String) -> Unit,
    onOpenShopForRanks: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (currentRank != CordlyRepository.RANK_NEO_PLUS) {
            item {
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = CordlyAmber, modifier = Modifier.size(34.dp))
                        Text(
                            text = if (isArabic) "التواصل المباشر مع المطور حصري لرتبة نيو بلس" else "Direct Developer Contact is exclusive to Neo Plus",
                            style = MaterialTheme.typography.titleMedium,
                            color = CordlyTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onOpenShopForRanks,
                            colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "فتح نيو بلس / تجربة مجانية" else "Get Neo Plus / Free Trial", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isArabic) "تواصل مباشرة مع المطور" else "Message the Developer Directly",
                            style = MaterialTheme.typography.titleMedium,
                            color = CordlyAmber,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(if (isArabic) "الموضوع" else "Subject") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text(if (isArabic) "اكتب رسالتك أو اقتراحك..." else "Write your message or request...") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSendDeveloperMessage(subject.ifBlank { "Neo+ Feedback" }, body)
                                subject = ""
                                body = ""
                            },
                            enabled = body.isNotBlank() && !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "إرسال للمطور" else "Send to Developer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (tickets.isNotEmpty()) {
                item {
                    Text(if (isArabic) "رسائلك المرسلة" else "Your Sent Tickets", style = MaterialTheme.typography.titleSmall, color = CordlyCyan)
                }
                items(tickets, key = { it.id }) { t ->
                    Surface(
                        color = CordlyCardSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CordlyDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(t.subject, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary, fontWeight = FontWeight.Bold)
                                Text(t.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = CordlyEmerald)
                            }
                            Text(t.message, style = MaterialTheme.typography.bodySmall, color = CordlyTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestModalTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) CordlyBlurple else CordlyCardSurface,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, CordlyDivider),
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else CordlyTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else CordlyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
