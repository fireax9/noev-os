package com.example.ui.cordly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import com.example.ui.theme.CordlyThemeState
import kotlinx.coroutines.delay

@Composable
fun AppEntranceIntroOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandName = "neov chat"

    val logoScale = remember { Animatable(0.45f) }
    val logoOffsetY = remember { Animatable(18f) }
    val glowAlpha = remember { Animatable(0f) }
    val containerAlpha = remember { Animatable(1f) }

    var typedCount by remember { mutableIntStateOf(0) }
    var showCursor by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundEffectsManager.play(AppSoundEffect.ENTRANCE_CHIME)
        // Stage 1: Pop the lowercase 'n' emblem BIG in the center
        glowAlpha.animateTo(0.85f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        logoScale.animateTo(
            targetValue = 1.9f,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        delay(260)

        // Stage 2: Shrink 'n' smoothly down to a sleek compact badge while shifting slightly up
        logoOffsetY.animateTo(
            targetValue = -14f,
            animationSpec = tween(360, easing = FastOutSlowInEasing)
        )
        logoScale.animateTo(
            targetValue = 0.96f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // Stage 3: Type out "neov chat" character-by-character cleanly
        showCursor = true
        for (i in 1..brandName.length) {
            typedCount = i
            delay(58)
        }
        showTagline = true
        delay(220)
        showCursor = false
        delay(340)

        // Stage 4: Smooth fade-out into main UI
        glowAlpha.animateTo(0f, animationSpec = tween(280))
        containerAlpha.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
        onFinished()
    }

    val typedText = brandName.take(typedCount)
    val cursorAlpha by animateFloatAsState(
        targetValue = if (showCursor) 1f else 0f,
        animationSpec = tween(150),
        label = "introCursorAlpha"
    )

    val auraPrimary = if (CordlyThemeState.isDark) Color(0xFF5865F2) else Color(0xFF404EED)
    val auraSecondary = if (CordlyThemeState.isDark) Color(0xFF0C05AB) else Color(0xFF7289DA)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .alpha(containerAlpha.value)
            .background(CordlyCanvasBg)
            .testTag("app_entrance_intro")
    ) {
        // Soft Discord-Blurple Ambient Glow behind the big 'n'
        Box(
            modifier = Modifier
                .size(290.dp)
                .alpha(glowAlpha.value * 0.42f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            auraPrimary.copy(alpha = 0.65f),
                            auraSecondary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                color = Color(0xFF5865F2),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.30f)
                ),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .offset(y = logoOffsetY.value.dp)
                    .scale(logoScale.value)
                    .size(82.dp)
                    .testTag("app_entrance_logo")
            ) {
                AsyncImage(
                    model = R.drawable.neov_icon_fg,
                    contentDescription = "neov chat n logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Typewriter brand name — always LTR so "neov chat" writes smoothly
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("app_entrance_title")
                ) {
                    Text(
                        text = buildAnnotatedString {
                            val firstWordLen = typedText.length.coerceAtMost(4)
                            if (firstWordLen > 0) {
                                withStyle(
                                    SpanStyle(
                                        color = CordlyTextPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append(typedText.substring(0, firstWordLen))
                                }
                            }
                            if (typedText.length > 4) {
                                withStyle(
                                    SpanStyle(
                                        color = CordlyBlurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(typedText.substring(4))
                                }
                            }
                        },
                        fontSize = 32.sp,
                        letterSpacing = 0.6.sp
                    )
                    if (cursorAlpha > 0.01f) {
                        Text(
                            text = "|",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            color = CordlyCyan.copy(alpha = cursorAlpha),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(260)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Text(
                    text = "Voice • Video • Community",
                    style = MaterialTheme.typography.labelMedium,
                    color = CordlyTextSecondary,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
