package com.example.ui.cordly

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.repository.CordlyRepository
import com.example.ui.theme.CordlyAmber
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextMuted
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ChatMessageItem(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhoto: String?,
    val senderRank: String? = null,
    val text: String,
    val messageType: String = "text", // "text" | "image" | "audio" | "sticker" | "gif" | "deleted"
    val mediaData: String? = null,
    val mediaDurationMs: Long? = null,
    val replyToText: String?,
    val deletedForEveryone: Boolean? = null,
    val createdAt: Timestamp?
)

@Composable
fun ChatScreen(
    title: String,
    subtitle: String?,
    isDirectMessage: Boolean,
    showBackButton: Boolean,
    currentUserId: String,
    currentUserRank: String = CordlyRepository.RANK_NORMAL,
    compactCards: Boolean = false,
    messagesState: UiState<List<ChatMessageItem>>,
    hasWallpaper: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (
        text: String,
        replyToText: String?,
        messageType: String,
        mediaData: String?,
        mediaDurationMs: Long?
    ) -> Unit,
    onDeleteMessage: (messageId: String, forEveryone: Boolean) -> Unit,
    onUserProfileClick: (userId: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onHeaderProfileClick: (() -> Unit)? = null,
    onStartVoiceCall: (() -> Unit)? = null,
    onStartVideoCall: (() -> Unit)? = null,
    onStatusToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    var messageText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessageItem?>(null) }
    var showMediaTray by remember { mutableStateOf(false) }
    var mediaTrayTab by remember { mutableIntStateOf(0) } // 0 = Stickers, 1 = GIFs
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableLongStateOf(0L) }
    var playingAudioMsgId by remember { mutableStateOf<String?>(null) }
    var fullscreenImageSource by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val isUltraOrPlus = currentUserRank == CordlyRepository.RANK_ULTRA ||
        currentUserRank == CordlyRepository.RANK_NEO_PLUS

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isEncodingPhoto = true
            scope.launch {
                val maxEdge = if (isUltraOrPlus) 1600 else 360
                val maxBytes = if (isUltraOrPlus) 460_000 else 72_000
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(
                    context = context,
                    uri = uri,
                    maxWidth = maxEdge,
                    maxHeight = maxEdge,
                    maxBase64Bytes = maxBytes
                )
                isEncodingPhoto = false
                if (encoded != null) {
                    val caption = messageText.trim()
                    onSendMessage(caption, replyingTo?.text, "image", encoded, null)
                    messageText = ""
                    replyingTo = null
                } else {
                    onStatusToast("Could not prepare image")
                }
            }
        }
    }

    fun startVoiceNoteCapture() {
        isRecordingAudio = true
        recordingDurationMs = 0L
        scope.launch {
            val result = VoiceNoteHelper.recordVoiceClip(
                shouldContinue = { isRecordingAudio },
                maxDurationMs = 20_000L,
                onDurationTick = { recordingDurationMs = it }
            )
            isRecordingAudio = false
            if (result != null) {
                val (audioData, durationMs) = result
                onSendMessage("", replyingTo?.text, "audio", audioData, durationMs)
                replyingTo = null
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceNoteCapture()
        } else {
            onStatusToast(strings.permissionCallRequired)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (hasWallpaper) Color.Transparent else CordlyCanvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Modern Chat Header
        Surface(
            color = CordlySurfaceDark.copy(alpha = if (hasWallpaper) 0.88f else 1f),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = CordlyTextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        color = if (isDirectMessage) CordlyBlurple.copy(alpha = 0.2f) else CordlyCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDirectMessage) Icons.Rounded.AlternateEmail else Icons.Rounded.Tag,
                                contentDescription = null,
                                tint = if (isDirectMessage) CordlyBlurple else CordlyCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onHeaderProfileClick != null) {
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onHeaderProfileClick)
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                        .testTag("chat_header_profile_button")
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val effectiveSubtitle = when {
                            onHeaderProfileClick != null -> "Tap to view profile & video"
                            else -> subtitle
                        }
                        if (!effectiveSubtitle.isNullOrBlank()) {
                            Text(
                                text = effectiveSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (onHeaderProfileClick != null) CordlyCyan else CordlyTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onStartVoiceCall != null) {
                        IconButton(
                            onClick = onStartVoiceCall,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_voice_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = strings.voiceCallAction,
                                tint = CordlyCyan
                            )
                        }
                    }
                    if (onStartVideoCall != null) {
                        IconButton(
                            onClick = onStartVideoCall,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = strings.videoCallAction,
                                tint = CordlyBlurple
                            )
                        }
                    }
                }
                HorizontalDivider(color = CordlyDivider, thickness = 1.dp)
            }
        }

        // Message Feed
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CordlyBlurple)
                    }
                }
                is UiState.Error -> {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Forum,
                        title = "Couldn't load messages",
                        subtitle = messagesState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    if (messages.isEmpty()) {
                        EmptyPlaceholder(
                            icon = if (isDirectMessage) Icons.Rounded.AlternateEmail else Icons.Rounded.Tag,
                            title = "${strings.welcomeToChannelPrefix} ${if (isDirectMessage) title else "#$title"}!",
                            subtitle = strings.startOfConversation,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = if (compactCards) 8.dp else 14.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(if (compactCards) 4.dp else 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(messages, key = { _, item -> item.id }) { index, msg ->
                                val prev = messages.getOrNull(index - 1)
                                val showHeader = prev == null || prev.senderId != msg.senderId ||
                                    ((msg.createdAt?.seconds ?: 0L) - (prev.createdAt?.seconds ?: 0L) > 300)
                                DiscordMessageRow(
                                    message = msg,
                                    isOwnMessage = msg.senderId == currentUserId,
                                    showHeader = showHeader,
                                    compactCards = compactCards,
                                    isAudioPlaying = playingAudioMsgId == msg.id,
                                    onPlayAudioToggle = {
                                        val audioData = msg.mediaData
                                        if (!audioData.isNullOrBlank()) {
                                            if (playingAudioMsgId == msg.id) {
                                                playingAudioMsgId = null
                                            } else {
                                                playingAudioMsgId = msg.id
                                                scope.launch {
                                                    VoiceNoteHelper.playVoiceClip(audioData) {
                                                        if (playingAudioMsgId == msg.id) {
                                                            playingAudioMsgId = null
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onOpenFullscreenImage = { source ->
                                        fullscreenImageSource = source
                                    },
                                    onAvatarClick = {
                                        onUserProfileClick(msg.senderId, msg.senderName, msg.senderPhoto)
                                    },
                                    onReply = { replyingTo = msg },
                                    onDeleteForMe = { onDeleteMessage(msg.id, false) },
                                    onDeleteForEveryone = { onDeleteMessage(msg.id, true) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stickers & GIFs Picker Tray
        AnimatedVisibility(visible = showMediaTray) {
            Surface(
                color = CordlySurfaceDark,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, CordlyDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = mediaTrayTab == 0,
                            onClick = { mediaTrayTab = 0 },
                            label = { Text(strings.stickersLabel) },
                            leadingIcon = {
                                Icon(Icons.Rounded.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CordlyBlurple,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chat_stickers_tab")
                        )
                        FilterChip(
                            selected = mediaTrayTab == 1,
                            onClick = { mediaTrayTab = 1 },
                            label = { Text(strings.gifsLabel) },
                            leadingIcon = {
                                Icon(Icons.Rounded.GifBox, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CordlyCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("chat_gifs_tab")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showMediaTray = false }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = CordlyTextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (mediaTrayTab == 0) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 72.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PeyShopCatalog.stickers, key = { it.id }) { sticker ->
                                Surface(
                                    color = CordlyCardSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, sticker.accentColor.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSendMessage(sticker.emoji, replyingTo?.text, "sticker", sticker.id, null)
                                            replyingTo = null
                                            showMediaTray = false
                                        }
                                        .testTag("send_sticker_${sticker.id}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Text(text = sticker.emoji, fontSize = 26.sp)
                                        Text(
                                            text = sticker.titleEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CordlyTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PeyShopCatalog.gifPresets, key = { it.id }) { gif ->
                                Surface(
                                    color = gif.primaryColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, gif.secondaryColor.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSendMessage(gif.titleEn, replyingTo?.text, "gif", gif.id, null)
                                            replyingTo = null
                                            showMediaTray = false
                                        }
                                        .testTag("send_gif_${gif.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Text(text = gif.symbol, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = gif.titleEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CordlyTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reply Banner
        AnimatedVisibility(visible = replyingTo != null) {
            val target = replyingTo
            if (target != null) {
                Surface(
                    color = CordlyElevatedSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = null,
                            tint = CordlyCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val previewBody = when (target.messageType) {
                            "image" -> strings.photoLabel
                            "audio" -> strings.voiceNoteLabel
                            "sticker" -> target.text
                            "gif" -> "GIF • ${target.text}"
                            else -> target.text
                        }
                        Text(
                            text = "${strings.replyingToPrefix} ${target.senderName}: $previewBody",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CordlyTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { replyingTo = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel reply",
                                tint = CordlyTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Voice Recording Status Strip
        AnimatedVisibility(visible = isRecordingAudio) {
            Surface(
                color = CordlyCrimson.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, CordlyCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = CordlyCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    val sec = recordingDurationMs / 1000L
                    Text(
                        text = "${strings.recordingVoiceHint} (${sec}s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CordlyTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = CordlyCrimson,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isRecordingAudio = false }
                    ) {
                        Text(
                            text = "Stop & Send",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Rich Media Composer Bar
        Surface(
            color = CordlySurfaceDark.copy(alpha = if (hasWallpaper) 0.90f else 1f),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Photo Picker (4K HD for Ultra / Neo+, standard otherwise)
                IconButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isEncodingPhoto && !isRecordingAudio,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_attach_photo_button")
                ) {
                    if (isEncodingPhoto) {
                        CircularProgressIndicator(
                            color = CordlyCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = strings.photoLabel,
                            tint = if (isUltraOrPlus) CordlyCyan else CordlyTextSecondary
                        )
                    }
                }

                // Stickers & GIFs tray toggle
                IconButton(
                    onClick = { showMediaTray = !showMediaTray },
                    enabled = !isRecordingAudio,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_stickers_gifs_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEmotions,
                        contentDescription = strings.stickersLabel,
                        tint = if (showMediaTray) CordlyAmber else CordlyTextSecondary
                    )
                }

                // Text Input Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = {
                        Text(
                            text = if (isDirectMessage) "${strings.messageHintPrefix} $title" else "${strings.messageHintPrefix} #$title",
                            color = CordlyTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val trimmed = messageText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendMessage(trimmed, replyingTo?.text, "text", null, null)
                                messageText = ""
                                replyingTo = null
                            }
                        }
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CordlyCardSurface,
                        unfocusedContainerColor = CordlyCardSurface,
                        focusedBorderColor = CordlyBlurple,
                        unfocusedBorderColor = CordlyDivider,
                        focusedTextColor = CordlyTextPrimary,
                        unfocusedTextColor = CordlyTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Note Record Button
                IconButton(
                    onClick = {
                        if (isRecordingAudio) {
                            isRecordingAudio = false
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                startVoiceNoteCapture()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingAudio) CordlyCrimson else CordlyCardSurface)
                        .testTag("chat_voice_note_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingAudio) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = strings.voiceNoteLabel,
                        tint = if (isRecordingAudio) Color.White else CordlyEmerald
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                val canSend = messageText.trim().isNotEmpty()
                Surface(
                    color = if (canSend) CordlyBlurple else CordlyCardSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(
                        onClick = {
                            val trimmed = messageText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendMessage(trimmed, replyingTo?.text, "text", null, null)
                                messageText = ""
                                replyingTo = null
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message",
                            tint = if (canSend) Color.White else CordlyTextMuted
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Zoom Dialog for Photo Messages
    val fullImg = fullscreenImageSource
    if (fullImg != null) {
        Dialog(
            onDismissRequest = { fullscreenImageSource = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { fullscreenImageSource = null },
                contentAlignment = Alignment.Center
            ) {
                CordlyRemoteOrDataImage(
                    source = fullImg,
                    contentDescription = "Fullscreen image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { fullscreenImageSource = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscordMessageRow(
    message: ChatMessageItem,
    isOwnMessage: Boolean,
    showHeader: Boolean,
    compactCards: Boolean,
    isAudioPlaying: Boolean,
    onPlayAudioToggle: () -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onReply: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showMenu by remember { mutableStateOf(false) }
    val timeText = remember(message.createdAt) {
        val date = (message.createdAt ?: Timestamp.now()).toDate()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    val isDeleted = message.deletedForEveryone == true || message.messageType == "deleted"
    val isNeoPlusSender = !isDeleted && message.senderRank == CordlyRepository.RANK_NEO_PLUS
    val neoGold = Color(0xFFFFD166)

    Surface(
        color = when {
            isNeoPlusSender -> neoGold.copy(alpha = 0.11f)
            isOwnMessage -> CordlyBlurple.copy(alpha = 0.10f)
            else -> CordlyCardSurface.copy(alpha = 0.58f)
        },
        shape = RoundedCornerShape(16.dp),
        border = when {
            isNeoPlusSender -> BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(listOf(neoGold, CordlyAmber, Color(0xFFFDE047)))
            )
            isOwnMessage -> BorderStroke(0.8.dp, CordlyBlurple.copy(alpha = 0.25f))
            else -> BorderStroke(0.8.dp, CordlyDivider.copy(alpha = 0.45f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { showMenu = true },
                onLongClick = { showMenu = true }
            )
            .testTag("chat_message_${message.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (compactCards) 6.dp else 10.dp,
                    bottom = if (compactCards) 6.dp else 10.dp,
                    start = 10.dp,
                    end = 10.dp
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!message.replyToText.isNullOrBlank() && !isDeleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 50.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = null,
                            tint = CordlyCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CordlyCardSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CordlyDivider)
                        ) {
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showHeader) {
                        UserAvatar(
                            displayName = message.senderName,
                            photoUrl = message.senderPhoto,
                            size = if (compactCards) 34.dp else 40.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onAvatarClick)
                        )
                    } else {
                        Box(
                            modifier = Modifier.width(if (compactCards) 34.dp else 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextMuted.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (showHeader) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = message.senderName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when {
                                        isNeoPlusSender -> neoGold
                                        isOwnMessage -> CordlyCyan
                                        else -> CordlyTextPrimary
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = onAvatarClick)
                                )
                                RankBadgePill(rank = message.senderRank)
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CordlyTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        // Message Content By Type
                        when {
                            isDeleted -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = CordlyTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = strings.messageDeletedPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = CordlyTextMuted
                                    )
                                }
                            }
                            message.messageType == "image" -> {
                                val source = message.mediaData
                                if (!source.isNullOrBlank()) {
                                    Surface(
                                        color = Color.Black,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, CordlyDivider),
                                        modifier = Modifier
                                            .widthIn(max = 260.dp)
                                            .heightIn(max = 220.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onOpenFullscreenImage(source) }
                                    ) {
                                        CordlyRemoteOrDataImage(
                                            source = source,
                                            contentDescription = strings.photoLabel,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (message.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = CordlyTextPrimary
                                    )
                                }
                            }
                            message.messageType == "audio" -> {
                                val secs = ((message.mediaDurationMs ?: 1000L) / 1000L).coerceAtLeast(1L)
                                Surface(
                                    color = if (isAudioPlaying) CordlyEmerald.copy(alpha = 0.22f) else CordlyElevatedSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAudioPlaying) CordlyEmerald else CordlyDivider
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(onClick = onPlayAudioToggle)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAudioPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                            contentDescription = strings.voiceNoteLabel,
                                            tint = CordlyEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = CordlyCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "${strings.voiceNoteLabel} • ${secs}s",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CordlyTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            message.messageType == "sticker" -> {
                                val item = remember(message.mediaData) {
                                    PeyShopCatalog.findStickerById(message.mediaData)
                                }
                                Surface(
                                    color = (item?.accentColor ?: CordlyAmber).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        (item?.accentColor ?: CordlyAmber).copy(alpha = 0.45f)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = item?.emoji ?: message.text.ifBlank { "✨" },
                                            fontSize = 48.sp
                                        )
                                        if (item != null) {
                                            Text(
                                                text = item.titleEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = item.accentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            message.messageType == "gif" -> {
                                val gifPreset = remember(message.mediaData) {
                                    PeyShopCatalog.findGifById(message.mediaData)
                                } ?: PeyShopCatalog.gifPresets.first()
                                AnimatedGifBubble(preset = gifPreset)
                            }
                            else -> {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CordlyTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isDeleted) {
                    DropdownMenuItem(
                        text = { Text(strings.replyAction) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onReply()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(strings.deleteForMeAction, color = CordlyTextPrimary) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = CordlyAmber
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDeleteForMe()
                    },
                    modifier = Modifier.testTag("delete_for_me_item")
                )
                if (isOwnMessage && !isDeleted) {
                    DropdownMenuItem(
                        text = { Text(strings.deleteForEveryoneAction, color = CordlyCrimson) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = CordlyCrimson
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteForEveryone()
                        },
                        modifier = Modifier.testTag("delete_for_everyone_item")
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedGifBubble(
    preset: ChatGifPreset,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "gif_${preset.id}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gifPhase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gifPulse"
    )

    Surface(
        color = Color(0xFF0A0E1B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, preset.secondaryColor.copy(alpha = 0.75f)),
        modifier = modifier.size(width = 180.dp, height = 112.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension * 0.44f
                drawCircle(
                    color = preset.primaryColor.copy(alpha = 0.25f * pulse),
                    radius = maxR * 1.15f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = preset.secondaryColor.copy(alpha = 0.65f),
                    radius = maxR * pulse,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                for (i in 0..5) {
                    val ang = (phase + i * 60f) * PI / 180.0
                    val px = cx + (maxR * 1.18f) * cos(ang).toFloat()
                    val py = cy + (maxR * 0.86f) * sin(ang).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) Color.White else preset.primaryColor,
                        radius = 4f,
                        center = Offset(px, py)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = preset.symbol, fontSize = 36.sp)
                Text(
                    text = "GIF • ${preset.titleEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
