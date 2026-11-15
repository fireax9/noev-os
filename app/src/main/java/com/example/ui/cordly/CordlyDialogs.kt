package com.example.ui.cordly

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LookupKey
import com.example.ui.theme.CordlyBlurple
import com.example.ui.theme.CordlyCardSurface
import com.example.ui.theme.CordlyCrimson
import com.example.ui.theme.CordlyCyan
import com.example.ui.theme.CordlyEmerald
import com.example.ui.theme.CordlySurfaceDark
import com.example.ui.theme.CordlyTextPrimary
import com.example.ui.theme.CordlyTextSecondary
import kotlinx.coroutines.launch

@Composable
fun AddFriendDialog(
    searchState: UiState<LookupKey?>?,
    isBusy: Boolean,
    onSearch: (String) -> Unit,
    onSendRequest: (LookupKey) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        textContentColor = CordlyTextSecondary,
        title = { Text("Add a Friend", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your friend's Gmail or neov chat Friend Code (e.g. NEV-A1B2C3) to earn +200 P E Y!",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Gmail or Friend Code") },
                    singleLine = true,
                    trailingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_input")
                )

                Button(
                    onClick = { onSearch(query) },
                    enabled = query.isNotBlank() && searchState !is UiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_friend_button")
                ) {
                    if (searchState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Find Friend")
                }

                when (searchState) {
                    is UiState.Error -> {
                        Text(
                            text = searchState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CordlyCrimson
                        )
                    }
                    is UiState.Success -> {
                        val found = searchState.data
                        if (found != null) {
                            Surface(
                                color = CordlyCardSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CordlyEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    UserAvatar(
                                        displayName = found.displayName,
                                        photoUrl = found.photoUrl,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = found.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CordlyTextPrimary
                                        )
                                        Text(
                                            text = found.friendCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CordlyCyan
                                        )
                                    }
                                    Button(
                                        onClick = { onSendRequest(found) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = CordlyEmerald),
                                        modifier = Modifier.testTag("send_friend_request_button")
                                    ) {
                                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateServerDialog(
    isBusy: Boolean,
    onCreate: (name: String, description: String?, emoji: String, accentHex: String, iconUrl: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val emojis = listOf("🎮", "🎧", "🚀", "☕", "🎨", "📚", "⚽", "🔥")
    var selectedEmoji by remember { mutableStateOf(emojis.first()) }
    val accents = listOf("#5865F2", "#22D3EE", "#23A55A", "#F59E0B", "#EC4899")
    var selectedAccent by remember { mutableStateOf(accents.first()) }
    var pickedIconUrl by remember { mutableStateOf<String?>(null) }
    var isProcessingIcon by remember { mutableStateOf(false) }
    var iconError by remember { mutableStateOf<String?>(null) }

    val iconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            iconError = null
            isProcessingIcon = true
            scope.launch {
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(
                    context = context,
                    uri = uri,
                    maxWidth = 144,
                    maxHeight = 144,
                    maxBase64Bytes = 40_000
                )
                isProcessingIcon = false
                if (encoded != null) {
                    pickedIconUrl = encoded
                } else {
                    iconError = "Could not load selected image. Try another photo."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        textContentColor = CordlyTextSecondary,
        title = { Text("Create a Server", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServerIconView(
                        serverName = name.ifBlank { "Server" },
                        iconEmoji = selectedEmoji,
                        iconUrl = pickedIconUrl,
                        accentColor = parseHexColor(selectedAccent),
                        size = 56.dp,
                        cornerRadius = 16.dp
                    )
                    OutlinedButton(
                        onClick = {
                            iconPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingIcon,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("pick_create_server_icon_button")
                    ) {
                        if (isProcessingIcon) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CordlyCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when {
                                isProcessingIcon -> "Loading..."
                                pickedIconUrl != null -> "Change Photo"
                                else -> "Server Photo"
                            }
                        )
                    }
                }
                if (iconError != null) {
                    Text(
                        text = iconError.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CordlyCrimson
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    placeholder = { Text("e.g. Squad HQ (friends join by name!)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_server_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topic (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Fallback Emoji", style = MaterialTheme.typography.labelLarge, color = CordlyTextPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { emoji ->
                        val active = emoji == selectedEmoji && pickedIconUrl == null
                        Surface(
                            color = if (active) CordlyBlurple.copy(alpha = 0.3f) else CordlyCardSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = if (active) BorderStroke(1.5.dp, CordlyBlurple) else null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedEmoji = emoji
                                    pickedIconUrl = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = CordlyTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accents.forEach { hex ->
                        val color = parseHexColor(hex)
                        val active = hex == selectedAccent
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedAccent = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description, selectedEmoji, selectedAccent, pickedIconUrl) },
                enabled = name.trim().length >= 2 && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple),
                modifier = Modifier.testTag("confirm_create_server_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinServerDialog(
    isBusy: Boolean,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        textContentColor = CordlyTextSecondary,
        title = { Text("Join a Server", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Type your friend's exact Server Name (e.g. Squad HQ) or paste an invite code.")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Server Name or Invite Code") },
                    placeholder = { Text("e.g. Squad HQ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_server_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(query) },
                enabled = query.trim().length >= 2 && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CordlyCyan, contentColor = Color(0xFF082026)),
                modifier = Modifier.testTag("confirm_join_server_button")
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddChannelDialog(
    isBusy: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var channelName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CordlySurfaceDark,
        titleContentColor = CordlyTextPrimary,
        textContentColor = CordlyTextSecondary,
        title = { Text("Create Text Channel", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add a new #channel for your server members.")
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g. voice-chat-notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(channelName) },
                enabled = channelName.trim().isNotEmpty() && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple)
            ) {
                Text("Create Channel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
