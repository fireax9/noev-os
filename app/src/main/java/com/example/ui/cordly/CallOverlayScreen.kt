package com.example.ui.cordly

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CallSession
import com.example.data.model.CallStreamFrame
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
fun IncomingCallBanner(
    call: CallSession,
    onAnswerVoice: () -> Unit,
    onAnswerVideo: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val pulse by rememberInfiniteTransition(label = "ringPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "ringAlpha"
    )

    Surface(
        color = CordlySurfaceDark,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, CordlyEmerald.copy(alpha = pulse)),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("incoming_call_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(CordlyEmerald.copy(alpha = 0.16f), CordlyBlurple.copy(alpha = 0.14f))
                    )
                )
                .padding(14.dp)
        ) {
            UserAvatar(
                displayName = call.callerName,
                photoUrl = call.callerPhoto,
                status = "online",
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.callerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (call.callType == "video") strings.incomingVideoCall else strings.incomingVoiceCall,
                    style = MaterialTheme.typography.labelMedium,
                    color = CordlyCyan
                )
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CordlyCrimson)
                    .testTag("decline_call_button")
            ) {
                Icon(Icons.Rounded.CallEnd, contentDescription = strings.declineCall, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = if (call.callType == "video") onAnswerVideo else onAnswerVoice,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CordlyEmerald)
                    .testTag("answer_call_button")
            ) {
                Icon(
                    imageVector = if (call.callType == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = strings.acceptCall,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ActiveCallOverlayDialog(
    call: CallSession,
    currentUserId: String,
    streams: List<CallStreamFrame>,
    onToggleMute: (Boolean) -> Unit,
    onToggleVideo: (Boolean) -> Unit,
    onPublishStreamSample: (frameData: String?, audioLevel: Double, videoEnabled: Boolean, muted: Boolean) -> Unit,
    onEndCall: () -> Unit,
    onMinimize: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var isMuted by remember(call.id) { mutableStateOf(false) }
    var isSpeakerOn by remember(call.id) { mutableStateOf(call.callType == "video") }
    var isVideoEnabled by remember(call.id) { mutableStateOf(call.callType == "video") }
    var useFrontCamera by remember(call.id) { mutableStateOf(true) }
    var elapsedSeconds by remember(call.id) { mutableLongStateOf(0L) }
    var localMicAmplitude by remember(call.id) { mutableDoubleStateOf(0.0) }
    var latestCapturedFrame by remember(call.id) { mutableStateOf<String?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission || (isVideoEnabled && !hasCameraPermission)) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            )
        }
    }

    // Configure Android AudioManager speakerphone & voice communication mode during active call
    DisposableEffect(call.id, isSpeakerOn) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val prevMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        val prevSpeaker = audioManager?.isSpeakerphoneOn ?: false
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = isSpeakerOn
        } catch (_: Exception) {
        }
        onDispose {
            try {
                audioManager?.mode = prevMode
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = prevSpeaker
            } catch (_: Exception) {
            }
        }
    }

    // Live call duration timer
    LaunchedEffect(call.id, call.status) {
        while (isActive) {
            if (call.status == "active") {
                elapsedSeconds += 1L
            }
            delay(1000L)
        }
    }

    // Real AudioRecord microphone amplitude sampler when permitted and unmuted
    LaunchedEffect(call.id, hasAudioPermission, isMuted) {
        if (!hasAudioPermission || isMuted) {
            localMicAmplitude = 0.0
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            sampleMicAmplitude(
                shouldContinue = { isActive && !isMuted },
                onAmplitude = { amp -> localMicAmplitude = amp }
            )
        }
    }

    // Periodic stream presence & camera frame sync to Firestore call participants
    LaunchedEffect(call.id, isMuted, isVideoEnabled) {
        while (isActive) {
            val frame = if (isVideoEnabled && hasCameraPermission) latestCapturedFrame else null
            onPublishStreamSample(frame, localMicAmplitude, isVideoEnabled && hasCameraPermission, isMuted)
            delay(1200L)
        }
    }

    val isCaller = call.callerId == currentUserId
    val peerName = if (call.targetType == "server") {
        call.calleeName
    } else if (isCaller) {
        call.calleeName
    } else {
        call.callerName
    }
    val peerPhoto = if (isCaller) call.calleePhoto else call.callerPhoto
    val remoteStreams = streams.filter { it.senderId != currentUserId }

    Dialog(
        onDismissRequest = onMinimize,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .testTag("active_call_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF000000), Color(0xFF07070B), Color(0xFF000000))
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Call Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val durationText = String.format(
                            java.util.Locale.US,
                            "%02d:%02d",
                            elapsedSeconds / 60L,
                            elapsedSeconds % 60L
                        )
                        Text(
                            text = when {
                                call.status == "ringing" -> strings.callingStatus
                                call.targetType == "server" -> "${strings.groupVoiceCall} • $durationText"
                                else -> "${strings.connectedCallStatus} • $durationText"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (call.status == "active") CordlyEmerald else CordlyAmber
                        )
                    }

                    Surface(
                        color = CordlyCardSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CordlyDivider),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onMinimize)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = strings.closeButton,
                            style = MaterialTheme.typography.labelMedium,
                            color = CordlyTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Missing runtime permission banner
                AnimatedVisibility(visible = !hasAudioPermission || (isVideoEnabled && !hasCameraPermission)) {
                    Surface(
                        color = CordlyAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = strings.permissionCallRequired,
                                style = MaterialTheme.typography.bodySmall,
                                color = CordlyTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CordlyAmber, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(strings.grantPermissions, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Video / Voice Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideoEnabled && hasCameraPermission) {
                        // Video Split Stage: Remote Stream(s) + Self CameraX Preview
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(
                                color = CordlyCardSurface,
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if ((remoteStreams.firstOrNull()?.audioLevel ?: 0.0) > 0.18) CordlyEmerald else CordlyDivider
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                val firstRemote = remoteStreams.firstOrNull()
                                val remoteBitmap = remember(firstRemote?.frameData) {
                                    decodeBase64Bitmap(firstRemote?.frameData)
                                }
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (remoteBitmap != null && firstRemote?.videoEnabled == true) {
                                        Image(
                                            bitmap = remoteBitmap.asImageBitmap(),
                                            contentDescription = peerName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            UserAvatar(
                                                displayName = firstRemote?.senderName ?: peerName,
                                                photoUrl = peerPhoto,
                                                status = "online",
                                                size = 96.dp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = firstRemote?.senderName ?: peerName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = CordlyTextPrimary
                                            )
                                            if (call.status == "ringing") {
                                                Text(
                                                    text = strings.callingStatus,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = CordlyCyan
                                                )
                                            }
                                        }
                                    }
                                    // Flip camera quick icon
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { useFrontCamera = !useFrontCamera }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Cameraswitch, contentDescription = strings.flipCamera, tint = CordlyCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.flipCamera, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Self live CameraX preview
                            Surface(
                                color = CordlyCardSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (localMicAmplitude > 0.16 && !isMuted) CordlyEmerald else CordlyBlurple
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(185.dp)
                            ) {
                                CameraXLivePreview(
                                    useFrontCamera = useFrontCamera,
                                    onFrameEncoded = { latestCapturedFrame = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        // Voice Call Stage: Participant Cards with live mic amplitude rings
                        val displayParticipants = if (remoteStreams.isNotEmpty()) {
                            remoteStreams
                        } else {
                            listOf(
                                CallStreamFrame(
                                    callId = call.id,
                                    senderId = call.calleeId ?: call.targetId,
                                    senderName = peerName,
                                    audioLevel = 0.0,
                                    videoEnabled = false,
                                    muted = false
                                )
                            )
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 148.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                VoiceParticipantTile(
                                    name = "You",
                                    photoUrl = if (isCaller) call.callerPhoto else call.calleePhoto,
                                    audioLevel = if (isMuted) 0.0 else localMicAmplitude,
                                    muted = isMuted
                                )
                            }
                            items(displayParticipants, key = { it.senderId }) { participant ->
                                VoiceParticipantTile(
                                    name = participant.senderName,
                                    photoUrl = if (participant.senderId == call.callerId) call.callerPhoto else peerPhoto,
                                    audioLevel = if (participant.muted) 0.0 else participant.audioLevel,
                                    muted = participant.muted
                                )
                            }
                        }
                    }
                }

                // Bottom Call Controls Bar
                Surface(
                    color = CordlySurfaceDark,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CordlyDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        CallControlPill(
                            icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (isMuted) strings.unmuteMic else strings.muteMic,
                            active = !isMuted,
                            activeColor = CordlyElevatedSurface,
                            inactiveColor = CordlyCrimson,
                            testTag = "call_mute_button",
                            onClick = {
                                isMuted = !isMuted
                                onToggleMute(isMuted)
                            }
                        )

                        CallControlPill(
                            icon = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                            label = if (isVideoEnabled) strings.cameraOff else strings.cameraOn,
                            active = isVideoEnabled,
                            activeColor = CordlyBlurple,
                            inactiveColor = CordlyCardSurface,
                            testTag = "call_video_button",
                            onClick = {
                                val next = !isVideoEnabled
                                isVideoEnabled = next
                                if (next && !hasCameraPermission) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                                onToggleVideo(next)
                            }
                        )

                        CallControlPill(
                            icon = if (isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            label = strings.speakerOn,
                            active = isSpeakerOn,
                            activeColor = CordlyCyan.copy(alpha = 0.25f),
                            inactiveColor = CordlyCardSurface,
                            testTag = "call_speaker_button",
                            onClick = { isSpeakerOn = !isSpeakerOn }
                        )

                        CallControlPill(
                            icon = Icons.Rounded.CallEnd,
                            label = strings.endCall,
                            active = false,
                            activeColor = CordlyCrimson,
                            inactiveColor = CordlyCrimson,
                            testTag = "call_end_button",
                            onClick = onEndCall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceParticipantTile(
    name: String,
    photoUrl: String?,
    audioLevel: Double,
    muted: Boolean
) {
    val speaking = audioLevel > 0.12 && !muted
    Surface(
        color = CordlyCardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (speaking) 2.dp else 1.dp,
            color = if (speaking) CordlyEmerald else CordlyDivider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = if (speaking) 3.dp else 0.dp,
                        color = if (speaking) CordlyEmerald else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                UserAvatar(
                    displayName = name,
                    photoUrl = photoUrl,
                    status = if (muted) "dnd" else "online",
                    size = 76.dp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (muted) Icons.Rounded.MicOff else Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = if (muted) CordlyCrimson else if (speaking) CordlyEmerald else CordlyTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CallControlPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            color = if (active) activeColor else inactiveColor,
            shape = CircleShape,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CordlyTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CameraXLivePreview(
    useFrontCamera: Boolean,
    onFrameEncoded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val lastEncodedMs = remember { longArrayOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
            executor.shutdownNow()
        }
    }

    LaunchedEffect(useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastEncodedMs[0] >= 1100L) {
                        lastEncodedMs[0] = now
                        encodeProxyToDataUri(imageProxy, useFrontCamera)?.let { dataUri ->
                            onFrameEncoded(dataUri)
                        }
                    }
                    imageProxy.close()
                }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (_: Exception) {
                // Camera may be unavailable in headless emulators without camera hardware
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private fun encodeProxyToDataUri(image: ImageProxy, mirror: Boolean): String? {
    return try {
        val nv21 = yuv420888ToNv21(image) ?: return null
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val rawOut = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 55, rawOut)
        val rawBytes = rawOut.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            if (mirror) postScale(-1f, 1f)
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val scaled = Bitmap.createScaledBitmap(rotated, 160, 120, true)
        val jpegOut = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 48, jpegOut)
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()
        scaled.recycle()
        val encoded = Base64.encodeToString(jpegOut.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$encoded"
    } catch (_: Exception) {
        null
    }
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
    val yBuffer = image.planes.getOrNull(0)?.buffer ?: return null
    val uBuffer = image.planes.getOrNull(1)?.buffer ?: return null
    val vBuffer = image.planes.getOrNull(2)?.buffer ?: return null
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    return nv21
}

private fun decodeBase64Bitmap(dataUri: String?): Bitmap? {
    if (dataUri.isNullOrBlank()) return null
    return try {
        val clean = dataUri.substringAfter("base64,", dataUri)
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
private suspend fun sampleMicAmplitude(
    shouldContinue: () -> Boolean,
    onAmplitude: (Double) -> Unit
) {
    val sampleRate = 16000
    val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(1024)
    val recorder = try {
        AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    } catch (_: Exception) {
        null
    } ?: return

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        return
    }

    val buffer = ShortArray(bufferSize / 2)
    try {
        recorder.startRecording()
        while (shouldContinue()) {
            val read = recorder.read(buffer, 0, buffer.size)
            if (read > 0) {
                var peak = 0
                for (i in 0 until read) {
                    val sample = abs(buffer[i].toInt())
                    if (sample > peak) peak = sample
                }
                val normalized = (peak / 12000.0).coerceIn(0.0, 1.0)
                withContext(Dispatchers.Main) {
                    onAmplitude(normalized)
                }
            }
            delay(180L)
        }
    } catch (_: Exception) {
    } finally {
        try {
            recorder.stop()
        } catch (_: Exception) {
        }
        recorder.release()
    }
}
