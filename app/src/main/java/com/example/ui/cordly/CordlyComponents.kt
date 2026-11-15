package com.example.ui.cordly

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Server
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
import com.example.ui.theme.CordlyTextMuted
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary

fun statusColor(status: String): Color = when (status) {
    "online" -> CordlyEmerald
    "idle" -> CordlyAmber
    "dnd" -> CordlyCrimson
    else -> CordlyTextMuted
}

fun statusLabel(status: String): String = when (status) {
    "online" -> "Online"
    "idle" -> "Idle"
    "dnd" -> "Do Not Disturb"
    else -> "Offline"
}

fun parseHexColor(hex: String, fallback: Color = CordlyBlurple): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun AppWallpaperBackground(
    wallpaperState: WallpaperState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CordlyCanvasBg)
    ) {
        val presetRes = wallpaperState.mode.drawableRes()
        when {
            wallpaperState.mode == AppWallpaperMode.CUSTOM && wallpaperState.customFilePath != null -> {
                val customBitmap = rememberFileImageBitmap(
                    filePath = wallpaperState.customFilePath,
                    version = wallpaperState.customVersion
                )
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF080A12).copy(alpha = 0.56f))
                )
            }
            presetRes != null -> {
                AsyncImage(
                    model = presetRes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF080A12).copy(alpha = 0.52f))
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    displayName: String,
    photoUrl: String?,
    status: String? = null,
    frameId: String = "none",
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarBg = rememberAvatarColor(displayName)
    val frame = remember(frameId) { PeyShopCatalog.findById(frameId) }
    val hasSpecialFrame = frame.id != "none"

    val rotationAngle: Float
    val counterRotation: Float
    val pulseScale: Float
    if (hasSpecialFrame && frame.animated) {
        val transition = rememberInfiniteTransition(label = "avatarFrame_${frame.id}")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "frameRotation"
        )
        val reverseAngle by transition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "frameCounterRotation"
        )
        val pulse by transition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "framePulse"
        )
        rotationAngle = angle
        counterRotation = reverseAngle
        pulseScale = pulse
    } else {
        rotationAngle = 0f
        counterRotation = 0f
        pulseScale = 0f
    }

    val ringPadding = if (hasSpecialFrame) (frame.strokeDp + 2.2f).dp else 0.dp
    val primaryGlow = frame.ringColors.firstOrNull() ?: CordlyCyan
    val secondaryGlow = frame.ringColors.getOrElse(1) { primaryGlow }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (hasSpecialFrame) {
                    Modifier.drawBehind {
                        val strokePx = frame.strokeDp.dp.toPx()
                        val ringRadius = (this.size.minDimension - strokePx) / 2f
                        val centerOffset = androidx.compose.ui.geometry.Offset(
                            this.size.width / 2f,
                            this.size.height / 2f
                        )

                        // Soft pulsing outer halo
                        if (frame.animated) {
                            drawCircle(
                                color = primaryGlow.copy(alpha = 0.25f * pulseScale),
                                radius = ringRadius + strokePx * 0.75f,
                                style = Stroke(width = strokePx * 1.4f)
                            )
                        }

                        // Primary spinning sweep ring
                        rotate(rotationAngle) {
                            drawCircle(
                                brush = Brush.sweepGradient(frame.ringColors),
                                radius = ringRadius,
                                style = Stroke(width = strokePx)
                            )
                        }

                        // Counter-rotating kinetic arcs + orbiting glowing spark particle
                        if (frame.animated) {
                            val arcTopLeft = androidx.compose.ui.geometry.Offset(
                                centerOffset.x - ringRadius,
                                centerOffset.y - ringRadius
                            )
                            val arcSize = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f)
                            rotate(counterRotation) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.85f),
                                    startAngle = 0f,
                                    sweepAngle = 42f,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(
                                        width = strokePx * 0.65f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    color = secondaryGlow.copy(alpha = 0.9f),
                                    startAngle = 180f,
                                    sweepAngle = 42f,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(
                                        width = strokePx * 0.65f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }

                            val rad = Math.toRadians(rotationAngle.toDouble())
                            val orbCenter = androidx.compose.ui.geometry.Offset(
                                x = centerOffset.x + (ringRadius * kotlin.math.cos(rad)).toFloat(),
                                y = centerOffset.y + (ringRadius * kotlin.math.sin(rad)).toFloat()
                            )
                            drawCircle(
                                color = Color.White,
                                radius = (strokePx * 0.85f).coerceAtLeast(2.2f),
                                center = orbCenter
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Surface(
            color = avatarBg,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxSize()
                .padding(ringPadding)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.40f).sp
                )
                if (!photoUrl.isNullOrBlank()) {
                    CordlyRemoteOrDataImage(
                        source = photoUrl,
                        contentDescription = "$displayName avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
        if (status != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(CordlySurfaceDark)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(statusColor(status))
            )
        }
    }
}

@Composable
fun ServerIconView(
    serverName: String,
    iconEmoji: String,
    iconUrl: String?,
    accentColor: Color,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.25f),
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = iconEmoji,
                fontSize = (size.value * 0.48f).sp
            )
            if (!iconUrl.isNullOrBlank()) {
                CordlyRemoteOrDataImage(
                    source = iconUrl,
                    contentDescription = "$serverName icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                )
            }
        }
    }
}

private fun rememberAvatarColor(seed: String): Color {
    val palette = listOf(
        CordlyBlurple,
        Color(0xFF0EA5E9),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF8B5CF6)
    )
    val idx = (seed.hashCode().let { if (it < 0) -it else it }) % palette.size
    return palette[idx]
}

@Composable
fun ServerRail(
    servers: List<Server>,
    activeDestination: ActiveDestination,
    pendingRequestCount: Int,
    hasWallpaper: Boolean = false,
    onSelectHome: () -> Unit,
    onSelectServer: (Server) -> Unit,
    onCreateServerClick: () -> Unit,
    onJoinServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHomeSelected = activeDestination is ActiveDestination.Home

    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(CordlyRailBg.copy(alpha = if (hasWallpaper) 0.80f else 1f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Home / DMs Hub Button
        RailIconButton(
            selected = isHomeSelected,
            accentColor = CordlyBlurple,
            badgeCount = pendingRequestCount,
            testTag = "rail_home_button",
            onClick = onSelectHome
        ) {
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = "Direct Messages and Friends",
                tint = if (isHomeSelected) Color.White else CordlyTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        HorizontalDivider(
            color = CordlyDivider,
            thickness = 1.dp,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .width(32.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(servers, key = { it.id }) { server ->
                val isSelected = activeDestination is ActiveDestination.ServerSpace &&
                    activeDestination.serverId == server.id
                val serverAccent = parseHexColor(server.accentHex)

                RailIconButton(
                    selected = isSelected,
                    accentColor = serverAccent,
                    badgeCount = 0,
                    testTag = "rail_server_${server.id}",
                    onClick = { onSelectServer(server) }
                ) {
                    Text(
                        text = server.iconEmoji,
                        fontSize = 22.sp
                    )
                    if (!server.iconUrl.isNullOrBlank()) {
                        CordlyRemoteOrDataImage(
                            source = server.iconUrl,
                            contentDescription = "${server.name} icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            item {
                RailActionButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Create Server",
                    tint = CordlyEmerald,
                    testTag = "rail_create_server_button",
                    onClick = onCreateServerClick
                )
            }

            item {
                RailActionButton(
                    icon = Icons.Rounded.Explore,
                    contentDescription = "Join Server by Name",
                    tint = CordlyCyan,
                    testTag = "rail_join_server_button",
                    onClick = onJoinServerClick
                )
            }
        }
    }
}

@Composable
private fun RailIconButton(
    selected: Boolean,
    accentColor: Color,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 16.dp else 25.dp,
        animationSpec = spring(),
        label = "railCorner"
    )
    val pillHeight by animateDpAsState(
        targetValue = if (selected) 34.dp else 0.dp,
        animationSpec = spring(),
        label = "railPill"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor else CordlyCardSurface,
        label = "railBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        if (pillHeight > 1.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(Color.White)
            )
        }

        Surface(
            color = bgColor,
            shape = RoundedCornerShape(cornerRadius),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }

        if (badgeCount > 0) {
            Surface(
                color = CordlyCrimson,
                shape = CircleShape,
                border = BorderStroke(2.dp, CordlyRailBg),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp)
                    .size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RailActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = CordlyCardSurface,
        shape = CircleShape,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun UserDockBar(
    profile: UserPrivate?,
    onEditStatusClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CordlyRailBg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CordlyDivider.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditStatusClick)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("user_dock_bar")
        ) {
            UserAvatar(
                displayName = profile?.displayName ?: "neov user",
                photoUrl = profile?.photoUrl,
                status = profile?.status ?: "online",
                frameId = profile?.equippedFrame ?: "none",
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.displayName ?: "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = CordlyAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenShopClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = "P E Y Shop",
                                tint = CordlyAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${profile?.peyPoints ?: 200} P E Y",
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = profile?.customStatus?.takeIf { it.isNotBlank() }
                            ?: statusLabel(profile?.status ?: "online"),
                        style = MaterialTheme.typography.labelSmall,
                        color = CordlyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onEditStatusClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("edit_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings, Wallpaper, and P E Y Shop",
                    tint = CordlyTextSecondary
                )
            }
            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out",
                    tint = CordlyCrimson
                )
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = CordlyElevatedSurface.copy(alpha = 0.9f),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(
                    Brush.radialGradient(
                        listOf(CordlyBlurple.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CordlyCyan,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CordlyTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = CordlyTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = CordlyBlurple,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAction)
                    .minimumInteractiveComponentSize()
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}
