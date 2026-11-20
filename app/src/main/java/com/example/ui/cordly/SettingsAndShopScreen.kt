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
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
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
import com.example.data.repository.CordlyRepository
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlyRailBg
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import com.example.ui.theme.CordlyThemeState
import com.example.ui.theme.LocalAppAccentColor
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.LocalOnAppAccentColorSelected
import com.example.ui.theme.LocalOnAppThemeModeSelected
import kotlinx.coroutines.launch

/**
 * Clean Settings & World Languages Dialog — completely isolated from P E Y Shop.
 */
@Composable
fun SettingsAndShopDialog(
    initialTab: Int,
    profile: UserPrivate?,
    wallpaperState: WallpaperState,
    layoutPrefs: AppUiLayoutPrefs,
    currentLogoVariant: AppLogoVariant,
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
    onUpdateProfileThemeColor: (String?) -> Unit,
    onUpdateAvatarVideo: (String?) -> Unit,
    onUpdateAppLogo: (AppLogoVariant) -> Unit,
    onUpdateLayoutPrefs: (AppUiLayoutPrefs) -> Unit,
    onPreviewOwnProfile: () -> Unit,
    onOpenShopForRanks: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var displayName by remember(profile?.userId) { mutableStateOf(profile?.displayName.orEmpty()) }
    var customStatus by remember(profile?.userId) { mutableStateOf(profile?.customStatus.orEmpty()) }
    var selectedStatus by remember(profile?.userId) { mutableStateOf(profile?.status ?: "online") }
    var pendingPhotoUrl by remember(profile?.userId) { mutableStateOf(profile?.photoUrl) }
    var pendingBannerUrl by remember(profile?.userId) { mutableStateOf(profile?.bannerUrl) }
    var clearBannerRequested by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val uid = profile?.userId.orEmpty()
    val currentRank = remember(profile?.rank, profile?.rankExpiresAtMs) {
        CordlyRepository.effectiveRank(profile)
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageError = null
            isProcessingImage = true
            scope.launch {
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(context, uri, 144, 144, 40_000)
                isProcessingImage = false
                if (encoded != null) {
                    pendingPhotoUrl = encoded
                    onQuickUpdateMedia(encoded, null, false, "Profile photo")
                } else {
                    imageError = "Could not load selected profile photo."
                }
            }
        }
    }

    val avatarVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && uid.isNotBlank()) {
            imageError = null
            isProcessingImage = true
            scope.launch {
                VideoEffectHelper.processPickedVideoTo5Seconds(context, uri, uid)
                    .onSuccess { processed ->
                        isProcessingImage = false
                        onUpdateAvatarVideo(processed.dataUri)
                    }
                    .onFailure {
                        isProcessingImage = false
                        imageError = "Could not process avatar video."
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
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(context, uri, 400, 160, 55_000)
                isProcessingImage = false
                if (encoded != null) {
                    pendingBannerUrl = encoded
                    clearBannerRequested = false
                    onQuickUpdateMedia(null, encoded, false, "Profile banner")
                } else {
                    imageError = "Could not load selected banner image."
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
                if (saved) onCustomWallpaperSaved() else imageError = "Could not load selected wallpaper photo."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge, color = CordlyTextPrimary)
                        Text(
                            text = "${profile?.peyPoints ?: 200} ${strings.peyAvailableSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = CordlyTextSecondary)
                    }
                }

                // Isolated Settings Tabs: Profile & Appearance | World Languages (NO Shop here!)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        icon = Icons.Rounded.Language,
                        label = strings.tabLanguages,
                        selected = selectedTab == 1,
                        testTag = "settings_language_tab",
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CordlyDivider)

                if (selectedTab == 0) {
                    SettingsProfileTab(
                        profile = profile,
                        currentRank = currentRank,
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it },
                        customStatus = customStatus,
                        onCustomStatusChange = { customStatus = it },
                        selectedStatus = selectedStatus,
                        onStatusSelect = { selectedStatus = it },
                        pendingPhotoUrl = pendingPhotoUrl,
                        pendingBannerUrl = pendingBannerUrl,
                        wallpaperState = wallpaperState,
                        layoutPrefs = layoutPrefs,
                        currentLogoVariant = currentLogoVariant,
                        isBusy = isBusy,
                        isProcessingImage = isProcessingImage,
                        imageError = imageError,
                        onPickAvatar = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickAvatarVideo = {
                            avatarVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onPickBanner = {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onClearBanner = {
                            pendingBannerUrl = null
                            clearBannerRequested = true
                            onQuickUpdateMedia(null, null, true, "Profile banner")
                        },
                        onSelectPresetBanner = { preset ->
                            if (hasRequiredRank(currentRank, preset.requiredRank)) {
                                pendingBannerUrl = preset.bannerSource
                                clearBannerRequested = false
                                onQuickUpdateMedia(null, preset.bannerSource, false, "Profile banner")
                            } else {
                                onOpenShopForRanks()
                            }
                        },
                        onSelectWallpaperMode = onSelectWallpaperMode,
                        onPickCustomWallpaper = {
                            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onUpdateProfileThemeColor = onUpdateProfileThemeColor,
                        onUpdateAppLogo = onUpdateAppLogo,
                        onUpdateLayoutPrefs = onUpdateLayoutPrefs,
                        onOpenShopForRanks = onOpenShopForRanks,
                        onSave = {
                            onSaveProfile(selectedStatus, customStatus, displayName, pendingPhotoUrl, pendingBannerUrl, clearBannerRequested)
                        },
                        onPreviewOwnProfile = onPreviewOwnProfile,
                        onSignOut = {
                            onDismiss()
                            onSignOut()
                        }
                    )
                } else {
                    WorldLanguagesTab()
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
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else CordlyTextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else CordlyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsProfileTab(
    profile: UserPrivate?,
    currentRank: String,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    customStatus: String,
    onCustomStatusChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelect: (String) -> Unit,
    pendingPhotoUrl: String?,
    pendingBannerUrl: String?,
    wallpaperState: WallpaperState,
    layoutPrefs: AppUiLayoutPrefs,
    currentLogoVariant: AppLogoVariant,
    isBusy: Boolean,
    isProcessingImage: Boolean,
    imageError: String?,
    onPickAvatar: () -> Unit,
    onPickAvatarVideo: () -> Unit,
    onPickBanner: () -> Unit,
    onClearBanner: () -> Unit,
    onSelectPresetBanner: (ProfileBannerPreset) -> Unit,
    onSelectWallpaperMode: (AppWallpaperMode) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onUpdateProfileThemeColor: (String?) -> Unit,
    onUpdateAppLogo: (AppLogoVariant) -> Unit,
    onUpdateLayoutPrefs: (AppUiLayoutPrefs) -> Unit,
    onOpenShopForRanks: () -> Unit,
    onSave: () -> Unit,
    onPreviewOwnProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentThemeMode = LocalAppThemeMode.current
    val onThemeModeSelected = LocalOnAppThemeModeSelected.current
    val currentAccentColor = LocalAppAccentColor.current
    val onAccentColorSelected = LocalOnAppAccentColorSelected.current
    val langCode = LocalSelectedLanguageCode.current
    val isArabicLocale = remember(langCode) {
        AppLanguageManager.resolveEffectiveCode(langCode).startsWith("ar")
    }
    val hasUltra = hasRequiredRank(currentRank, CordlyRepository.RANK_ULTRA)
    val hasNeoPlus = hasRequiredRank(currentRank, CordlyRepository.RANK_NEO_PLUS)

    val statuses = listOf("online" to CordlyEmerald, "idle" to CordlyAmber, "dnd" to CordlyCrimson)
    val profileColors = listOf("#5865F2", "#22D3EE", "#10B981", "#F59E0B", "#EC4899", "#9333EA", "#EF4444")

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isProcessingImage) {
            LinearProgressIndicator(color = CordlyCyan, trackColor = CordlyDivider, modifier = Modifier.fillMaxWidth())
        }
        if (imageError != null) {
            Surface(color = CordlyCrimson.copy(alpha = 0.16f), border = BorderStroke(1.dp, CordlyCrimson), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(imageError, style = MaterialTheme.typography.labelMedium, color = CordlyCrimson, modifier = Modifier.padding(10.dp))
            }
        }

        // Profile Preview Card
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
                        .height(104.dp)
                        .background(Brush.linearGradient(listOf(CordlyBlurple, Color(0xFF9333EA), CordlyCyan)))
                ) {
                    if (!pendingBannerUrl.isNullOrBlank()) {
                        CordlyRemoteOrDataImage(pendingBannerUrl, "Profile banner", ContentScale.Crop, Modifier.fillMaxSize())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.58f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onPickBanner)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(strings.bannerLabel, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                        if (pendingBannerUrl != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.58f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClearBanner)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove banner", tint = CordlyCrimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Box(modifier = Modifier.clip(CircleShape).clickable(onClick = onPickAvatar)) {
                        UserAvatar(
                            displayName = displayName.ifBlank { profile?.displayName ?: "User" },
                            photoUrl = pendingPhotoUrl,
                            status = selectedStatus,
                            frameId = profile?.equippedFrame ?: "none",
                            size = 64.dp
                        )
                        Surface(color = CordlyBlurple, shape = CircleShape, border = BorderStroke(2.dp, CordlyCardSurface), modifier = Modifier.align(Alignment.BottomStart).size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName.ifBlank { profile?.displayName ?: "neov user" }, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary)
                        Text("${rankDisplayTitle(currentRank, isArabicLocale)} • ${profile?.friendCode ?: "..."}", style = MaterialTheme.typography.labelSmall, color = CordlyCyan)
                    }
                }
            }
        }

        // Neo Plus Video Avatar Button
        OutlinedButton(
            onClick = { if (hasNeoPlus) onPickAvatarVideo() else onOpenShopForRanks() },
            border = BorderStroke(1.dp, if (hasNeoPlus) CordlyAmber else CordlyDivider),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (hasNeoPlus) Icons.Rounded.VideoLibrary else Icons.Rounded.Lock, contentDescription = null, tint = CordlyAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.videoAvatarButton, color = if (hasNeoPlus) CordlyAmber else CordlyTextSecondary)
        }

        // Ultra Full Profile Color Picker
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(strings.fullProfileColorTitle, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary)
            if (!hasUltra) {
                Text("ULTRA", color = CordlyAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            profileColors.forEach { hex ->
                val color = parseHexColor(hex)
                val isSelected = profile?.profileThemeColorHex == hex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else CordlyDivider, CircleShape)
                        .clickable { if (hasUltra) onUpdateProfileThemeColor(hex) else onOpenShopForRanks() }
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Neo Plus Launcher Logo Switcher
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AppShortcut, contentDescription = null, tint = CordlyAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.appLogoSectionTitle, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            AppLogoVariant.entries.forEach { variant ->
                val selected = variant == currentLogoVariant
                Surface(
                    color = if (selected) variant.badgeColor.copy(alpha = 0.22f) else CordlyCardSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) variant.badgeColor else CordlyDivider),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (hasNeoPlus) onUpdateAppLogo(variant) else onOpenShopForRanks() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(variant.badgeColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabicLocale) variant.titleAr else variant.titleEn, style = MaterialTheme.typography.labelMedium, color = CordlyTextPrimary)
                    }
                }
            }
        }

        // Neo Plus App Layout Customizer
        Surface(
            color = CordlyCardSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CordlyDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ViewQuilt, contentDescription = null, tint = CordlyAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.layoutSectionTitle, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabicLocale) "شريط السيرفرات على اليمين" else "Server Rail on Right", style = MaterialTheme.typography.bodySmall, color = CordlyTextSecondary)
                    Switch(
                        checked = layoutPrefs.railOnRight,
                        onCheckedChange = { checked ->
                            if (hasNeoPlus) onUpdateLayoutPrefs(layoutPrefs.copy(railOnRight = checked)) else onOpenShopForRanks()
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabicLocale) "تباعد رسائل مضغوط" else "Compact Chat Spacing", style = MaterialTheme.typography.bodySmall, color = CordlyTextSecondary)
                    Switch(
                        checked = layoutPrefs.compactMessageCards,
                        onCheckedChange = { checked ->
                            if (hasNeoPlus) onUpdateLayoutPrefs(layoutPrefs.copy(compactMessageCards = checked)) else onOpenShopForRanks()
                        }
                    )
                }
            }
        }

        // Preset Banner Styles (including Dragon Ball, Jujutsu Kaisen & Blue Lock Ultra banners)
        Text(strings.bannerStylesTitle, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            ProfileBannerCatalog.presets.forEach { preset ->
                val isSelected = pendingBannerUrl == preset.bannerSource
                val unlocked = hasRequiredRank(currentRank, preset.requiredRank)
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) CordlyCyan else CordlyDivider),
                    modifier = Modifier
                        .width(118.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPresetBanner(preset) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PresetBannerCanvas(preset = preset, modifier = Modifier.fillMaxSize())
                        if (!unlocked) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = CordlyAmber, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(16.dp))
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyCyan, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp))
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(if (isArabicLocale) preset.titleAr else preset.titleEn, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Night & Day Mode
        Text(strings.appearanceModeTitle, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeModeOptionCard(strings.nightModeLabel, Icons.Rounded.DarkMode, Color.Black, Color(0xFF232330), Color.White, currentThemeMode == AppThemeMode.NIGHT, "theme_mode_night", { onThemeModeSelected(AppThemeMode.NIGHT) }, Modifier.weight(1f))
            ThemeModeOptionCard(strings.dayModeLabel, Icons.Rounded.LightMode, Color.White, Color(0xFFCBD5E1), CordlyAmber, currentThemeMode == AppThemeMode.DAY, "theme_mode_day", { onThemeModeSelected(AppThemeMode.DAY) }, Modifier.weight(1f))
        }

        // App Accent Color
        Text(strings.appColorTitle, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            AppAccentColor.entries.forEach { accent ->
                val isSelected = accent == currentAccentColor
                val label = if (isArabicLocale) accent.labelAr else accent.labelEn
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onAccentColorSelected(accent) }.padding(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(accent.primary, accent.darkSecondary))).border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else CordlyDivider, CircleShape)
                    ) {
                        if (isSelected) Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) CordlyTextPrimary else CordlyTextSecondary)
                }
            }
        }

        // App Wallpaper
        Text(strings.backgroundTitle, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf(
                AppWallpaperMode.DEFAULT, AppWallpaperMode.PRESET_1, AppWallpaperMode.PRESET_2, AppWallpaperMode.PRESET_3,
                AppWallpaperMode.SHAPE_HEX, AppWallpaperMode.SHAPE_AURORA, AppWallpaperMode.SHAPE_WAVES, AppWallpaperMode.SHAPE_GALAXY
            ).forEach { mode ->
                WallpaperSwatchCard(mode, wallpaperState.mode == mode, { onSelectWallpaperMode(mode) })
            }
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (wallpaperState.mode == AppWallpaperMode.CUSTOM) 2.dp else 1.dp, if (wallpaperState.mode == AppWallpaperMode.CUSTOM) CordlyCyan else CordlyDivider),
                modifier = Modifier.width(96.dp).height(110.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onPickCustomWallpaper)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = strings.yourPhoto, tint = CordlyCyan, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(strings.yourPhoto, style = MaterialTheme.typography.labelSmall, color = CordlyTextPrimary)
                }
            }
        }

        // Display Name & Status
        OutlinedTextField(value = displayName, onValueChange = onDisplayNameChange, label = { Text(strings.displayNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = customStatus, onValueChange = onCustomStatusChange, label = { Text(strings.customStatusLabel) }, placeholder = { Text(strings.customStatusPlaceholder) }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            statuses.forEach { (key, color) ->
                val active = key == selectedStatus
                Surface(
                    color = if (active) color.copy(alpha = 0.22f) else CordlyCardSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) color else CordlyDivider),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onStatusSelect(key) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusLabel(key, strings), style = MaterialTheme.typography.labelSmall, color = CordlyTextPrimary, maxLines = 1)
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_settings_button")
        ) {
            Text(strings.saveProfileButton)
        }

        OutlinedButton(onClick = onPreviewOwnProfile, border = BorderStroke(1.dp, CordlyCyan), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Rounded.PlayCircleFilled, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.previewProfileButton, color = CordlyCyan)
        }

        OutlinedButton(onClick = onSignOut, border = BorderStroke(1.dp, CordlyCrimson.copy(alpha = 0.6f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
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
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CordlyBlurple else CordlyDivider),
        modifier = modifier.height(60.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Surface(color = previewBg, shape = CircleShape, border = BorderStroke(1.5.dp, previewBorder), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = previewContentTint, modifier = Modifier.size(17.dp)) }
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyBlurple, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun WallpaperSwatchCard(mode: AppWallpaperMode, selected: Boolean, onClick: () -> Unit) {
    val presetRes = mode.drawableRes()
    Surface(
        color = CordlyCanvasBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) CordlyCyan else CordlyDivider),
        modifier = Modifier.width(96.dp).height(110.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                mode.isShapeWallpaper() -> WallpaperShapeCanvas(mode = mode, isDark = CordlyThemeState.isDark, modifier = Modifier.fillMaxSize())
                presetRes != null -> AsyncImage(model = presetRes, contentDescription = mode.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CordlyRailBg, CordlyCanvasBg))))
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyCyan, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp))
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 6.dp, vertical = 5.dp)) {
                Text(mode.label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        if (q.isBlank()) allLangs else allLangs.filter {
            it.nativeName.lowercase().contains(q) || it.englishName.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("${strings.languageTitle} (${allLangs.size} ${strings.languagesCountSuffix})", style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary, fontWeight = FontWeight.Bold)
        Text(strings.languageSubtitle, style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.languageSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CordlyCyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("world_language_search_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(340.dp)) {
            if (query.isBlank()) {
                item(key = "lang_system") {
                    LanguageOptionRow(strings.deviceDefaultLanguage, "Auto System Locale", "system", false, selectedCode == "system") { onLanguageSelected("system") }
                }
            }
            items(filteredLangs, key = { "lang_${it.code}" }) { lang ->
                LanguageOptionRow(lang.nativeName, lang.englishName, lang.code, lang.isRtl, selectedCode.equals(lang.code, true)) { onLanguageSelected(lang.code) }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(nativeName: String, englishName: String, code: String, isRtl: Boolean, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) CordlyBlurple.copy(alpha = 0.22f) else CordlyCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) CordlyCyan else CordlyDivider),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nativeName, style = MaterialTheme.typography.titleSmall, color = CordlyTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("$englishName • ${code.uppercase()}" + if (isRtl) " • RTL" else "", style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WorldLanguagePickerDialog(selectedCode: String, onSelectLanguage: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    val allLangs = remember { AppLanguageManager.allWorldLanguages }
    val filteredLangs = remember(query, allLangs) {
        val q = query.trim().lowercase()
        if (q.isBlank()) allLangs else allLangs.filter {
            it.nativeName.lowercase().contains(q) || it.englishName.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        title = { Text("${strings.languageTitle} (${allLangs.size})", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text(strings.languageSearchPlaceholder) }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CordlyCyan) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (query.isBlank()) {
                        item { LanguageOptionRow(strings.deviceDefaultLanguage, "Auto", "system", false, selectedCode == "system") { onSelectLanguage("system"); onDismiss() } }
                    }
                    items(filteredLangs, key = { "dlg_${it.code}" }) { lang ->
                        LanguageOptionRow(lang.nativeName, lang.englishName, lang.code, lang.isRtl, selectedCode.equals(lang.code, true)) { onSelectLanguage(lang.code); onDismiss() }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.closeButton) } }
    )
}
