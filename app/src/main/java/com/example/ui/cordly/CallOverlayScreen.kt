package com.example.ui.cordly

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
import com.example.data.repository.CordlyRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors

data class InviteableFriend(
    val uid: String,
    val name: String,
    val photoUrl: String?
)

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

    DisposableEffect(call.id) {
        SoundEffectsManager.startRingingLoop(incoming = true)
        onDispose { SoundEffectsManager.stopRingingLoop() }
    }

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
                onClick = {
                    SoundEffectsManager.stopRingingLoop()
                    SoundEffectsManager.play(AppSoundEffect.CALL_END)
                    onDecline()
                },
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
                onClick = {
                    SoundEffectsManager.stopRingingLoop()
                    SoundEffectsManager.play(AppSoundEffect.CALL_CONNECT)
                    if (call.callType == "video") onAnswerVideo() else onAnswerVoice()
                },
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
    currentUserRank: String,
    streams: List<CallStreamFrame>,
    inviteableFriends: List<InviteableFriend> = emptyList(),
    onToggleMute: (Boolean) -> Unit,
    onToggleVideo: (Boolean) -> Unit,
    onUpdateVideoQuality: (String) -> Unit,
    onAddParticipant: (friendUid: String, friendName: String) -> Unit,
    onPublishStreamSample: (
        frameData: String?,
        audioData: String?,
        audioSeq: Long,
        voiceEffect: String,
        faceFilter: String,
        videoQuality: String,
        audioLevel: Double,
        videoEnabled: Boolean,
        muted: Boolean
    ) -> Unit,
    onStatusToast: (String) -> Unit = {},
    onEndCall: () -> Unit,
    onMinimize: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isUltraOrPlus = currentUserRank == CordlyRepository.RANK_ULTRA || currentUserRank == CordlyRepository.RANK_NEO_PLUS
    val isNeoPlus = currentUserRank == CordlyRepository.RANK_NEO_PLUS

    var isMuted by remember(call.id) { mutableStateOf(false) }
    var isSpeakerOn by remember(call.id) { mutableStateOf(true) }
    var isVideoEnabled by remember(call.id) { mutableStateOf(call.callType == "video") }
    var useFrontCamera by remember(call.id) { mutableStateOf(true) }
    var elapsedSeconds by remember(call.id) { mutableLongStateOf(0L) }
    var localMicAmplitude by remember(call.id) { mutableDoubleStateOf(0.0) }
    var latestCapturedFrame by remember(call.id) { mutableStateOf<String?>(null) }

    var selectedQuality by remember(call.id) { mutableStateOf(VideoQualityPreset.fromId(call.videoQuality)) }
    val latestOnPublishSample by rememberUpdatedState(onPublishStreamSample)
    val latestOnStatusToast by rememberUpdatedState(onStatusToast)

    // Shared global Video Quality sync: whenever ANY peer changes quality, update for everyone
    LaunchedEffect(call.videoQuality) {
        val synced = VideoQualityPreset.fromId(call.videoQuality)
        if (synced != selectedQuality) {
            selectedQuality = synced
            latestOnStatusToast("${strings.videoQualityLabel}: ${synced.label}")
        }
    }
    var selectedFaceFilter by remember(call.id) { mutableStateOf(CallFaceFilter.NONE) }
    var selectedVoiceEffect by remember(call.id) { mutableStateOf(VoiceEffectType.NORMAL) }
    var isRecordingCall by remember(call.id) { mutableStateOf(false) }
    var showQualityBar by remember(call.id) { mutableStateOf(false) }
    var showFilterBar by remember(call.id) { mutableStateOf(false) }
    var showVoiceFxBar by remember(call.id) { mutableStateOf(false) }
    var showAddFriendDialog by remember(call.id) { mutableStateOf(false) }

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

    val audioEngine = remember(call.id) { CallAudioEngine(context.applicationContext) }

    // Configure audio routing & start non-blocking AudioTrack playback + release on dispose
    DisposableEffect(call.id, isSpeakerOn) {
        val prev = CallAudioEngine.configureAudioRouting(context, isSpeakerOn)
        audioEngine.startPlaybackTrack(scope)
        onDispose {
            CallAudioEngine.restoreAudioRouting(context, prev)
        }
    }
    DisposableEffect(call.id) {
        onDispose {
            SoundEffectsManager.stopRingingLoop()
            audioEngine.release()
        }
    }

    // Ringback sound while ringing; connect chime once active
    LaunchedEffect(call.status) {
        if (call.status == "ringing" && call.callerId == currentUserId) {
            SoundEffectsManager.startRingingLoop(incoming = false)
        } else if (call.status == "active") {
            SoundEffectsManager.stopRingingLoop()
            SoundEffectsManager.play(AppSoundEffect.CALL_CONNECT)
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

    // Capture real mic PCM -> Gain Boost -> Voice Changer DSP -> µ-law Base64 -> Publish to peers
    LaunchedEffect(call.id, hasAudioPermission) {
        if (!hasAudioPermission) return@LaunchedEffect
        audioEngine.startCaptureLoop(
            scope = scope,
            isMutedProvider = { isMuted },
            voiceEffectProvider = { selectedVoiceEffect },
            onPacketReady = { audioB64, amp, seq ->
                localMicAmplitude = amp
                val frame = if (isVideoEnabled && hasCameraPermission) latestCapturedFrame else null
                latestOnPublishSample(
                    frame,
                    audioB64,
                    seq,
                    selectedVoiceEffect.id,
                    selectedFaceFilter.id,
                    selectedQuality.id,
                    amp,
                    isVideoEnabled && hasCameraPermission,
                    isMuted
                )
            }
        )
    }

    // Non-blocking enqueue of every new remote audio packet for background AudioTrack playback
    val remoteStreams = streams.filter { it.senderId != currentUserId }
    LaunchedEffect(remoteStreams) {
        remoteStreams.forEach { frame ->
            if (!frame.muted) {
                audioEngine.enqueueRemoteAudioPacket(frame.senderId, frame.audioData, frame.audioSeq)
            }
        }
    }

    // Video/presence keep-alive loop (runs whenever mic is muted, permission missing, or video frame changes)
    LaunchedEffect(call.id, hasAudioPermission) {
        while (isActive) {
            if (!hasAudioPermission || isMuted) {
                val frame = if (isVideoEnabled && hasCameraPermission) latestCapturedFrame else null
                latestOnPublishSample(
                    frame,
                    null,
                    0L,
                    selectedVoiceEffect.id,
                    selectedFaceFilter.id,
                    selectedQuality.id,
                    localMicAmplitude,
                    isVideoEnabled && hasCameraPermission,
                    isMuted
                )
            }
            delay(900L)
        }
    }

    val isCaller = call.callerId == currentUserId
    val peerName = when {
        call.participantIds.size > 2 -> "${call.calleeName} (${call.participantIds.size})"
        call.targetType == "server" -> call.calleeName
        isCaller -> call.calleeName
        else -> call.callerName
    }
    val peerPhoto = if (isCaller) call.calleePhoto else call.callerPhoto

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
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Call Header + Quality / Multi-call / Close actions
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
                                call.targetType == "server" -> "${strings.groupVoiceCall} • $durationText • ${selectedQuality.label}"
                                else -> "${strings.connectedCallStatus} • $durationText • ${selectedQuality.label}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (call.status == "active") CordlyEmerald else CordlyAmber
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isNeoPlus && inviteableFriends.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFFD166).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD166)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAddFriendDialog = true }
                                    .testTag("call_add_participant_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = strings.addToCall, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+Call", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                                }
                            }
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
                }

                // Missing runtime permission banner
                AnimatedVisibility(visible = !hasAudioPermission || (isVideoEnabled && !hasCameraPermission)) {
                    Surface(
                        color = CordlyAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
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
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideoEnabled && hasCameraPermission) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Remote video stage — strictly uses ContentScale.Fit so peer image NEVER stretches!
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if ((remoteStreams.firstOrNull()?.audioLevel ?: 0.0) > 0.16) CordlyEmerald else CordlyDivider
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
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            UserAvatar(
                                                displayName = firstRemote?.senderName ?: peerName,
                                                photoUrl = peerPhoto,
                                                status = "online",
                                                size = 92.dp
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

                                    // Peer quality pill + Flip Camera button
                                    if (firstRemote != null) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "${firstRemote.videoQuality} • ${CallFaceFilter.fromId(firstRemote.faceFilter).emoji}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CordlyCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.68f),
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

                            // Self CameraX live preview with FIT_CENTER (no stretching!) + Snapchat Face Filter overlay
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (localMicAmplitude > 0.14 && !isMuted) CordlyEmerald else CordlyBlurple
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                            ) {
                                CameraXLivePreview(
                                    useFrontCamera = useFrontCamera,
                                    qualityPreset = selectedQuality,
                                    faceFilter = selectedFaceFilter,
                                    onFrameEncoded = { latestCapturedFrame = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
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
                                    muted = isMuted,
                                    badge = if (selectedVoiceEffect != VoiceEffectType.NORMAL) selectedVoiceEffect.titleEn else null
                                )
                            }
                            items(displayParticipants, key = { it.senderId }) { participant ->
                                VoiceParticipantTile(
                                    name = participant.senderName,
                                    photoUrl = if (participant.senderId == call.callerId) call.callerPhoto else peerPhoto,
                                    audioLevel = if (participant.muted) 0.0 else participant.audioLevel,
                                    muted = participant.muted,
                                    badge = participant.voiceEffect.takeIf { it != "normal" }?.let { VoiceEffectType.fromId(it).titleEn }
                                )
                            }
                        }
                    }
                }

                // Expandable tool trays: Video Quality (144p..4K), Snapchat Filters, Voice Changer
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnimatedVisibility(visible = showQualityBar) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp)
                        ) {
                            VideoQualityPreset.entries.forEach { preset ->
                                val selected = preset == selectedQuality
                                Surface(
                                    color = if (selected) CordlyBlurple else CordlyCardSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (preset.isHdTier) CordlyCyan.copy(alpha = 0.7f) else CordlyDivider
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedQuality = preset
                                            onUpdateVideoQuality(preset.id)
                                            onStatusToast("${strings.videoQualityLabel}: ${preset.label}")
                                        }
                                        .testTag("quality_chip_${preset.id}")
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else CordlyTextPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showFilterBar) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp)
                        ) {
                            CallFaceFilter.entries.forEach { filter ->
                                val selected = filter == selectedFaceFilter
                                Surface(
                                    color = if (selected) Color(0xFFEC4899) else CordlyCardSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (selected) Color(0xFFF9A8D4) else CordlyDivider),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedFaceFilter = filter }
                                        .testTag("filter_chip_${filter.id}")
                                ) {
                                    Text(
                                        text = "${filter.emoji} ${filter.titleEn}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showVoiceFxBar) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp)
                        ) {
                            VoiceEffectType.entries.forEach { fx ->
                                val locked = fx != VoiceEffectType.NORMAL && !isUltraOrPlus
                                val selected = fx == selectedVoiceEffect
                                Surface(
                                    color = if (selected) CordlyEmerald else CordlyCardSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (locked) CordlyAmber else CordlyDivider),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (locked) {
                                                onStatusToast(strings.ultraFeatureLockedNotice)
                                            } else {
                                                selectedVoiceEffect = fx
                                            }
                                        }
                                        .testTag("voice_fx_${fx.id}")
                                ) {
                                    Text(
                                        text = if (locked) "🔒 ${fx.titleEn}" else fx.titleEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (locked) CordlyAmber else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Feature toggle strip (Quality / Snapchat Filters / Voice Changer / Record)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        MiniCallFeatureChip(
                            icon = Icons.Rounded.HighQuality,
                            label = selectedQuality.id,
                            active = showQualityBar,
                            accent = CordlyCyan,
                            testTag = "call_quality_toggle",
                            onClick = {
                                showQualityBar = !showQualityBar
                                showFilterBar = false
                                showVoiceFxBar = false
                            }
                        )
                        MiniCallFeatureChip(
                            icon = Icons.Rounded.AutoAwesome,
                            label = strings.faceFiltersLabel,
                            active = showFilterBar || selectedFaceFilter != CallFaceFilter.NONE,
                            accent = Color(0xFFEC4899),
                            testTag = "call_filters_toggle",
                            onClick = {
                                showFilterBar = !showFilterBar
                                showQualityBar = false
                                showVoiceFxBar = false
                            }
                        )
                        MiniCallFeatureChip(
                            icon = Icons.Rounded.RecordVoiceOver,
                            label = strings.voiceChangerLabel,
                            active = showVoiceFxBar || selectedVoiceEffect != VoiceEffectType.NORMAL,
                            accent = CordlyEmerald,
                            testTag = "call_voice_fx_toggle",
                            onClick = {
                                showVoiceFxBar = !showVoiceFxBar
                                showQualityBar = false
                                showFilterBar = false
                            }
                        )
                        MiniCallFeatureChip(
                            icon = if (isRecordingCall) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                            label = if (isRecordingCall) strings.stopRecordingLabel else strings.recordCallLabel,
                            active = isRecordingCall,
                            accent = CordlyCrimson,
                            testTag = "call_record_toggle",
                            onClick = {
                                if (!isUltraOrPlus) {
                                    onStatusToast(strings.ultraFeatureLockedNotice)
                                } else if (!isRecordingCall) {
                                    audioEngine.startCallRecording(call.id)
                                    isRecordingCall = true
                                    onStatusToast(strings.recordingStartedNotice)
                                } else {
                                    val saved = audioEngine.stopCallRecording()
                                    isRecordingCall = false
                                    onStatusToast(
                                        if (saved != null) strings.recordingSavedNotice else "Recording stopped"
                                    )
                                }
                            }
                        )
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
                                .padding(vertical = 10.dp, horizontal = 8.dp)
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
                                onClick = {
                                    if (isRecordingCall) {
                                        audioEngine.stopCallRecording()
                                        isRecordingCall = false
                                    }
                                    SoundEffectsManager.play(AppSoundEffect.CALL_END)
                                    onEndCall()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Neo Plus Multi-Person Add Friend Dialog
    if (showAddFriendDialog) {
        val available = inviteableFriends.filter { !call.participantIds.contains(it.uid) }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            containerColor = CordlySurfaceDark,
            title = { Text(strings.addToCall, color = CordlyTextPrimary) },
            text = {
                if (available.isEmpty()) {
                    Text(strings.noFriendsToAddCall, color = CordlyTextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(available, key = { it.uid }) { friend ->
                            Surface(
                                color = CordlyCardSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onAddParticipant(friend.uid, friend.name)
                                        showAddFriendDialog = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    UserAvatar(displayName = friend.name, photoUrl = friend.photoUrl, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(friend.name, color = CordlyTextPrimary, modifier = Modifier.weight(1f))
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = Color(0xFFFFD166))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text(strings.closeButton, color = CordlyTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MiniCallFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) accent.copy(alpha = 0.22f) else CordlyCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) accent else CordlyDivider),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (active) accent else CordlyTextSecondary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else CordlyTextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun VoiceParticipantTile(
    name: String,
    photoUrl: String?,
    audioLevel: Double,
    muted: Boolean,
    badge: String? = null
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
            Spacer(modifier = Modifier.height(8.dp))
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
            if (!badge.isNullOrBlank()) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyCyan
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
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
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
    qualityPreset: VideoQualityPreset,
    faceFilter: CallFaceFilter,
    onFrameEncoded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val lastEncodedMs = remember { longArrayOf(0L) }
    val activeQualityRef = remember { arrayOf(qualityPreset) }
    val activeFilterRef = remember { arrayOf(faceFilter) }
    activeQualityRef[0] = qualityPreset
    activeFilterRef[0] = faceFilter

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
                    if (now - lastEncodedMs[0] >= 900L) {
                        lastEncodedMs[0] = now
                        CallVideoFrameProcessor.encodeProxyToDataUri(
                            image = imageProxy,
                            mirror = useFrontCamera,
                            qualityPreset = activeQualityRef[0],
                            faceFilter = activeFilterRef[0]
                        )?.let { dataUri ->
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
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if (faceFilter != CallFaceFilter.NONE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    CallVideoFrameProcessor.drawFaceFilterOnCanvas(
                        canvas.nativeCanvas,
                        size.width,
                        size.height,
                        faceFilter
                    )
                }
            }
        }
    }
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
