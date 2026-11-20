package com.example.ui.cordly

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PublicProfile
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private class VideoTextureController {
    var activePlayer: MediaPlayer? = null
    var activeSurface: Surface? = null
    var boundTexture: SurfaceTexture? = null
    var boundSource: ProfileVideoSource? = null

    fun releasePlayer() {
        try {
            activePlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            activePlayer?.release()
        } catch (_: Exception) {
        }
        activePlayer = null
        try {
            activeSurface?.release()
        } catch (_: Exception) {
        }
        activeSurface = null
        boundTexture = null
        boundSource = null
    }
}

@Composable
fun ProfileVideoBackgroundPlayer(
    source: ProfileVideoSource,
    isUnlimitedDuration: Boolean = false,
    modifier: Modifier = Modifier,
    onProgressFraction: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val controller = remember { VideoTextureController() }
    val latestOnProgress = androidx.compose.runtime.rememberUpdatedState(onProgressFraction)

    LaunchedEffect(source, isUnlimitedDuration) {
        while (isActive) {
            val player = controller.activePlayer
            if (player != null) {
                try {
                    if (player.isPlaying) {
                        val pos = player.currentPosition
                        val totalMs = if (isUnlimitedDuration) {
                            player.duration.takeIf { it > 0 } ?: 1
                        } else {
                            if (pos >= VideoEffectHelper.MAX_PROFILE_VIDEO_MS.toInt()) {
                                player.seekTo(0)
                            }
                            VideoEffectHelper.MAX_PROFILE_VIDEO_MS.toInt()
                        }
                        val fraction = (pos.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                        latestOnProgress.value?.invoke(fraction)
                    }
                } catch (_: Exception) {
                }
            }
            delay(80L)
        }
    }

    DisposableEffect(source) {
        onDispose {
            controller.releasePlayer()
        }
    }

    fun startIfNeeded(textureView: TextureView, surfaceTexture: SurfaceTexture) {
        if (controller.boundSource == source && controller.boundTexture === surfaceTexture && controller.activePlayer != null) {
            return
        }
        controller.releasePlayer()
        val player = MediaPlayer()
        val surface = Surface(surfaceTexture)
        controller.activePlayer = player
        controller.activeSurface = surface
        controller.boundTexture = surfaceTexture
        controller.boundSource = source
        try {
            player.setSurface(surface)
            when (source) {
                is ProfileVideoSource.RawRes -> {
                    val afd = context.resources.openRawResourceFd(source.resId)
                    if (afd != null) {
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } else {
                        val rawUri = Uri.parse("android.resource://${context.packageName}/${source.resId}")
                        player.setDataSource(context, rawUri)
                    }
                }
                is ProfileVideoSource.LocalFile -> {
                    player.setDataSource(source.file.absolutePath)
                }
            }
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { _, w, h ->
                applyCenterCropTransform(textureView, w, h)
            }
            player.setOnPreparedListener { mp ->
                if (controller.activePlayer !== mp) return@setOnPreparedListener
                applyCenterCropTransform(textureView, mp.videoWidth, mp.videoHeight)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.start()
            }
            player.setOnErrorListener { _, _, _ -> true }
            player.prepareAsync()
        } catch (_: Exception) {
            controller.releasePlayer()
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        startIfNeeded(this@apply, surface)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        controller.activePlayer?.let { mp ->
                            try {
                                applyCenterCropTransform(this@apply, mp.videoWidth, mp.videoHeight)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        controller.releasePlayer()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
        update = { textureView ->
            val currentTex = textureView.surfaceTexture
            if (textureView.isAvailable && currentTex != null) {
                startIfNeeded(textureView, currentTex)
            }
        },
        modifier = modifier
    )
}

private fun applyCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val viewWidth = textureView.width
    val viewHeight = textureView.height
    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

    val scaleX: Float
    val scaleY: Float
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

@Composable
fun DiscordProfileSheetDialog(
    targetProfile: PublicProfile?,
    fallbackName: String,
    fallbackPhoto: String?,
    fallbackUserId: String,
    isOwnProfile: Boolean,
    isLoading: Boolean,
    onMessageUser: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val uid = targetProfile?.userId?.takeIf { it.isNotBlank() } ?: fallbackUserId
    val displayName = targetProfile?.displayName?.takeIf { it.isNotBlank() } ?: fallbackName
    val photoUrl = targetProfile?.photoUrl ?: fallbackPhoto
    val bannerUrl = targetProfile?.bannerUrl
    val status = targetProfile?.status ?: "online"
    val customStatus = targetProfile?.customStatus
    val friendCode = targetProfile?.friendCode
    val equippedFrame = targetProfile?.equippedFrame ?: "none"
    val equippedCardFrame = targetProfile?.equippedCardFrame ?: "none"
    val equippedEffect = targetProfile?.equippedProfileEffect ?: "none"
    val memberRank = remember(targetProfile?.rank, targetProfile?.rankExpiresAtMs) {
        com.example.data.repository.CordlyRepository.effectiveRank(targetProfile)
    }
    val customThemeColor = remember(targetProfile?.profileThemeColorHex, memberRank) {
        if (memberRank != com.example.data.repository.CordlyRepository.RANK_NORMAL) {
            targetProfile?.profileThemeColorHex?.takeIf { it.isNotBlank() }?.let { parseHexColor(it) }
        } else {
            null
        }
    }
    val effectItem = remember(equippedEffect) { PeyShopCatalog.findEffectById(equippedEffect) }
    val frameItem = remember(equippedFrame) { PeyShopCatalog.findById(equippedFrame) }
    val cardFrameItem = remember(equippedCardFrame) { PeyShopCatalog.findCardFrameById(equippedCardFrame) }

    val videoSource = remember(uid, equippedEffect, targetProfile?.profileVideoData) {
        VideoEffectHelper.resolveVideoSource(
            context = context,
            uid = uid,
            equippedEffect = equippedEffect,
            profileVideoData = targetProfile?.profileVideoData
        )
    }
    val avatarVideoSource = remember(uid, memberRank, targetProfile?.avatarVideoData) {
        if (memberRank == com.example.data.repository.CordlyRepository.RANK_NEO_PLUS) {
            VideoEffectHelper.resolveAvatarVideoSource(
                context = context,
                uid = uid,
                avatarVideoData = targetProfile?.avatarVideoData
            )
        } else {
            null
        }
    }

    val progressState = remember(videoSource) { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val hasCardFrame = cardFrameItem.id != "none"
        val borderColors = when {
            hasCardFrame -> cardFrameItem.borderColors
            customThemeColor != null -> listOf(customThemeColor, customThemeColor.copy(alpha = 0.65f), CordlyCyan)
            videoSource != null -> effectItem.accentColors
            else -> listOf(CordlyDivider, CordlyBlurple)
        }
        val activeCardAccent = if (hasCardFrame) cardFrameItem.accentColor else customThemeColor
        Surface(
            color = activeCardAccent?.copy(alpha = 0.16f)?.let { tint ->
                Color(
                    red = (CordlySurfaceDark.red * 0.82f + tint.red * 0.18f).coerceIn(0f, 1f),
                    green = (CordlySurfaceDark.green * 0.82f + tint.green * 0.18f).coerceIn(0f, 1f),
                    blue = (CordlySurfaceDark.blue * 0.82f + tint.blue * 0.18f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            } ?: CordlySurfaceDark,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(
                width = if (hasCardFrame) 2.8.dp else if (customThemeColor != null) 2.dp else 1.5.dp,
                brush = Brush.linearGradient(borderColors)
            ),
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .testTag("discord_profile_viewer_dialog")
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Full-card Discord Profile Video Effect layer (5s or ∞ unlimited when equipped)
                if (videoSource != null) {
                    ProfileVideoBackgroundPlayer(
                        source = videoSource,
                        isUnlimitedDuration = effectItem.isUnlimitedDuration,
                        onProgressFraction = { progressState.floatValue = it },
                        modifier = Modifier.matchParentSize()
                    )
                    // Scrim to keep profile info ultra-readable while the 5s animation pops behind
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        (activeCardAccent ?: Color.Black).copy(alpha = 0.24f),
                                        Color(0xFF0A0D18).copy(alpha = 0.72f),
                                        Color(0xFF0A0D18).copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )
                } else if (cardFrameItem.artworkResId != null) {
                    Image(
                        painter = painterResource(cardFrameItem.artworkResId),
                        contentDescription = cardFrameItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF070910).copy(alpha = 0.28f),
                                        Color(0xFF070910).copy(alpha = 0.78f),
                                        Color(0xFF070910).copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header / Banner Zone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .then(
                                if (videoSource == null && cardFrameItem.artworkResId == null) {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            if (activeCardAccent != null) {
                                                listOf(activeCardAccent, activeCardAccent.copy(alpha = 0.7f), CordlyBlurple)
                                            } else {
                                                listOf(CordlyBlurple, Color(0xFF7C3AED), CordlyCyan)
                                            }
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (videoSource == null && !bannerUrl.isNullOrBlank()) {
                            CordlyRemoteOrDataImage(
                                source = bannerUrl,
                                contentDescription = "$displayName banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Top-left 5s Video Badge + Top-right Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (videoSource != null) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.62f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CordlyCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.PlayCircleFilled,
                                            contentDescription = null,
                                            tint = CordlyCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (effectItem.isUnlimitedDuration) {
                                                "${effectItem.title} • ∞ Full Video"
                                            } else {
                                                "${effectItem.title} • 5s Loop"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.56f),
                                shape = CircleShape
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("close_profile_viewer_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close profile",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (videoSource != null) {
                            IsolatedVideoProgressBar(
                                progressProvider = { progressState.floatValue },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(3.dp)
                            )
                        }
                    }

                    // Avatar + Identity Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.offset(y = (-42).dp)
                            ) {
                                UserAvatar(
                                    displayName = displayName,
                                    photoUrl = photoUrl,
                                    status = status,
                                    frameId = equippedFrame,
                                    size = 94.dp
                                )
                                if (avatarVideoSource != null) {
                                    ProfileVideoBackgroundPlayer(
                                        source = avatarVideoSource,
                                        isUnlimitedDuration = true,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                if (cardFrameItem.id != "none") {
                                    Surface(
                                        color = CordlyElevatedSurface.copy(alpha = 0.90f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, cardFrameItem.accentColor.copy(alpha = 0.75f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = cardFrameItem.accentColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = cardFrameItem.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cardFrameItem.accentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (frameItem.id != "none") {
                                    Surface(
                                        color = CordlyElevatedSurface.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = CordlyAmber,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = frameItem.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CordlyAmber,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.offset(y = (-24).dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = CordlyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                RankBadgePill(rank = memberRank)
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = CordlyCyan,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!friendCode.isNullOrBlank()) {
                                Text(
                                    text = "$friendCode • ${statusLabel(status, strings)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = customThemeColor ?: CordlyCyan
                                )
                            } else {
                                Text(
                                    text = statusLabel(status, strings),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor(status)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = (customThemeColor ?: CordlyCardSurface).copy(
                                    alpha = if (customThemeColor != null) 0.22f else 0.84f
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    (customThemeColor ?: CordlyDivider).copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = strings.aboutMember,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CordlyTextSecondary
                                    )
                                    Text(
                                        text = customStatus?.takeIf { it.isNotBlank() }
                                            ?: "Hanging out on neov chat!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = CordlyTextPrimary
                                    )
                                    HorizontalDivider(color = CordlyDivider.copy(alpha = 0.6f))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.profileIntroLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CordlyTextSecondary
                                        )
                                        Text(
                                            text = if (videoSource != null) effectItem.title else "Standard Banner",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (videoSource != null) CordlyCyan else CordlyTextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (!isOwnProfile && onMessageUser != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onMessageUser()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = customThemeColor ?: CordlyBlurple
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_viewer_message_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.Forum,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.sendDirectMessageButton)
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
private fun IsolatedVideoProgressBar(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val smoothProgress by animateFloatAsState(
        targetValue = progressProvider(),
        label = "video5sProgress"
    )
    LinearProgressIndicator(
        progress = { smoothProgress },
        color = CordlyCyan,
        trackColor = Color.White.copy(alpha = 0.2f),
        modifier = modifier
    )
}
