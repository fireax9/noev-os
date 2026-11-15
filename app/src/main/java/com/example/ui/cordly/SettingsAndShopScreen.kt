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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
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
                            text = "neov chat Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary
                        )
                        Text(
                            text = "${profile?.peyPoints ?: 200} P E Y Points Available",
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = CordlyTextSecondary)
                    }
                }

                // Sub-Tabs: Settings & Appearance | P E Y Shop
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CordlyRailBg)
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    ModalTabButton(
                        icon = Icons.Rounded.Palette,
                        label = "Profile & Theme",
                        selected = selectedTab == 0,
                        testTag = "settings_profile_tab",
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ModalTabButton(
                        icon = Icons.Rounded.ShoppingBag,
                        label = "P E Y Shop",
                        selected = selectedTab == 1,
                        testTag = "settings_shop_tab",
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CordlyDivider)

                if (selectedTab == 0) {
                    SettingsProfileTab(
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
                } else {
                    PeyShopTab(
                        profile = profile,
                        isBusy = isBusy,
                        onBuyFrame = onBuyFrame,
                        onEquipFrame = onEquipFrame,
                        onBuyProfileEffect = onBuyProfileEffect,
                        onEquipProfileEffect = onEquipProfileEffect,
                        onPreviewOwnProfile = onPreviewOwnProfile
                    )
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
                    color = Color(0xFFFFB4AB),
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

        // App Background Wallpaper Selector (3 Provided Wallpapers + Default + Custom Photo)
        Text(
            text = "App Background Wallpaper",
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary
        )
        Text(
            text = "Pick one of the 3 studio gradients or upload your own wallpaper photo.",
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
                AppWallpaperMode.PRESET_3
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
                            contentDescription = "Custom wallpaper",
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
                            contentDescription = "Pick custom background image",
                            tint = CordlyCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your Photo",
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
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = customStatus,
            onValueChange = onCustomStatusChange,
            label = { Text("Custom Status") },
            placeholder = { Text("Playing ranked / Hanging out...") },
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
                            text = if (key == "dnd") "DND" else statusLabel(key),
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextPrimary
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
            Text("Save Profile & Appearance")
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
            Text("Preview Discord Profile & 5s Video", color = CordlyCyan)
        }

        OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.dp, CordlyCrimson.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CordlyCrimson, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", color = CordlyCrimson)
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
            if (presetRes != null) {
                AsyncImage(
                    model = presetRes,
                    contentDescription = mode.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(CordlyRailBg, CordlyCanvasBg))
                        )
                )
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
                    text = mode.label,
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
    onBuyFrame: (ProfileFrameItem) -> Unit,
    onEquipFrame: (String) -> Unit,
    onBuyProfileEffect: (ProfileVideoEffectItem, String?, Boolean) -> Unit,
    onEquipProfileEffect: (String, String?, Boolean) -> Unit,
    onPreviewOwnProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shopSection by remember { mutableIntStateOf(0) } // 0 = 5s Profile Videos, 1 = Animated Frames
    var processingVideoError by remember { mutableStateOf<String?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }

    val balance = profile?.peyPoints ?: 200L
    val ownedFrames = profile?.ownedFrames?.ifEmpty { listOf("none") } ?: listOf("none")
    val equippedFrame = profile?.equippedFrame ?: "none"

    val ownedEffects = profile?.ownedProfileEffects?.ifEmpty { listOf("none") } ?: listOf("none")
    val equippedEffect = profile?.equippedProfileEffect ?: "none"
    val uid = profile?.userId ?: ""

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && uid.isNotBlank()) {
            processingVideoError = null
            isProcessingVideo = true
            scope.launch {
                val processedResult = VideoEffectHelper.processPickedVideoTo5Seconds(
                    context = context,
                    sourceUri = uri,
                    uid = uid
                )
                isProcessingVideo = false
                processedResult
                    .onSuccess { processed ->
                        val customEffect = PeyShopCatalog.findEffectById("custom_video")
                        val alreadyOwned = ownedEffects.contains("custom_video")
                        if (alreadyOwned) {
                            onEquipProfileEffect("custom_video", processed.dataUri, !processed.cloudSynced)
                        } else {
                            onBuyProfileEffect(customEffect, processed.dataUri, !processed.cloudSynced)
                        }
                    }
                    .onFailure { err ->
                        processingVideoError = err.localizedMessage ?: "Failed to trim picked video to 5 seconds."
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
                            text = "Earn +200 P E Y at sign-in and +200 P E Y for every new friend!",
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

        // Shop Category Switcher: 5s Video Intros vs Animated Avatar Frames
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModalTabButton(
                    icon = Icons.Rounded.VideoLibrary,
                    label = "5s Video Effects",
                    selected = shopSection == 0,
                    testTag = "shop_videos_tab",
                    onClick = { shopSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                ModalTabButton(
                    icon = Icons.Rounded.Stars,
                    label = "Avatar Frames",
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
                        color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (shopSection == 0) {
            item {
                Text(
                    text = "Discord-style 5-second profile video animations (plays automatically whenever someone opens your profile!).",
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyTextSecondary
                )
            }

            items(PeyShopCatalog.profileEffects, key = { "eff_${it.id}" }) { effect ->
                val isOwned = effect.id == "none" || ownedEffects.contains(effect.id)
                val isEquipped = equippedEffect == effect.id
                val canAfford = balance >= effect.price
                val customFile = remember(uid, profile?.profileVideoData) {
                    if (uid.isNotBlank()) AppAppearanceStore.customProfileVideoFile(context, uid) else null
                }
                val hasCustomFile = effect.isCustomUpload &&
                    ((customFile != null && customFile.exists()) || !profile?.profileVideoData.isNullOrBlank())

                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isEquipped) 1.5.dp else 1.dp,
                        color = if (isEquipped) CordlyCyan else CordlyDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Lightweight preview box for 5s video animation (full MP4 plays smoothly in DiscordProfileSheetDialog)
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
                                            contentDescription = "Preview 5s effect",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
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
                                Button(
                                    onClick = {
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
                                    modifier = Modifier.testTag("pick_custom_video_effect_button")
                                ) {
                                    Text(
                                        text = when {
                                            isProcessingVideo -> "5s..."
                                            isOwned -> "Pick Video"
                                            else -> "Unlock"
                                        }
                                    )
                                }
                                if (hasCustomFile && !isEquipped && isOwned) {
                                    OutlinedButton(
                                        onClick = { onEquipProfileEffect("custom_video", null, false) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Equip", color = CordlyTextPrimary, style = MaterialTheme.typography.labelSmall)
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
                                    Text("Buy")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
