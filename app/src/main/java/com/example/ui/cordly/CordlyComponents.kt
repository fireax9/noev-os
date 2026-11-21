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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AddTask
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.theme.CordlyThemeState

fun statusColor(status: String): Color = when (status) {
    "online" -> CordlyEmerald
    "idle" -> CordlyAmber
    "dnd" -> CordlyCrimson
    else -> CordlyTextMuted
}

fun statusLabel(status: String, strings: AppStrings = EnglishStrings): String = when (status) {
    "online" -> strings.statusOnline
    "idle" -> strings.statusIdle
    "dnd" -> strings.statusDnd
    else -> strings.statusOffline
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
fun WallpaperShapeCanvas(
    mode: AppWallpaperMode,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (mode) {
            AppWallpaperMode.SHAPE_HEX -> {
                val bg = if (isDark) {
                    listOf(Color(0xFF040914), Color(0xFF0B1F3A), Color(0xFF071324))
                } else {
                    listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color(0xFFDBEAFE))
                }
                drawRect(Brush.linearGradient(bg))
                val r = (min(w, h) * 0.11f).coerceIn(22f, 76f)
                val dx = r * 1.72f
                val dy = r * 1.50f
                val strokeColor = if (isDark) Color(0xFF22D3EE).copy(alpha = 0.24f) else Color(0xFF0284C7).copy(alpha = 0.20f)
                var row = 0
                var y = 0f
                while (y < h + r) {
                    var x = if (row % 2 == 0) 0f else dx * 0.5f
                    while (x < w + r) {
                        val path = Path().apply {
                            for (i in 0..5) {
                                val ang = (60 * i - 30) * PI / 180.0
                                val px = x + r * 0.86f * cos(ang).toFloat()
                                val py = y + r * 0.86f * sin(ang).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(path, color = strokeColor, style = Stroke(width = 1.6f))
                        x += dx
                    }
                    y += dy
                    row++
                }
            }
            AppWallpaperMode.SHAPE_AURORA -> {
                val bg = if (isDark) {
                    listOf(Color(0xFF050816), Color(0xFF102A3B), Color(0xFF1E1B4B))
                } else {
                    listOf(Color(0xFFECFDF5), Color(0xFFE0E7FF), Color(0xFFF5F3FF))
                }
                drawRect(Brush.verticalGradient(bg))
                val wave1 = if (isDark) Color(0xFF10B981).copy(alpha = 0.24f) else Color(0xFF059669).copy(alpha = 0.16f)
                val wave2 = if (isDark) Color(0xFF6366F1).copy(alpha = 0.28f) else Color(0xFF4F46E5).copy(alpha = 0.16f)
                for (idx in 0..2) {
                    val y0 = h * (0.28f + idx * 0.22f)
                    val path = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, y0)
                        cubicTo(w * 0.35f, y0 - h * 0.16f, w * 0.65f, y0 + h * 0.18f, w, y0 - h * 0.08f)
                        lineTo(w, h)
                        close()
                    }
                    drawPath(path, color = if (idx % 2 == 0) wave1 else wave2)
                }
            }
            AppWallpaperMode.SHAPE_WAVES -> {
                val bg = if (isDark) {
                    listOf(Color(0xFF090614), Color(0xFF1E103C), Color(0xFF0C0524))
                } else {
                    listOf(Color(0xFFFDF2F8), Color(0xFFEDE9FE), Color(0xFFEEF2FF))
                }
                drawRect(Brush.linearGradient(bg))
                val ringColor = if (isDark) Color(0xFFEC4899) else Color(0xFF7C3AED)
                val maxR = min(w, h) * 0.9f
                for (ring in 1..6) {
                    val radius = (maxR / 6f) * ring
                    drawCircle(
                        color = ringColor.copy(alpha = if (isDark) 0.22f else 0.16f),
                        radius = radius,
                        center = Offset(w * 0.82f, h * 0.22f),
                        style = Stroke(width = 2.4f)
                    )
                    drawCircle(
                        color = CordlyBlurple.copy(alpha = if (isDark) 0.20f else 0.14f),
                        radius = radius * 0.72f,
                        center = Offset(w * 0.18f, h * 0.78f),
                        style = Stroke(width = 2.0f)
                    )
                }
            }
            AppWallpaperMode.SHAPE_GALAXY -> {
                val bg = if (isDark) {
                    listOf(Color(0xFF03030B), Color(0xFF0F172A), Color(0xFF170B3B))
                } else {
                    listOf(Color(0xFFF1F5F9), Color(0xFFE0E7FF), Color(0xFFE0F2FE))
                }
                drawRect(Brush.verticalGradient(bg))
                val gridColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color(0xFF3B82F6).copy(alpha = 0.14f)
                val spacing = (min(w, h) * 0.12f).coerceIn(28f, 64f)
                var x = spacing
                while (x < w) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += spacing
                }
                var y = spacing
                while (y < h) {
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += spacing
                }
                val diamondSize = min(w, h) * 0.24f
                val cx = w * 0.5f
                val cy = h * 0.42f
                val diamond = Path().apply {
                    moveTo(cx, cy - diamondSize)
                    lineTo(cx + diamondSize, cy)
                    lineTo(cx, cy + diamondSize)
                    lineTo(cx - diamondSize, cy)
                    close()
                }
                drawPath(diamond, color = CordlyBlurple.copy(alpha = if (isDark) 0.26f else 0.18f), style = Stroke(width = 2.5f))
            }
            else -> Unit
        }
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
        val scrimColor = if (CordlyThemeState.isDark) {
            Color(0xFF080A12).copy(alpha = 0.56f)
        } else {
            Color(0xFFF8FAFC).copy(alpha = 0.72f)
        }
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
                        .background(scrimColor)
                )
            }
            wallpaperState.mode.isShapeWallpaper() -> {
                WallpaperShapeCanvas(
                    mode = wallpaperState.mode,
                    isDark = CordlyThemeState.isDark,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (CordlyThemeState.isDark) Color.Black.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.18f)
                        )
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
                        .background(scrimColor)
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
                        val centerOffset = Offset(
                            this.size.width / 2f,
                            this.size.height / 2f
                        )
                        val sweepBrush = Brush.sweepGradient(frame.ringColors)

                        // Soft pulsing outer halo
                        if (frame.animated) {
                            drawCircle(
                                color = primaryGlow.copy(alpha = 0.24f * pulseScale),
                                radius = ringRadius + strokePx * 0.7f,
                                style = Stroke(width = strokePx * 1.3f)
                            )
                        }

                        when (frame.shapeStyle) {
                            FrameShapeStyle.OCTAGON_CYBER -> {
                                val octPath = Path().apply {
                                    for (i in 0..7) {
                                        val ang = (45.0 * i + 22.5) * PI / 180.0
                                        val px = centerOffset.x + ringRadius * cos(ang).toFloat()
                                        val py = centerOffset.y + ringRadius * sin(ang).toFloat()
                                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                                    }
                                    close()
                                }
                                rotate(rotationAngle * 0.35f) {
                                    drawPath(
                                        path = octPath,
                                        brush = sweepBrush,
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                if (frame.animated) {
                                    rotate(counterRotation * 0.5f) {
                                        for (corner in 0..3) {
                                            val ang = (corner * 90.0) * PI / 180.0
                                            val pt = Offset(
                                                centerOffset.x + ringRadius * cos(ang).toFloat(),
                                                centerOffset.y + ringRadius * sin(ang).toFloat()
                                            )
                                            drawCircle(Color.White, radius = (strokePx * 0.72f).coerceAtLeast(2f), center = pt)
                                        }
                                    }
                                }
                            }
                            FrameShapeStyle.PRISM_DIAMOND -> {
                                val diamondSide = ringRadius * 1.44f
                                val boxTopLeft = Offset(
                                    centerOffset.x - diamondSide / 2f,
                                    centerOffset.y - diamondSide / 2f
                                )
                                val boxSize = Size(diamondSide, diamondSide)
                                val cornerRad = CornerRadius(strokePx * 1.8f, strokePx * 1.8f)
                                rotate(rotationAngle * 0.5f) {
                                    drawRoundRect(
                                        brush = sweepBrush,
                                        topLeft = boxTopLeft,
                                        size = boxSize,
                                        cornerRadius = cornerRad,
                                        style = Stroke(width = strokePx * 0.92f)
                                    )
                                }
                                rotate(counterRotation * 0.5f + 45f) {
                                    drawRoundRect(
                                        color = secondaryGlow.copy(alpha = 0.85f),
                                        topLeft = boxTopLeft,
                                        size = boxSize,
                                        cornerRadius = cornerRad,
                                        style = Stroke(width = strokePx * 0.72f)
                                    )
                                }
                            }
                            FrameShapeStyle.DOUBLE_STARBURST -> {
                                val starPath = Path().apply {
                                    val points = 12
                                    for (i in 0 until points * 2) {
                                        val rad = if (i % 2 == 0) ringRadius else ringRadius * 0.84f
                                        val ang = (180.0 / points * i) * PI / 180.0
                                        val px = centerOffset.x + rad * cos(ang).toFloat()
                                        val py = centerOffset.y + rad * sin(ang).toFloat()
                                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                                    }
                                    close()
                                }
                                rotate(rotationAngle * 0.5f) {
                                    drawPath(starPath, brush = sweepBrush, style = Stroke(width = strokePx * 0.9f))
                                }
                                if (frame.animated) {
                                    rotate(counterRotation) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.72f),
                                            radius = ringRadius * 0.86f,
                                            style = Stroke(
                                                width = strokePx * 0.45f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                                            )
                                        )
                                    }
                                }
                            }
                            FrameShapeStyle.FLAME_CROWN -> {
                                rotate(rotationAngle * 0.65f) {
                                    drawCircle(
                                        brush = sweepBrush,
                                        radius = ringRadius,
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                val flareCount = 6
                                rotate(counterRotation * 0.35f) {
                                    for (i in 0 until flareCount) {
                                        val ang = (360.0 / flareCount * i) * PI / 180.0
                                        val pInner = Offset(
                                            centerOffset.x + (ringRadius - strokePx * 0.4f) * cos(ang).toFloat(),
                                            centerOffset.y + (ringRadius - strokePx * 0.4f) * sin(ang).toFloat()
                                        )
                                        val pOuter = Offset(
                                            centerOffset.x + (ringRadius + strokePx * 1.45f) * cos(ang).toFloat(),
                                            centerOffset.y + (ringRadius + strokePx * 1.45f) * sin(ang).toFloat()
                                        )
                                        drawLine(
                                            color = if (i % 2 == 0) primaryGlow else Color.White,
                                            start = pInner,
                                            end = pOuter,
                                            strokeWidth = strokePx * 0.85f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                            FrameShapeStyle.MATRIX_DASHED -> {
                                rotate(rotationAngle) {
                                    drawCircle(
                                        brush = sweepBrush,
                                        radius = ringRadius,
                                        style = Stroke(
                                            width = strokePx,
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f))
                                        )
                                    )
                                }
                                if (frame.animated) {
                                    rotate(counterRotation) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.8f),
                                            radius = ringRadius * 0.90f,
                                            style = Stroke(
                                                width = strokePx * 0.45f,
                                                cap = StrokeCap.Round,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 16f))
                                            )
                                        )
                                    }
                                }
                            }
                            FrameShapeStyle.CIRCLE_ORBIT -> {
                                rotate(rotationAngle) {
                                    drawCircle(
                                        brush = sweepBrush,
                                        radius = ringRadius,
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                if (frame.animated) {
                                    val arcTopLeft = Offset(
                                        centerOffset.x - ringRadius,
                                        centerOffset.y - ringRadius
                                    )
                                    val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
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
                                                cap = StrokeCap.Round
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
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                    val rad = Math.toRadians(rotationAngle.toDouble())
                                    val orbCenter = Offset(
                                        x = centerOffset.x + (ringRadius * cos(rad)).toFloat(),
                                        y = centerOffset.y + (ringRadius * sin(rad)).toFloat()
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = (strokePx * 0.85f).coerceAtLeast(2.2f),
                                        center = orbCenter
                                    )
                                }
                            }
                            FrameShapeStyle.SUKUNA_SHRINE_HANDS -> {
                                // Domain Expansion: Malevolent Shrine animated fire tongues + dark curse slash ring
                                val flameColors = listOf(
                                    Color(0xFFFF4500),
                                    Color(0xFFEF4444),
                                    Color(0xFFF59E0B),
                                    Color(0xFF991B1B),
                                    Color(0xFFFF6B00)
                                )
                                drawCircle(
                                    color = Color(0xFFDC2626).copy(alpha = 0.28f + 0.24f * pulseScale),
                                    radius = ringRadius + strokePx * 1.45f,
                                    style = Stroke(width = strokePx * 1.7f)
                                )
                                rotate(rotationAngle * 0.45f) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(flameColors),
                                        radius = ringRadius,
                                        style = Stroke(width = strokePx * 1.15f)
                                    )
                                }
                                // Flickering animated fire tongues around the entire rim
                                val tongueCount = 12
                                for (i in 0 until tongueCount) {
                                    val baseDeg = i * (360f / tongueCount) + (rotationAngle * 0.28f)
                                    val ang = baseDeg * PI / 180.0
                                    val flameLen = strokePx * (1.5f + 0.8f * sin(ang * 3.0 + pulseScale * PI).toFloat())
                                    val leftAng = (baseDeg - 9f) * PI / 180.0
                                    val rightAng = (baseDeg + 9f) * PI / 180.0
                                    val baseL = Offset(
                                        centerOffset.x + (ringRadius - strokePx * 0.2f) * cos(leftAng).toFloat(),
                                        centerOffset.y + (ringRadius - strokePx * 0.2f) * sin(leftAng).toFloat()
                                    )
                                    val baseR = Offset(
                                        centerOffset.x + (ringRadius - strokePx * 0.2f) * cos(rightAng).toFloat(),
                                        centerOffset.y + (ringRadius - strokePx * 0.2f) * sin(rightAng).toFloat()
                                    )
                                    val tip = Offset(
                                        centerOffset.x + (ringRadius + flameLen) * cos(ang).toFloat(),
                                        centerOffset.y + (ringRadius + flameLen) * sin(ang).toFloat()
                                    )
                                    val flamePath = Path().apply {
                                        moveTo(baseL.x, baseL.y)
                                        lineTo(tip.x, tip.y)
                                        lineTo(baseR.x, baseR.y)
                                        close()
                                    }
                                    drawPath(
                                        path = flamePath,
                                        color = if (i % 3 == 0) Color(0xFFFDE047).copy(alpha = 0.90f)
                                        else if (i % 2 == 0) Color(0xFFFF4500).copy(alpha = 0.88f)
                                        else Color(0xFFDC2626).copy(alpha = 0.86f)
                                    )
                                }
                            }
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
        if (frame.shapeStyle == FrameShapeStyle.SUKUNA_SHRINE_HANDS) {
            // Foreground Malevolent Shrine fire crest + Sukuna Domain Expansion folded hands integrated into the frame
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val cx = w * 0.5f
                val strokePx = frame.strokeDp.dp.toPx()

                // Lower rim blaze enveloping the wrists
                val blazeBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFFF4500), Color(0xFF991B1B)),
                    startY = h * 0.64f,
                    endY = h
                )
                drawArc(
                    brush = blazeBrush,
                    startAngle = 22f,
                    sweepAngle = 136f,
                    useCenter = false,
                    topLeft = Offset(strokePx * 0.45f, strokePx * 0.45f),
                    size = Size(w - strokePx * 0.9f, h - strokePx * 0.9f),
                    style = Stroke(width = strokePx * (1.0f + 0.25f * pulseScale), cap = StrokeCap.Round)
                )

                // Sukuna Domain Expansion hands (symmetric interlocking steeple mudra at bottom center of frame)
                val handsY = h * 0.94f
                val span = w * 0.23f
                val spireH = h * 0.30f
                val inkColor = Color(0xFF14090A)
                val handFill = Color(0xFFFFF5EB)
                val fireAccent = Color(0xFFFF4500)

                val glowCircleRadius = span * (0.95f + 0.16f * pulseScale)
                drawCircle(
                    color = fireAccent.copy(alpha = 0.36f + 0.18f * pulseScale),
                    radius = glowCircleRadius,
                    center = Offset(cx, handsY - spireH * 0.38f)
                )

                // Left hand & raised middle fingers
                val leftHand = Path().apply {
                    moveTo(cx - span * 1.15f, handsY)
                    lineTo(cx - span * 0.56f, handsY - spireH * 0.56f)
                    lineTo(cx - span * 0.04f, handsY - spireH)
                    lineTo(cx + span * 0.02f, handsY - spireH * 0.54f)
                    lineTo(cx - span * 0.28f, handsY - spireH * 0.14f)
                    lineTo(cx - span * 0.85f, handsY + h * 0.02f)
                    close()
                }
                // Right hand & raised middle fingers
                val rightHand = Path().apply {
                    moveTo(cx + span * 1.15f, handsY)
                    lineTo(cx + span * 0.56f, handsY - spireH * 0.56f)
                    lineTo(cx + span * 0.04f, handsY - spireH)
                    lineTo(cx - span * 0.02f, handsY - spireH * 0.54f)
                    lineTo(cx + span * 0.28f, handsY - spireH * 0.14f)
                    lineTo(cx + span * 0.85f, handsY + h * 0.02f)
                    close()
                }

                drawPath(leftHand, color = handFill)
                drawPath(rightHand, color = handFill)
                val inkStroke = Stroke(
                    width = (w * 0.028f).coerceAtLeast(1.3f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                drawPath(leftHand, color = inkColor, style = inkStroke)
                drawPath(rightHand, color = inkColor, style = inkStroke)

                // Sukuna curse wrist markings (double black bands)
                drawLine(
                    color = inkColor,
                    start = Offset(cx - span * 0.98f, handsY - spireH * 0.12f),
                    end = Offset(cx - span * 0.62f, handsY - spireH * 0.03f),
                    strokeWidth = (w * 0.028f).coerceAtLeast(1.3f),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = inkColor,
                    start = Offset(cx + span * 0.98f, handsY - spireH * 0.12f),
                    end = Offset(cx + span * 0.62f, handsY - spireH * 0.03f),
                    strokeWidth = (w * 0.028f).coerceAtLeast(1.3f),
                    cap = StrokeCap.Round
                )
            }
        }
        val artRes = frame.artworkResId
        if (artRes != null && size >= 38.dp) {
            val emblemSize = (size.value * 0.36f).coerceIn(15f, 32f).dp
            Surface(
                color = CordlySurfaceDark,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, primaryGlow),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(emblemSize)
            ) {
                Image(
                    painter = painterResource(artRes),
                    contentDescription = frame.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
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
    onOpenQuestsClick: () -> Unit = {},
    onSelectServer: (Server) -> Unit,
    onCreateServerClick: () -> Unit,
    onJoinServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
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
                contentDescription = strings.messagesTab,
                tint = if (isHomeSelected) Color.White else CordlyTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dedicated Quests & Ultra Directory Button ("المهام")
        RailActionButton(
            icon = Icons.Rounded.AddTask,
            contentDescription = strings.questsTabTitle,
            tint = CordlyAmber,
            testTag = "rail_quests_button",
            onClick = onOpenQuestsClick
        )

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
                    contentDescription = strings.createServerTitle,
                    tint = CordlyEmerald,
                    testTag = "rail_create_server_button",
                    onClick = onCreateServerClick
                )
            }

            item {
                RailActionButton(
                    icon = Icons.Rounded.Explore,
                    contentDescription = strings.joinServerTitle,
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
                    .background(CordlyTextPrimary)
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
    val strings = LocalAppStrings.current
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
                val effectiveRank = com.example.data.repository.CordlyRepository.effectiveRank(profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = profile?.displayName ?: "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        color = CordlyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    RankBadgePill(rank = effectiveRank)
                }
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
                                contentDescription = strings.tabPeyShop,
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
                            ?: statusLabel(profile?.status ?: "online", strings),
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
                    contentDescription = strings.settingsTitle,
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
                    contentDescription = strings.signOutButton,
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

@Composable
fun RankBadgePill(
    rank: String?,
    modifier: Modifier = Modifier
) {
    when (rank) {
        com.example.data.repository.CordlyRepository.RANK_NEO_PLUS -> {
            Surface(
                color = CordlyAmber,
                shape = RoundedCornerShape(6.dp),
                modifier = modifier
            ) {
                Text(
                    text = "NEO+",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        com.example.data.repository.CordlyRepository.RANK_ULTRA -> {
            Surface(
                color = CordlyCyan.copy(alpha = 0.22f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CordlyCyan.copy(alpha = 0.7f)),
                modifier = modifier
            ) {
                Text(
                    text = "ULTRA",
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

val LocalAppLogoVariant = staticCompositionLocalOf { AppLogoVariant.DEFAULT }
val LocalOnAppLogoVariantSelected = staticCompositionLocalOf<(AppLogoVariant) -> Unit> { {} }

@Composable
fun NeovBrandEmblem(
    variant: AppLogoVariant,
    size: Dp = 32.dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val gradientColors = remember(variant) {
        when (variant) {
            AppLogoVariant.DEFAULT -> listOf(Color(0xFF5865F2), Color(0xFF404EED), Color(0xFF3139C8))
            AppLogoVariant.GOLD -> listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309))
            AppLogoVariant.CRIMSON -> listOf(Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFF881337))
            AppLogoVariant.EMERALD -> listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF047857))
        }
    }
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = if (size >= 60.dp) 1.5.dp else 1.dp,
            color = Color.White.copy(alpha = 0.34f)
        ),
        shadowElevation = if (size >= 60.dp) 16.dp else 2.dp,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                // Subtle diagonal highlight sheen
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = w * 0.48f,
                    center = Offset(w * 0.24f, h * 0.20f)
                )
                // Clean modern geometric lowercase 'n' glyph
                val strokeW = w * 0.175f
                val leftX = w * 0.31f
                val rightX = w * 0.69f
                val topY = h * 0.31f
                val bottomY = h * 0.72f
                val archPeakY = h * 0.31f

                val nPath = Path().apply {
                    moveTo(leftX, bottomY)
                    lineTo(leftX, topY)
                    moveTo(leftX, h * 0.45f)
                    cubicTo(
                        leftX, archPeakY - h * 0.02f,
                        rightX, archPeakY - h * 0.02f,
                        rightX, h * 0.48f
                    )
                    lineTo(rightX, bottomY)
                }
                drawPath(
                    path = nPath,
                    color = Color.White,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}


