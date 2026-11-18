package com.example.ui.cordly

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCanvasBg
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyDivider
import com.example.ui.theme.CordlyElevatedSurface
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextMuted
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class ChatMessageItem(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhoto: String?,
    val text: String,
    val replyToText: String?,
    val createdAt: Timestamp?
)

@Composable
fun ChatScreen(
    title: String,
    subtitle: String?,
    isDirectMessage: Boolean,
    showBackButton: Boolean,
    currentUserId: String,
    messagesState: UiState<List<ChatMessageItem>>,
    hasWallpaper: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (text: String, replyToText: String?) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onUserProfileClick: (userId: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onHeaderProfileClick: (() -> Unit)? = null,
    onStartVoiceCall: (() -> Unit)? = null,
    onStartVideoCall: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    var messageText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessageItem?>(null) }
    val listState = rememberLazyListState()

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
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
        // Top Header
        Surface(
            color = CordlySurfaceDark.copy(alpha = if (hasWallpaper) 0.86f else 1f),
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
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
                            onHeaderProfileClick != null -> "Tap to view profile & 5s video"
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
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                    onAvatarClick = {
                                        onUserProfileClick(msg.senderId, msg.senderName, msg.senderPhoto)
                                    },
                                    onReply = { replyingTo = msg },
                                    onDelete = { onDeleteMessage(msg.id) }
                                )
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
                        Text(
                            text = "${strings.replyingToPrefix} ${target.senderName}: ${target.text}",
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

        // Composer Bar
        Surface(
            color = CordlySurfaceDark.copy(alpha = if (hasWallpaper) 0.88f else 1f),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
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
                                onSendMessage(trimmed, replyingTo?.text)
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
                Spacer(modifier = Modifier.width(8.dp))
                val canSend = messageText.trim().isNotEmpty()
                Surface(
                    color = if (canSend) CordlyBlurple else CordlyCardSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(
                        onClick = {
                            val trimmed = messageText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendMessage(trimmed, replyingTo?.text)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscordMessageRow(
    message: ChatMessageItem,
    isOwnMessage: Boolean,
    showHeader: Boolean,
    onAvatarClick: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val timeText = remember(message.createdAt) {
        val date = (message.createdAt ?: Timestamp.now()).toDate()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { showMenu = true },
                onLongClick = { showMenu = true }
            )
            .padding(
                top = if (showHeader) 8.dp else 1.dp,
                bottom = 2.dp,
                start = 4.dp,
                end = 4.dp
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!message.replyToText.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 52.dp, bottom = 3.dp)
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
                        size = 40.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarClick)
                    )
                } else {
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextMuted.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (showHeader) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.senderName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isOwnMessage) CordlyCyan else CordlyTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onAvatarClick)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CordlyTextPrimary
                    )
                }
            }
        }

        val strings = LocalAppStrings.current
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
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
            if (isOwnMessage) {
                DropdownMenuItem(
                    text = { Text(strings.deleteMessageAction, color = CordlyCrimson) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = CordlyCrimson
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
