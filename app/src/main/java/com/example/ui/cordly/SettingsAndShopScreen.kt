package com.example.ui.cordly

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.repository.CordlyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.UserPrivate
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlyThemeState
import com.example.ui.theme.LocalAppAccentColor
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.LocalOnAppAccentColorSelected
import com.example.ui.theme.LocalOnAppThemeModeSelected
import com.example.ui.theme.CordlyRailBg
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsAndShopDialog(
    initialTab: Int,
    profile: UserPrivate?,
    wallpaperState: WallpaperState,
    isBusy: Boolean,
    onSelectWallpaperMode: (AppWallpaperMode) -> Unit,
    onCustomWallpaperSaved: () -> Unit,
    onSaveProfile: (
        status: String,
        customStatus: String?,
        displayName: String?,
        photoUrl: String?,
        bannerUrl: String?,
        clearBanner: Boolean
    ) -> Unit,
    onQuickUpdateMedia: (
        photoUrl: String?,
        bannerUrl: String?,
        clearBanner: Boolean,
        label: String
    ) -> Unit,
    onClaimDailyReward: () -> Unit,
    onClaimChannelReward: (String) -> Unit,
    onBuyFrame: (ProfileFrameItem) -> Unit,
    onEquipFrame: (String) -> Unit,
    onBuyProfileEffect: (ProfileVideoEffectItem, String?, Boolean) -> Unit,
    onEquipProfileEffect: (String, String?, Boolean) -> Unit,
    onPreviewOwnProfile: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var displayName by remember(profile?.userId) { mutableStateOf(profile?.displayName.orEmpty()) }
    var customStatus by remember(profile?.userId) { mutableStateOf(profile?.customStatus.orEmpty()) }
    var selectedStatus by remember(profile?.userId) { mutableStateOf(profile?.status ?: "online") }
    var pendingPhotoUrl by remember(profile?.userId) { mutableStateOf(profile?.photoUrl) }
    var pendingBannerUrl by remember(profile?.userId) { mutableStateOf(profile?.bannerUrl) }
    var clearBannerRequested by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageError = null
            isProcessingImage = true
            scope.launch {
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(
                    context = context,
                    uri = uri,
                    maxWidth = 144,
                    maxHeight = 144,
                    maxBase64Bytes = 40_000
                )
                isProcessingImage = false
                if (encoded != null) {
                    pendingPhotoUrl = encoded
                    onQuickUpdateMedia(encoded, null, false, "Profile photo")
                } else {
                    imageError = "Could not load selected profile photo. Try another image."
                }
            }
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageError = null
            isProcessingImage = true
            scope.launch {
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(
                    context = context,
                    uri = uri,
                    maxWidth = 400,
                    maxHeight = 160,
                    maxBase64Bytes = 55_000
                )
                isProcessingImage = false
                if (encoded != null) {
                    pendingBannerUrl = encoded
                    clearBannerRequested = false
                    onQuickUpdateMedia(null, encoded, false, "Profile banner")
                } else {
                    imageError = "Could not load selected banner image. Try another image."
                }
            }
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageError = null
            isProcessingImage = true
            scope.launch {
                val dest = AppAppearanceStore.customWallpaperFile(context)
                val saved = ImageEncodingHelper.savePickedWallpaperToInternalStorage(context, uri, dest)
                isProcessingImage = false
                if (saved) {
                    onCustomWallpaperSaved()
                } else {
                    imageError = "Could not load selected wallpaper photo. Try another image."
                }
            }
        }
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
            border = BorderStroke(1.dp, CordlyDivider),
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Modal Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary
                        )
                        Text(
                            text = "${profile?.peyPoints ?: 200} ${strings.peyAvailableSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = CordlyTextSecondary)
                    }
                }

                // Sub-Tabs: Profile & Theme | P E Y Shop | World Languages
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    ModalTabButton(
                        icon = Icons.Rounded.Palette,
                        label = strings.tabProfileTheme,
                        selected = selectedTab == 0,
                        testTag = "settings_profile_tab",
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ModalTabButton(
                        icon = Icons.Rounded.ShoppingBag,
                        label = strings.tabPeyShop,
                        selected = selectedTab == 1,
                        testTag = "settings_shop_tab",
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    ModalTabButton(
                        icon = Icons.Rounded.Language,
                        label = strings.tabLanguages,
                        selected = selectedTab == 2,
                        testTag = "settings_language_tab",
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CordlyDivider)

                when (selectedTab) {
                    0 -> SettingsProfileTab(
                        profile = profile,
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it },
                        customStatus = customStatus,
                        onCustomStatusChange = { customStatus = it },
                        selectedStatus = selectedStatus,
                        onStatusSelect = { selectedStatus = it },
                        pendingPhotoUrl = pendingPhotoUrl,
                        pendingBannerUrl = pendingBannerUrl,
                        wallpaperState = wallpaperState,
                        isBusy = isBusy,
                        isProcessingImage = isProcessingImage,
                        imageError = imageError,
                        onPickAvatar = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickBanner = {
                            bannerPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearBanner = {
                            pendingBannerUrl = null
                            clearBannerRequested = true
                            onQuickUpdateMedia(null, null, true, "Profile banner")
                        },
                        onSelectPresetBanner = { preset ->
                            pendingBannerUrl = preset.bannerSource
                            clearBannerRequested = false
                            onQuickUpdateMedia(null, preset.bannerSource, false, "Profile banner")
                        },
                        onSelectWallpaperMode = onSelectWallpaperMode,
                        onPickCustomWallpaper = {
                            wallpaperPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSave = {
                            onSaveProfile(
                                selectedStatus,
                                customStatus,
                                displayName,
                                pendingPhotoUrl,
                                pendingBannerUrl,
                                clearBannerRequested
                            )
                        },
                        onPreviewOwnProfile = onPreviewOwnProfile,
                        onSignOut = {
                            onDismiss()
                            onSignOut()
                        }
                    )
                    1 -> PeyShopTab(
                        profile = profile,
                        isBusy = isBusy,
                        onClaimDailyReward = onClaimDailyReward,
                        onClaimChannelReward = onClaimChannelReward,
                        onBuyFrame = onBuyFrame,
                        onEquipFrame = onEquipFrame,
                        onBuyProfileEffect = onBuyProfileEffect,
                        onEquipProfileEffect = onEquipProfileEffect,
                        onPreviewOwnProfile = onPreviewOwnProfile
                    )
                    else -> WorldLanguagesTab()
                }
            }
        }
    }
}

@Composable
private fun ModalTabButton(
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
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else CordlyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else CordlyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsProfileTab(
    profile: UserPrivate?,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    customStatus: String,
    onCustomStatusChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelect: (String) -> Unit,
    pendingPhotoUrl: String?,
    pendingBannerUrl: String?,
    wallpaperState: WallpaperState,
    isBusy: Boolean,
    isProcessingImage: Boolean,
    imageError: String?,
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
    onClearBanner: () -> Unit,
    onSelectPresetBanner: (ProfileBannerPreset) -> Unit,
    onSelectWallpaperMode: (AppWallpaperMode) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onSave: () -> Unit,
    onPreviewOwnProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val statuses = listOf(
        "online" to CordlyEmerald,
        "idle" to CordlyAmber,
        "dnd" to CordlyCrimson
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isProcessingImage) {
            LinearProgressIndicator(
                color = CordlyCyan,
                trackColor = CordlyDivider,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (imageError != null) {
            Surface(
                color = CordlyCrimson.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, CordlyCrimson.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = imageError,
                    style = MaterialTheme.typography.labelMedium,
                    color = CordlyCrimson,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Discord-style Profile Preview Card (Banner + Avatar + Frame)
        Surface(
            color = CordlyCardSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CordlyDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(CordlyBlurple, Color(0xFF9333EA), CordlyCyan)
                            )
                        )
                ) {
                    if (!pendingBannerUrl.isNullOrBlank()) {
                        CordlyRemoteOrDataImage(
                            source = pendingBannerUrl,
                            contentDescription = "Profile background banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.58f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onPickBanner)
                                .testTag("pick_profile_banner_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    contentDescription = "Change profile banner",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Banner",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        if (pendingBannerUrl != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.58f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearBanner)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove banner",
                                        tint = CordlyCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Avatar row + Discord Profile Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onPickAvatar)
                            .testTag("pick_profile_avatar_button")
                    ) {
                        UserAvatar(
                            displayName = displayName.ifBlank { profile?.displayName ?: "User" },
                            photoUrl = pendingPhotoUrl,
                            status = selectedStatus,
                            frameId = profile?.equippedFrame ?: "none",
                            size = 64.dp
                        )
                        Surface(
                            color = CordlyBlurple,
                            shape = CircleShape,
                            border = BorderStroke(2.dp, CordlyCardSurface),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    contentDescription = "Change account photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName.ifBlank { profile?.displayName ?: "neov user" },
                            style = MaterialTheme.typography.titleMedium,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Friend Code: ${profile?.friendCode ?: "..."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyCyan
                        )
                        Text(
                            text = profile?.email ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val strings = LocalAppStrings.current
        val currentThemeMode = LocalAppThemeMode.current
        val onThemeModeSelected = LocalOnAppThemeModeSelected.current
        val currentAccentColor = LocalAppAccentColor.current
        val onAccentColorSelected = LocalOnAppAccentColorSelected.current
        val langCode = LocalSelectedLanguageCode.current
        val isArabicLocale = remember(langCode) {
            AppLanguageManager.resolveEffectiveCode(langCode).startsWith("ar")
        }

        // Preset Banner Styles & Shapes Selector (أشكال وخلفيات البانر)
        Text(
            text = strings.bannerStylesTitle,
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary
        )
        Text(
            text = strings.bannerStylesSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = CordlyTextSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            ProfileBannerCatalog.presets.forEach { preset ->
                val isSelected = pendingBannerUrl == preset.bannerSource
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) CordlyCyan else CordlyDivider
                    ),
                    modifier = Modifier
                        .width(116.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPresetBanner(preset) }
                        .testTag("banner_preset_${preset.id}")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PresetBannerCanvas(
                            preset = preset,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = CordlyCyan,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.62f))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isArabicLocale) preset.titleAr else preset.titleEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Night Mode & Day Mode Switcher
        Text(
            text = strings.appearanceModeTitle,
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary
        )
        Text(
            text = strings.appearanceModeSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = CordlyTextSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeModeOptionCard(
                label = strings.nightModeLabel,
                icon = Icons.Rounded.DarkMode,
                previewBg = Color(0xFF000000),
                previewBorder = Color(0xFF232330),
                previewContentTint = Color(0xFFF7F8FA),
                selected = currentThemeMode == AppThemeMode.NIGHT,
                testTag = "theme_mode_night",
                onClick = { onThemeModeSelected(AppThemeMode.NIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeOptionCard(
                label = strings.dayModeLabel,
                icon = Icons.Rounded.LightMode,
                previewBg = Color(0xFFFFFFFF),
                previewBorder = Color(0xFFCBD5E1),
                previewContentTint = Color(0xFFF59E0B),
                selected = currentThemeMode == AppThemeMode.DAY,
                testTag = "theme_mode_day",
                onClick = { onThemeModeSelected(AppThemeMode.DAY) },
                modifier = Modifier.weight(1f)
            )
        }

        // App Color Picker (تغيير لون التطبيق)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.appColorTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary
                )
                Text(
                    text = strings.appColorSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyTextSecondary
                )
            }
            Surface(
                color = currentAccentColor.primary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, currentAccentColor.primary.copy(alpha = 0.55f))
            ) {
                Text(
                    text = if (isArabicLocale) currentAccentColor.labelAr else currentAccentColor.labelEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = currentAccentColor.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp)
        ) {
            AppAccentColor.entries.forEach { accent ->
                val isSelected = accent == currentAccentColor
                val label = if (isArabicLocale) accent.labelAr else accent.labelEn
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onAccentColorSelected(accent) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("app_color_${accent.storageKey}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accent.primary.copy(alpha = 0.22f)
                                else CordlyCardSurface
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) accent.primary else CordlyDivider,
                                shape = CircleShape
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(accent.primary, accent.darkSecondary)
                                    )
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) CordlyTextPrimary else CordlyTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // App Background Wallpaper Selector (Pure Black + 3 Provided Wallpapers + Custom Photo)
        Text(
            text = strings.backgroundTitle,
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary
        )
        Text(
            text = strings.backgroundSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = CordlyTextSecondary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                AppWallpaperMode.DEFAULT,
                AppWallpaperMode.PRESET_1,
                AppWallpaperMode.PRESET_2,
                AppWallpaperMode.PRESET_3,
                AppWallpaperMode.SHAPE_HEX,
                AppWallpaperMode.SHAPE_AURORA,
                AppWallpaperMode.SHAPE_WAVES,
                AppWallpaperMode.SHAPE_GALAXY
            ).forEach { mode ->
                WallpaperSwatchCard(
                    mode = mode,
                    selected = wallpaperState.mode == mode,
                    customFilePath = null,
                    customVersion = 0L,
                    onClick = { onSelectWallpaperMode(mode) }
                )
            }

            // Custom wallpaper picker swatch
            val isCustomActive = wallpaperState.mode == AppWallpaperMode.CUSTOM
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isCustomActive) 2.dp else 1.dp,
                    color = if (isCustomActive) CordlyCyan else CordlyDivider
                ),
                modifier = Modifier
                    .width(96.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPickCustomWallpaper)
                    .testTag("wallpaper_custom_picker")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val customWallpaperBitmap = rememberFileImageBitmap(
                        filePath = wallpaperState.customFilePath,
                        version = wallpaperState.customVersion
                    )
                    if (customWallpaperBitmap != null) {
                        Image(
                            bitmap = customWallpaperBitmap,
                            contentDescription = strings.yourPhoto,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = strings.yourPhoto,
                            tint = CordlyCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.yourPhoto,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Editable Profile Fields
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text(strings.displayNameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = customStatus,
            onValueChange = onCustomStatusChange,
            label = { Text(strings.customStatusLabel) },
            placeholder = { Text(strings.customStatusPlaceholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            statuses.forEach { (key, color) ->
                val active = key == selectedStatus
                Surface(
                    color = if (active) color.copy(alpha = 0.22f) else CordlyCardSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) color else CordlyDivider),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStatusSelect(key) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (key) {
                                "online" -> strings.statusOnline
                                "idle" -> strings.statusIdle
                                "dnd" -> strings.statusDnd
                                else -> strings.statusOffline
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_settings_button")
        ) {
            Text(strings.saveProfileButton)
        }

        OutlinedButton(
            onClick = onPreviewOwnProfile,
            border = BorderStroke(1.dp, CordlyCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("preview_own_profile_button")
        ) {
            Icon(Icons.Rounded.PlayCircleFilled, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.previewProfileButton, color = CordlyCyan)
        }

        OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.dp, CordlyCrimson.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CordlyCrimson, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.signOutButton, color = CordlyCrimson)
        }
    }
}

@Composable
private fun ThemeModeOptionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    previewBg: Color,
    previewBorder: Color,
    previewContentTint: Color,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) CordlyBlurple.copy(alpha = 0.16f) else CordlyCardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) CordlyBlurple else CordlyDivider
        ),
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Surface(
                color = previewBg,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, previewBorder),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = previewContentTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = CordlyTextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = CordlyBlurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WallpaperSwatchCard(
    mode: AppWallpaperMode,
    selected: Boolean,
    customFilePath: String?,
    customVersion: Long,
    onClick: () -> Unit
) {
    val presetRes = mode.drawableRes()
    Surface(
        color = CordlyCanvasBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) CordlyCyan else CordlyDivider
        ),
        modifier = Modifier
            .width(96.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("wallpaper_swatch_${mode.storageKey}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                mode.isShapeWallpaper() -> {
                    WallpaperShapeCanvas(
                        mode = mode,
                        isDark = CordlyThemeState.isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                presetRes != null -> {
                    AsyncImage(
                        model = presetRes,
                        contentDescription = mode.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(CordlyRailBg, CordlyCanvasBg))
                            )
                    )
                }
            }

            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = CordlyCyan,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (mode == AppWallpaperMode.DEFAULT && !CordlyThemeState.isDark) {
                        "Clean Day"
                    } else {
                        mode.label
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PeyShopTab(
    profile: UserPrivate?,
    isBusy: Boolean,
    onClaimDailyReward: () -> Unit,
    onClaimChannelReward: (String) -> Unit,
    onBuyFrame: (ProfileFrameItem) -> Unit,
    onEquipFrame: (String) -> Unit,
    onBuyProfileEffect: (ProfileVideoEffectItem, String?, Boolean) -> Unit,
    onEquipProfileEffect: (String, String?, Boolean) -> Unit,
    onPreviewOwnProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val strings = LocalAppStrings.current

    var shopSection by remember { mutableIntStateOf(0) } // 0 = Profile Videos, 1 = Animated Frames
    var processingVideoError by remember { mutableStateOf<String?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }
    var pendingUploadEffectId by remember { mutableStateOf("custom_video") }
    var channelNameInput by remember { mutableStateOf("") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(profile?.lastDailyClaimAtMs, profile?.createdAt) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val dailyStatus = remember(profile?.dailyClaimsCount, profile?.lastDailyClaimAtMs, profile?.createdAt, nowMs) {
        CordlyRepository.computeDailyRewardStatus(profile, nowMs)
    }

    val balance = profile?.peyPoints ?: 200L
    val ownedFrames = profile?.ownedFrames?.ifEmpty { listOf("none") } ?: listOf("none")
    val equippedFrame = profile?.equippedFrame ?: "none"

    val ownedEffects = profile?.ownedProfileEffects?.ifEmpty { listOf("none") } ?: listOf("none")
    val equippedEffect = profile?.equippedProfileEffect ?: "none"
    val uid = profile?.userId ?: ""

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetEffectId = pendingUploadEffectId
        if (uri != null && uid.isNotBlank()) {
            processingVideoError = null
            isProcessingVideo = true
            scope.launch {
                val targetEffect = PeyShopCatalog.findEffectById(targetEffectId)
                val processedResult = if (targetEffect.isUnlimitedDuration) {
                    VideoEffectHelper.processPickedFullVideo(
                        context = context,
                        sourceUri = uri,
                        uid = uid
                    )
                } else {
                    VideoEffectHelper.processPickedVideoTo5Seconds(
                        context = context,
                        sourceUri = uri,
                        uid = uid
                    )
                }
                isProcessingVideo = false
                processedResult
                    .onSuccess { processed ->
                        val alreadyOwned = ownedEffects.contains(targetEffect.id)
                        if (alreadyOwned) {
                            onEquipProfileEffect(targetEffect.id, processed.dataUri, !processed.cloudSynced)
                        } else {
                            onBuyProfileEffect(targetEffect, processed.dataUri, !processed.cloudSynced)
                        }
                    }
                    .onFailure { err ->
                        processingVideoError = err.localizedMessage ?: "Failed to load selected video."
                    }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Surface(
                color = CordlyElevatedSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(CordlyAmber.copy(alpha = 0.2f), CordlyBlurple.copy(alpha = 0.16f))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Icon(
                        Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = CordlyAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$balance P E Y",
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.shopRewardsTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onPreviewOwnProfile,
                        border = BorderStroke(1.dp, CordlyCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayCircleFilled,
                            contentDescription = null,
                            tint = CordlyCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", color = CordlyCyan, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Daily P E Y Reward Card (+100 P E Y first 2 days, then +50 P E Y every day)
        item {
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (dailyStatus.isClaimable) CordlyEmerald else CordlyDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Surface(
                        color = CordlyEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = CordlyEmerald, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.dailyRewardTitle} (+${dailyStatus.nextRewardAmount} P E Y)",
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onClaimDailyReward,
                        enabled = dailyStatus.isClaimable && !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dailyStatus.isClaimable) CordlyEmerald else CordlyElevatedSurface,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("claim_daily_reward_button")
                    ) {
                        Text("${strings.claimNowButton} +${dailyStatus.nextRewardAmount}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Creator Channel Name Copy Bonus Card (fire ax9 -> +200 P E Y)
        item {
            val alreadyClaimed = profile?.channelRewardClaimed == true
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (alreadyClaimed) CordlyDivider else CordlyCyan.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CordlyCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.channelRewardTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = CordlyTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.channelRewardSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextSecondary
                            )
                        }
                    }

                    if (alreadyClaimed) {
                        Surface(
                            color = CordlyEmerald.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CordlyEmerald)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.channelRewardClaimedLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CordlyEmerald
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(CordlyRepository.CREATOR_CHANNEL_NAME))
                                channelNameInput = CordlyRepository.CREATOR_CHANNEL_NAME
                                onClaimChannelReward(CordlyRepository.CREATOR_CHANNEL_NAME)
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = CordlyCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_fire_ax9_reward_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.copyChannelNameButton, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = channelNameInput,
                                onValueChange = { channelNameInput = it },
                                label = { Text(strings.channelNameInputLabel) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onClaimChannelReward(channelNameInput) },
                                enabled = channelNameInput.trim().equals(CordlyRepository.CREATOR_CHANNEL_NAME, ignoreCase = true) && !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("claim_channel_reward_button")
                            ) {
                                Text(strings.claimNowButton)
                            }
                        }
                    }
                }
            }
        }

        // Shop Category Switcher: 5s Video Intros vs Animated Avatar Frames
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModalTabButton(
                    icon = Icons.Rounded.VideoLibrary,
                    label = strings.shopVideosTab,
                    selected = shopSection == 0,
                    testTag = "shop_videos_tab",
                    onClick = { shopSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                ModalTabButton(
                    icon = Icons.Rounded.Stars,
                    label = strings.shopFramesTab,
                    selected = shopSection == 1,
                    testTag = "shop_frames_tab",
                    onClick = { shopSection = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (processingVideoError != null) {
            item {
                Surface(
                    color = CordlyCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, CordlyCrimson.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = processingVideoError.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CordlyCrimson,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (shopSection == 0) {
            item {
                Text(
                    text = "Discord-style profile video animations: choose 5s clips (from 100 P E Y) or unlock Unlimited Full Video (1000 P E Y) with no time limit!",
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyTextSecondary
                )
            }

            items(PeyShopCatalog.profileEffects, key = { "eff_${it.id}" }) { effect ->
                val isOwned = effect.id == "none" || ownedEffects.contains(effect.id)
                val isEquipped = equippedEffect == effect.id
                val canAfford = balance >= effect.price
                val customFile = remember(uid, effect.id, profile?.profileVideoData) {
                    when {
                        uid.isBlank() -> null
                        effect.isUnlimitedDuration -> AppAppearanceStore.customProfileUnlimitedVideoFile(context, uid)
                        effect.isCustomUpload -> AppAppearanceStore.customProfileVideoFile(context, uid)
                        else -> null
                    }
                }
                val hasCustomFile = effect.isCustomUpload &&
                    ((customFile != null && customFile.exists()) || !profile?.profileVideoData.isNullOrBlank())

                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isEquipped) 1.5.dp else 1.dp,
                        color = if (isEquipped) CordlyCyan else if (effect.isUnlimitedDuration) CordlyAmber.copy(alpha = 0.65f) else CordlyDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Lightweight preview box for video animation (full MP4 plays smoothly in DiscordProfileSheetDialog)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CordlyDivider),
                            modifier = Modifier
                                .width(56.dp)
                                .height(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onPreviewOwnProfile)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(effect.accentColors))
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (effect.isCustomUpload) Icons.Rounded.VideoLibrary else Icons.Rounded.PlayCircleFilled,
                                            contentDescription = "Preview video effect",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                if (effect.isUnlimitedDuration) {
                                    Surface(
                                        color = CordlyAmber,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "∞ FULL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effect.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = CordlyTextPrimary
                            )
                            Text(
                                text = effect.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextSecondary
                            )
                            if (!isOwned) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${effect.price} P E Y",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CordlyAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (effect.isCustomUpload) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val pickButtonTag = if (effect.isUnlimitedDuration) {
                                    "pick_custom_video_unlimited_button"
                                } else {
                                    "pick_custom_video_effect_button"
                                }
                                Button(
                                    onClick = {
                                        pendingUploadEffectId = effect.id
                                        videoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    enabled = !isBusy && !isProcessingVideo && (isOwned || canAfford),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isOwned) CordlyCyan else if (canAfford) CordlyAmber else CordlyElevatedSurface,
                                        contentColor = Color(0xFF0A101B)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag(pickButtonTag)
                                ) {
                                    Text(
                                        text = when {
                                            isProcessingVideo && pendingUploadEffectId == effect.id ->
                                                if (effect.isUnlimitedDuration) "..." else "5s..."
                                            isOwned -> strings.pickVideoButton
                                            else -> strings.unlockButton
                                        }
                                    )
                                }
                                if (hasCustomFile && !isEquipped && isOwned) {
                                    OutlinedButton(
                                        onClick = { onEquipProfileEffect(effect.id, null, false) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(strings.equipButton, color = CordlyTextPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        } else {
                            when {
                                isEquipped -> {
                                    Surface(
                                        color = CordlyCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CordlyCyan)
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CordlyCyan,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                isOwned -> {
                                    OutlinedButton(
                                        onClick = { onEquipProfileEffect(effect.id, null, false) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CordlyBlurple)
                                    ) {
                                        Text("Equip", color = CordlyTextPrimary)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { onBuyProfileEffect(effect, null, false) },
                                        enabled = canAfford && !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canAfford) CordlyAmber else CordlyElevatedSurface,
                                            contentColor = if (canAfford) Color(0xFF1A1200) else CordlyTextSecondary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("buy_effect_${effect.id}")
                                    ) {
                                        Text("Buy")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(PeyShopCatalog.frames, key = { it.id }) { frame ->
                val isOwned = frame.id == "none" || ownedFrames.contains(frame.id)
                val isEquipped = equippedFrame == frame.id
                val canAfford = balance >= frame.price

                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isEquipped) 1.5.dp else 1.dp,
                        color = if (isEquipped) CordlyEmerald else CordlyDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        UserAvatar(
                            displayName = profile?.displayName ?: "You",
                            photoUrl = profile?.photoUrl,
                            frameId = frame.id,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = frame.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = CordlyTextPrimary
                            )
                            Text(
                                text = frame.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextSecondary
                            )
                            if (!isOwned) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${frame.price} P E Y",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CordlyAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        when {
                            isEquipped -> {
                                Surface(
                                    color = CordlyEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CordlyEmerald)
                                ) {
                                    Text(
                                        text = "Equipped",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CordlyEmerald,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            isOwned -> {
                                OutlinedButton(
                                    onClick = { onEquipFrame(frame.id) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CordlyBlurple)
                                ) {
                                    Text("Equip", color = CordlyTextPrimary)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onBuyFrame(frame) },
                                    enabled = canAfford && !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) CordlyAmber else CordlyElevatedSurface,
                                        contentColor = if (canAfford) Color(0xFF1A1200) else CordlyTextSecondary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("buy_frame_${frame.id}")
                                ) {
                                    Text(strings.buyButton)
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
fun WorldLanguagesTab() {
    val strings = LocalAppStrings.current
    val selectedCode = LocalSelectedLanguageCode.current
    val onLanguageSelected = LocalOnLanguageSelected.current

    var query by remember { mutableStateOf("") }
    val allLangs = remember { AppLanguageManager.allWorldLanguages }
    val filteredLangs = remember(query, allLangs) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            allLangs
        } else {
            allLangs.filter {
                it.nativeName.lowercase().contains(q) ||
                    it.englishName.lowercase().contains(q) ||
                    it.code.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "${strings.languageTitle} (${allLangs.size} ${strings.languagesCountSuffix})",
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.languageSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = CordlyTextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.languageSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CordlyCyan) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("world_language_search_input")
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            if (query.isBlank()) {
                item(key = "lang_system") {
                    LanguageOptionRow(
                        nativeName = strings.deviceDefaultLanguage,
                        englishName = "Auto System Locale",
                        code = "system",
                        isRtl = false,
                        selected = selectedCode == "system",
                        onClick = { onLanguageSelected("system") }
                    )
                }
            }
            items(filteredLangs, key = { "lang_${it.code}" }) { lang ->
                LanguageOptionRow(
                    nativeName = lang.nativeName,
                    englishName = lang.englishName,
                    code = lang.code,
                    isRtl = lang.isRtl,
                    selected = selectedCode.equals(lang.code, ignoreCase = true),
                    onClick = { onLanguageSelected(lang.code) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    nativeName: String,
    englishName: String,
    code: String,
    isRtl: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) CordlyBlurple.copy(alpha = 0.22f) else CordlyCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) CordlyCyan else CordlyDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("language_option_$code")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.titleSmall,
                    color = CordlyTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$englishName • ${code.uppercase()}" + if (isRtl) " • RTL" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = CordlyCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WorldLanguagePickerDialog(
    selectedCode: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    val allLangs = remember { AppLanguageManager.allWorldLanguages }
    val filteredLangs = remember(query, allLangs) {
        val q = query.trim().lowercase()
        if (q.isBlank()) allLangs else allLangs.filter {
            it.nativeName.lowercase().contains(q) ||
                it.englishName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        title = {
            Text(
                text = "${strings.languageTitle} (${allLangs.size})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.languageSearchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CordlyCyan) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            LanguageOptionRow(
                                nativeName = strings.deviceDefaultLanguage,
                                englishName = "Auto",
                                code = "system",
                                isRtl = false,
                                selected = selectedCode == "system",
                                onClick = { onSelectLanguage("system") }
                            )
                        }
                    }
                    items(filteredLangs, key = { "dlg_${it.code}" }) { lang ->
                        LanguageOptionRow(
                            nativeName = lang.nativeName,
                            englishName = lang.englishName,
                            code = lang.code,
                            isRtl = lang.isRtl,
                            selected = selectedCode.equals(lang.code, ignoreCase = true),
                            onClick = { onSelectLanguage(lang.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeButton)
            }
        }
    )
}
