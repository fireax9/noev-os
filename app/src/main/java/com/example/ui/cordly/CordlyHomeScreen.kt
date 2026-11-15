package com.example.ui.cordly

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.Friendship
import com.example.data.model.Server
import com.example.data.model.UserPrivate
import com.example.ui.auth.signOut
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
import kotlinx.coroutines.launch

@Composable
fun CordlyHomeScreen(
    viewModel: CordlyViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val ownProfileState by viewModel.ownProfile.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val serversState by viewModel.servers.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val homeTab by viewModel.homeTab.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val friendSearchState by viewModel.friendSearchState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val directMessagesState by viewModel.directMessages.collectAsStateWithLifecycle()
    val serverMessagesState by viewModel.serverMessages.collectAsStateWithLifecycle()
    val profileViewerTarget by viewModel.profileViewerTarget.collectAsStateWithLifecycle()
    val viewedPublicProfileState by viewModel.viewedPublicProfile.collectAsStateWithLifecycle()

    var wallpaperState by remember {
        mutableStateOf(AppAppearanceStore.peekCached() ?: WallpaperState())
    }
    LaunchedEffect(context) {
        wallpaperState = AppAppearanceStore.loadAsync(context)
    }
    val hasWallpaper = wallpaperState.mode != AppWallpaperMode.DEFAULT

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateServerDialog by remember { mutableStateOf(false) }
    var showJoinServerDialog by remember { mutableStateOf(false) }
    var showAddChannelForServer by remember { mutableStateOf<Server?>(null) }
    var settingsModalTab by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val ownProfile = (ownProfileState as? UiState.Success)?.data
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val servers = (serversState as? UiState.Success)?.data.orEmpty()

    val incomingRequests = remember(friendships, viewModel.currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientId == viewModel.currentUserId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppWallpaperBackground(wallpaperState = wallpaperState)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { _ ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 720.dp

                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ServerRail(
                            servers = servers,
                            activeDestination = destination,
                            pendingRequestCount = incomingRequests.size,
                            hasWallpaper = hasWallpaper,
                            onSelectHome = { viewModel.selectHome() },
                            onSelectServer = { viewModel.selectServer(it) },
                            onCreateServerClick = { showCreateServerDialog = true },
                            onJoinServerClick = { showJoinServerDialog = true }
                        )

                        WorkspaceSidebar(
                            modifier = Modifier.width(316.dp).fillMaxHeight(),
                            currentUserId = viewModel.currentUserId,
                            ownProfile = ownProfile,
                            destination = destination,
                            homeTab = homeTab,
                            hasWallpaper = hasWallpaper,
                            friendshipsState = friendshipsState,
                            conversationsState = conversationsState,
                            servers = servers,
                            onSetHomeTab = { viewModel.setHomeTab(it) },
                            onAddFriendClick = { showAddFriendDialog = true },
                            onCopyCode = { code ->
                                clipboardManager.setText(AnnotatedString(code))
                                scope.launch { snackbarHostState.showSnackbar("Copied: $code") }
                            },
                            onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                            onDeclineRequest = { viewModel.removeFriendship(it.id) },
                            onStartChatWithFriend = { viewModel.startChatWithFriend(it) },
                            onOpenConversation = { viewModel.openDirectChat(it.id) },
                            onOpenUserProfile = { uid, name, photo, onMsg ->
                                viewModel.openProfileViewer(uid, name, photo, onMsg)
                            },
                            onOpenChannel = { srv, ch -> viewModel.openServerChannel(srv.id, ch) },
                            onAddChannelClick = { showAddChannelForServer = it },
                            onUpdateServerIcon = { srv, url -> viewModel.updateServerIcon(srv, url) },
                            onImageError = { msg -> viewModel.showMessage(msg) },
                            onLeaveServer = { viewModel.leaveServer(it.id) },
                            onEditProfileClick = { settingsModalTab = 0 },
                            onOpenShopClick = { settingsModalTab = 1 },
                            onSignOutClick = { signOut(credentialManager, scope) }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            ActiveChatPane(
                                activeChat = activeChat,
                                showBackButton = false,
                                hasWallpaper = hasWallpaper,
                                currentUserId = viewModel.currentUserId,
                                conversations = conversations,
                                servers = servers,
                                directMessagesState = directMessagesState,
                                serverMessagesState = serverMessagesState,
                                onBack = { viewModel.closeActiveChat() },
                                onSendDirect = { conv, text, reply -> viewModel.sendDirectMessage(conv, text, reply) },
                                onDeleteDirect = { convId, msgId -> viewModel.deleteDirectMessage(convId, msgId) },
                                onSendServer = { srv, ch, text, reply -> viewModel.sendServerMessage(srv, ch, text, reply) },
                                onDeleteServer = { srvId, msgId -> viewModel.deleteServerMessage(srvId, msgId) },
                                onOpenUserProfile = { uid, name, photo ->
                                    viewModel.openProfileViewer(uid, name, photo, null)
                                }
                            )
                        }
                    }
                } else {
                    val currentChat = activeChat
                    if (currentChat != null) {
                        ActiveChatPane(
                            activeChat = currentChat,
                            showBackButton = true,
                            hasWallpaper = hasWallpaper,
                            currentUserId = viewModel.currentUserId,
                            conversations = conversations,
                            servers = servers,
                            directMessagesState = directMessagesState,
                            serverMessagesState = serverMessagesState,
                            onBack = { viewModel.closeActiveChat() },
                            onSendDirect = { conv, text, reply -> viewModel.sendDirectMessage(conv, text, reply) },
                            onDeleteDirect = { convId, msgId -> viewModel.deleteDirectMessage(convId, msgId) },
                            onSendServer = { srv, ch, text, reply -> viewModel.sendServerMessage(srv, ch, text, reply) },
                            onDeleteServer = { srvId, msgId -> viewModel.deleteServerMessage(srvId, msgId) },
                            onOpenUserProfile = { uid, name, photo ->
                                viewModel.openProfileViewer(uid, name, photo, null)
                            }
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ServerRail(
                                servers = servers,
                                activeDestination = destination,
                                pendingRequestCount = incomingRequests.size,
                                hasWallpaper = hasWallpaper,
                                onSelectHome = { viewModel.selectHome() },
                                onSelectServer = { viewModel.selectServer(it) },
                                onCreateServerClick = { showCreateServerDialog = true },
                                onJoinServerClick = { showJoinServerDialog = true }
                            )
                            WorkspaceSidebar(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                currentUserId = viewModel.currentUserId,
                                ownProfile = ownProfile,
                                destination = destination,
                                homeTab = homeTab,
                                hasWallpaper = hasWallpaper,
                                friendshipsState = friendshipsState,
                                conversationsState = conversationsState,
                                servers = servers,
                                onSetHomeTab = { viewModel.setHomeTab(it) },
                                onAddFriendClick = { showAddFriendDialog = true },
                                onCopyCode = { code ->
                                    clipboardManager.setText(AnnotatedString(code))
                                    scope.launch { snackbarHostState.showSnackbar("Copied: $code") }
                                },
                                onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                                onDeclineRequest = { viewModel.removeFriendship(it.id) },
                                onStartChatWithFriend = { viewModel.startChatWithFriend(it) },
                                onOpenConversation = { viewModel.openDirectChat(it.id) },
                                onOpenUserProfile = { uid, name, photo, onMsg ->
                                    viewModel.openProfileViewer(uid, name, photo, onMsg)
                                },
                                onOpenChannel = { srv, ch -> viewModel.openServerChannel(srv.id, ch) },
                                onAddChannelClick = { showAddChannelForServer = it },
                                onUpdateServerIcon = { srv, url -> viewModel.updateServerIcon(srv, url) },
                                onImageError = { msg -> viewModel.showMessage(msg) },
                                onLeaveServer = { viewModel.leaveServer(it.id) },
                                onEditProfileClick = { settingsModalTab = 0 },
                                onOpenShopClick = { settingsModalTab = 1 },
                                onSignOutClick = { signOut(credentialManager, scope) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            searchState = friendSearchState,
            isBusy = isBusy,
            onSearch = { viewModel.searchFriend(it) },
            onSendRequest = { target ->
                viewModel.sendFriendRequest(target) {
                    showAddFriendDialog = false
                }
            },
            onDismiss = {
                viewModel.clearFriendSearch()
                showAddFriendDialog = false
            }
        )
    }

    if (showCreateServerDialog) {
        CreateServerDialog(
            isBusy = isBusy,
            onCreate = { name, desc, emoji, accent, iconUrl ->
                viewModel.createServer(name, desc, emoji, accent, iconUrl) {
                    showCreateServerDialog = false
                }
            },
            onDismiss = { showCreateServerDialog = false }
        )
    }

    if (showJoinServerDialog) {
        JoinServerDialog(
            isBusy = isBusy,
            onJoin = { query ->
                viewModel.joinServerByInvite(query) {
                    showJoinServerDialog = false
                }
            },
            onDismiss = { showJoinServerDialog = false }
        )
    }

    val targetServerForChannel = showAddChannelForServer
    if (targetServerForChannel != null) {
        AddChannelDialog(
            isBusy = isBusy,
            onAdd = { channelName ->
                viewModel.addChannel(targetServerForChannel, channelName) {
                    showAddChannelForServer = null
                }
            },
            onDismiss = { showAddChannelForServer = null }
        )
    }

    val modalTab = settingsModalTab
    if (modalTab != null) {
        SettingsAndShopDialog(
            initialTab = modalTab,
            profile = ownProfile,
            wallpaperState = wallpaperState,
            isBusy = isBusy,
            onSelectWallpaperMode = { mode ->
                wallpaperState = AppAppearanceStore.saveMode(context, mode)
                viewModel.showMessage("Wallpaper: ${mode.label}")
            },
            onCustomWallpaperSaved = {
                wallpaperState = AppAppearanceStore.markCustomUpdated(context)
                viewModel.showMessage("Custom wallpaper applied!")
            },
            onSaveProfile = { status, custom, name, photoUrl, bannerUrl, clearBanner ->
                viewModel.updateStatus(status, custom, name, photoUrl, bannerUrl, clearBanner) {
                    settingsModalTab = null
                }
            },
            onQuickUpdateMedia = { photoUrl, bannerUrl, clearBanner, label ->
                viewModel.updateProfileMedia(photoUrl, bannerUrl, clearBanner, label)
            },
            onBuyFrame = { frame ->
                viewModel.purchaseAndEquipFrame(frame)
            },
            onEquipFrame = { frameId ->
                viewModel.equipFrame(frameId)
            },
            onBuyProfileEffect = { effect, videoUri, localOnly ->
                viewModel.purchaseAndEquipProfileEffect(effect, videoUri, localOnly)
            },
            onEquipProfileEffect = { effectId, videoUri, localOnly ->
                viewModel.equipProfileEffect(effectId, videoUri, localOnly)
            },
            onPreviewOwnProfile = {
                val me = ownProfile
                if (me != null) {
                    viewModel.openProfileViewer(me.userId, me.displayName, me.photoUrl, null)
                }
            },
            onSignOut = { signOut(credentialManager, scope) },
            onDismiss = { settingsModalTab = null }
        )
    }

    val activeViewer = profileViewerTarget
    if (activeViewer != null) {
        val loadedPublic = (viewedPublicProfileState as? UiState.Success)?.data
        val isProfileLoading = viewedPublicProfileState is UiState.Loading
        DiscordProfileSheetDialog(
            targetProfile = loadedPublic,
            fallbackName = activeViewer.fallbackName,
            fallbackPhoto = activeViewer.fallbackPhoto,
            fallbackUserId = activeViewer.userId,
            isOwnProfile = activeViewer.userId == viewModel.currentUserId,
            isLoading = isProfileLoading,
            onMessageUser = activeViewer.onMessageClick,
            onDismiss = { viewModel.closeProfileViewer() }
        )
    }
}

@Composable
private fun WorkspaceSidebar(
    currentUserId: String,
    ownProfile: UserPrivate?,
    destination: ActiveDestination,
    homeTab: Int,
    hasWallpaper: Boolean,
    friendshipsState: UiState<List<Friendship>>,
    conversationsState: UiState<List<Conversation>>,
    servers: List<Server>,
    onSetHomeTab: (Int) -> Unit,
    onAddFriendClick: () -> Unit,
    onCopyCode: (String) -> Unit,
    onAcceptRequest: (Friendship) -> Unit,
    onDeclineRequest: (Friendship) -> Unit,
    onStartChatWithFriend: (Friendship) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onOpenUserProfile: (uid: String, name: String, photo: String?, onMessage: (() -> Unit)?) -> Unit,
    onOpenChannel: (Server, String) -> Unit,
    onAddChannelClick: (Server) -> Unit,
    onUpdateServerIcon: (Server, String) -> Unit,
    onImageError: (String) -> Unit,
    onLeaveServer: (Server) -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CordlySurfaceDark.copy(alpha = if (hasWallpaper) 0.72f else 1f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (destination) {
                is ActiveDestination.Home -> {
                    HomeHubContent(
                        currentUserId = currentUserId,
                        ownProfile = ownProfile,
                        homeTab = homeTab,
                        friendshipsState = friendshipsState,
                        conversationsState = conversationsState,
                        onSetHomeTab = onSetHomeTab,
                        onAddFriendClick = onAddFriendClick,
                        onOpenShopClick = onOpenShopClick,
                        onCopyCode = onCopyCode,
                        onAcceptRequest = onAcceptRequest,
                        onDeclineRequest = onDeclineRequest,
                        onStartChatWithFriend = onStartChatWithFriend,
                        onOpenConversation = onOpenConversation,
                        onOpenUserProfile = onOpenUserProfile
                    )
                }
                is ActiveDestination.ServerSpace -> {
                    val server = servers.firstOrNull { it.id == destination.serverId }
                    if (server != null) {
                        ServerChannelsContent(
                            server = server,
                            currentUserId = currentUserId,
                            selectedChannel = destination.channelName,
                            onCopyInvite = onCopyCode,
                            onOpenChannel = { ch -> onOpenChannel(server, ch) },
                            onAddChannelClick = { onAddChannelClick(server) },
                            onUpdateServerIcon = { url -> onUpdateServerIcon(server, url) },
                            onImageError = onImageError,
                            onLeaveServer = { onLeaveServer(server) }
                        )
                    } else {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.Group,
                            title = "Server not found",
                            subtitle = "Select another server or return to Direct Messages.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        UserDockBar(
            profile = ownProfile,
            onEditStatusClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onSignOutClick = onSignOutClick
        )
    }
}

@Composable
private fun HomeHubContent(
    currentUserId: String,
    ownProfile: UserPrivate?,
    homeTab: Int,
    friendshipsState: UiState<List<Friendship>>,
    conversationsState: UiState<List<Conversation>>,
    onSetHomeTab: (Int) -> Unit,
    onAddFriendClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onCopyCode: (String) -> Unit,
    onAcceptRequest: (Friendship) -> Unit,
    onDeclineRequest: (Friendship) -> Unit,
    onStartChatWithFriend: (Friendship) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onOpenUserProfile: (uid: String, name: String, photo: String?, onMessage: (() -> Unit)?) -> Unit
) {
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()

    val incoming = remember(friendships, currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientId == currentUserId }
    }
    val outgoing = remember(friendships, currentUserId) {
        friendships.filter { it.status == "pending" && it.requesterId == currentUserId }
    }
    val accepted = remember(friendships) {
        friendships.filter { it.status == "accepted" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Branded Header: neov chat + P E Y Shop + Add Friend
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.neov_icon_fg,
                        contentDescription = "neov chat emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "neov chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = CordlyTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = CordlyAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CordlyAmber.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenShopClick)
                        .minimumInteractiveComponentSize()
                        .testTag("open_pey_shop_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ShoppingBag,
                            contentDescription = "Open P E Y Shop",
                            tint = CordlyAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Shop",
                            style = MaterialTheme.typography.labelLarge,
                            color = CordlyAmber
                        )
                    }
                }

                Button(
                    onClick = onAddFriendClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CordlyBlurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_friend_button")
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Discord-style Shareable Friend Code + P E Y Card
        if (ownProfile != null) {
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CordlyBlurple.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!ownProfile.bannerUrl.isNullOrBlank()) {
                        CordlyRemoteOrDataImage(
                            source = ownProfile.bannerUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xFF0B0E1A).copy(alpha = 0.65f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CordlyBlurple.copy(alpha = 0.18f), CordlyCyan.copy(alpha = 0.10f))
                                    )
                                )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Code: ${ownProfile.friendCode}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CordlyCyan
                            )
                            Text(
                                text = "Balance: ${ownProfile.peyPoints} P E Y (+200 / friend)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CordlyAmber,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onCopyCode(ownProfile.friendCode) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy friend code",
                                tint = CordlyCyan
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Segmented Tabs: Messages | Friends
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            HubTabPill(
                label = "Messages (${conversations.size})",
                selected = homeTab == 0,
                badgeCount = 0,
                testTag = "tab_messages",
                onClick = { onSetHomeTab(0) },
                modifier = Modifier.weight(1f)
            )
            HubTabPill(
                label = "Friends (${accepted.size})",
                selected = homeTab == 1,
                badgeCount = incoming.size,
                testTag = "tab_friends",
                onClick = { onSetHomeTab(1) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = CordlyDivider, thickness = 1.dp)

        if (homeTab == 0) {
            when (conversationsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CordlyBlurple)
                    }
                }
                is UiState.Error -> {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Forum,
                        title = "Unable to load messages",
                        subtitle = conversationsState.message
                    )
                }
                is UiState.Success -> {
                    if (conversations.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.Forum,
                            title = "No Direct Messages yet",
                            subtitle = "Add a friend using their Gmail or Friend Code to earn +200 P E Y and chat in real time.",
                            actionLabel = "Add a Friend",
                            onAction = onAddFriendClick
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(conversations, key = { it.id }) { conv ->
                                val isA = conv.participantAId == currentUserId
                                val peerUid = if (isA) conv.participantBId else conv.participantAId
                                val peerName = if (isA) conv.participantBName else conv.participantAName
                                val peerPhoto = if (isA) conv.participantBPhoto else conv.participantAPhoto
                                ConversationCard(
                                    conversation = conv,
                                    currentUserId = currentUserId,
                                    onAvatarClick = {
                                        onOpenUserProfile(peerUid, peerName, peerPhoto) {
                                            onOpenConversation(conv)
                                        }
                                    },
                                    onClick = { onOpenConversation(conv) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when (friendshipsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CordlyBlurple)
                    }
                }
                is UiState.Error -> {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Group,
                        title = "Unable to load friends",
                        subtitle = friendshipsState.message
                    )
                }
                is UiState.Success -> {
                    if (friendships.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.PersonAdd,
                            title = "Your Friends list is empty",
                            subtitle = "Add a friend by Gmail or Friend Code to earn +200 P E Y points for frame effects!",
                            actionLabel = "Find Friend",
                            onAction = onAddFriendClick
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (incoming.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "INCOMING REQUESTS (${incoming.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CordlyCyan
                                    )
                                }
                                items(incoming, key = { "in_${it.id}" }) { req ->
                                    FriendRequestCard(
                                        friendship = req,
                                        isIncoming = true,
                                        onAvatarClick = {
                                            onOpenUserProfile(req.requesterId, req.requesterName, req.requesterPhoto, null)
                                        },
                                        onAccept = { onAcceptRequest(req) },
                                        onDecline = { onDeclineRequest(req) }
                                    )
                                }
                            }

                            if (accepted.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "ALL FRIENDS (${accepted.size}) • Tap card to view 5s profile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CordlyTextSecondary
                                    )
                                }
                                items(accepted, key = { "fr_${it.id}" }) { friend ->
                                    val isRequester = friend.requesterId == currentUserId
                                    val peerId = if (isRequester) friend.recipientId else friend.requesterId
                                    val peerName = if (isRequester) friend.recipientName else friend.requesterName
                                    val peerPhoto = if (isRequester) friend.recipientPhoto else friend.requesterPhoto
                                    AcceptedFriendCard(
                                        friendship = friend,
                                        currentUserId = currentUserId,
                                        onViewProfileClick = {
                                            onOpenUserProfile(peerId, peerName, peerPhoto) {
                                                onStartChatWithFriend(friend)
                                            }
                                        },
                                        onMessageClick = { onStartChatWithFriend(friend) },
                                        onRemoveClick = { onDeclineRequest(friend) }
                                    )
                                }
                            }

                            if (outgoing.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "PENDING SENT (${outgoing.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CordlyTextMuted
                                    )
                                }
                                items(outgoing, key = { "out_${it.id}" }) { req ->
                                    FriendRequestCard(
                                        friendship = req,
                                        isIncoming = false,
                                        onAvatarClick = {
                                            onOpenUserProfile(req.recipientId, req.recipientName, req.recipientPhoto, null)
                                        },
                                        onAccept = {},
                                        onDecline = { onDeclineRequest(req) }
                                    )
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
private fun HubTabPill(
    label: String,
    selected: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) CordlyElevatedSurface else CordlyCardSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, CordlyBlurple.copy(alpha = 0.6f)) else null,
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) CordlyTextPrimary else CordlyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = CordlyCrimson,
                    shape = CircleShape,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    currentUserId: String,
    onAvatarClick: () -> Unit,
    onClick: () -> Unit
) {
    val isA = conversation.participantAId == currentUserId
    val peerName = if (isA) conversation.participantBName else conversation.participantAName
    val peerPhoto = if (isA) conversation.participantBPhoto else conversation.participantAPhoto

    Surface(
        color = CordlyCardSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("conversation_card_${conversation.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            UserAvatar(
                displayName = peerName,
                photoUrl = peerPhoto,
                status = "online",
                size = 44.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = conversation.lastMessageText ?: "Tap to start chatting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CordlyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    friendship: Friendship,
    isIncoming: Boolean,
    onAvatarClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val name = if (isIncoming) friendship.requesterName else friendship.recipientName
    val code = if (isIncoming) friendship.requesterCode else friendship.recipientCode
    val photo = if (isIncoming) friendship.requesterPhoto else friendship.recipientPhoto

    Surface(
        color = CordlyCardSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isIncoming) CordlyCyan.copy(alpha = 0.35f) else CordlyDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            UserAvatar(
                displayName = name,
                photoUrl = photo,
                size = 40.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (isIncoming) "Incoming • $code" else "Sent • $code",
                    style = MaterialTheme.typography.labelSmall,
                    color = CordlyTextSecondary
                )
            }
            if (isIncoming) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("accept_friend_${friendship.id}")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Accept friend request", tint = CordlyEmerald)
                }
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Decline or cancel", tint = CordlyCrimson)
            }
        }
    }
}

@Composable
private fun AcceptedFriendCard(
    friendship: Friendship,
    currentUserId: String,
    onViewProfileClick: () -> Unit,
    onMessageClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val isRequester = friendship.requesterId == currentUserId
    val name = if (isRequester) friendship.recipientName else friendship.requesterName
    val code = if (isRequester) friendship.recipientCode else friendship.requesterCode
    val photo = if (isRequester) friendship.recipientPhoto else friendship.requesterPhoto

    Surface(
        color = CordlyCardSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onViewProfileClick)
            .testTag("friend_profile_card_${friendship.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            UserAvatar(displayName = name, photoUrl = photo, status = "online", size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = CordlyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(code, style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
            }
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("message_friend_${friendship.id}")
            ) {
                Icon(Icons.Rounded.Forum, contentDescription = "Message $name", tint = CordlyBlurple)
            }
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove friend", tint = CordlyTextMuted)
            }
        }
    }
}

@Composable
private fun ServerChannelsContent(
    server: Server,
    currentUserId: String,
    selectedChannel: String,
    onCopyInvite: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAddChannelClick: () -> Unit,
    onUpdateServerIcon: (String) -> Unit,
    onImageError: (String) -> Unit,
    onLeaveServer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOwner = server.ownerId == currentUserId
    val accent = parseHexColor(server.accentHex)
    var isUpdatingPhoto by remember(server.id) { mutableStateOf(false) }

    val serverPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUpdatingPhoto = true
            scope.launch {
                val encoded = ImageEncodingHelper.encodePickedImageToDataUri(
                    context = context,
                    uri = uri,
                    maxWidth = 144,
                    maxHeight = 144,
                    maxBase64Bytes = 40_000
                )
                isUpdatingPhoto = false
                if (encoded != null) {
                    onUpdateServerIcon(encoded)
                } else {
                    onImageError("Could not load selected server image. Try another photo.")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Server Banner Header
        Surface(
            color = CordlyCardSurface.copy(alpha = 0.88f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.25f), CordlySurfaceDark.copy(alpha = 0.90f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (isOwner && !isUpdatingPhoto) {
                                    Modifier.clickable {
                                        serverPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("change_server_icon_button")
                    ) {
                        ServerIconView(
                            serverName = server.name,
                            iconEmoji = server.iconEmoji,
                            iconUrl = server.iconUrl,
                            accentColor = accent,
                            size = 48.dp,
                            cornerRadius = 14.dp
                        )
                        if (isOwner) {
                            Surface(
                                color = CordlyBlurple,
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, CordlySurfaceDark),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUpdatingPhoto) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 1.8.dp,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.CameraAlt,
                                            contentDescription = "Change server image",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isUpdatingPhoto -> "Updating server image..."
                                isOwner -> "${server.memberUids.size} member(s) • Tap icon to change photo"
                                else -> "${server.memberUids.size} member(s)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CordlyTextSecondary
                        )
                    }
                    if (!isOwner) {
                        IconButton(onClick = onLeaveServer, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = "Leave server",
                                tint = CordlyCrimson
                            )
                        }
                    }
                }
                if (!server.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = server.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CordlyTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = CordlyElevatedSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopyInvite(server.name) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Join by Name: \"${server.name}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = CordlyCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Copy", style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
                    }
                }
            }
        }

        HorizontalDivider(color = CordlyDivider, thickness = 1.dp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "TEXT CHANNELS",
                style = MaterialTheme.typography.labelSmall,
                color = CordlyTextSecondary
            )
            if (isOwner) {
                IconButton(
                    onClick = onAddChannelClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add channel", tint = CordlyTextPrimary)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(server.channels, key = { it }) { channel ->
                val isSelected = channel == selectedChannel
                Surface(
                    color = if (isSelected) CordlyElevatedSurface else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenChannel(channel) }
                        .testTag("channel_row_$channel")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tag,
                            contentDescription = null,
                            tint = if (isSelected) CordlyCyan else CordlyTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = channel,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) CordlyTextPrimary else CordlyTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChatPane(
    activeChat: ActiveChatTarget?,
    showBackButton: Boolean,
    hasWallpaper: Boolean,
    currentUserId: String,
    conversations: List<Conversation>,
    servers: List<Server>,
    directMessagesState: UiState<List<com.example.data.model.DirectMessage>>,
    serverMessagesState: UiState<List<com.example.data.model.ServerMessage>>,
    onBack: () -> Unit,
    onSendDirect: (Conversation, String, String?) -> Unit,
    onDeleteDirect: (String, String) -> Unit,
    onSendServer: (Server, String, String, String?) -> Unit,
    onDeleteServer: (String, String) -> Unit,
    onOpenUserProfile: (userId: String, name: String, photoUrl: String?) -> Unit
) {
    when (activeChat) {
        is ActiveChatTarget.DirectChat -> {
            val conv = conversations.firstOrNull { it.id == activeChat.conversationId }
            val peerId = if (conv != null) {
                if (conv.participantAId == currentUserId) conv.participantBId else conv.participantAId
            } else {
                ""
            }
            val peerName = if (conv != null) {
                if (conv.participantAId == currentUserId) conv.participantBName else conv.participantAName
            } else {
                "Direct Message"
            }
            val peerPhoto = if (conv != null) {
                if (conv.participantAId == currentUserId) conv.participantBPhoto else conv.participantAPhoto
            } else {
                null
            }

            val mappedMessages: UiState<List<ChatMessageItem>> = when (directMessagesState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(directMessagesState.message)
                is UiState.Success -> UiState.Success(
                    directMessagesState.data.map {
                        ChatMessageItem(
                            id = it.id,
                            senderId = it.senderId,
                            senderName = it.senderName,
                            senderPhoto = it.senderPhoto,
                            text = it.text,
                            replyToText = it.replyToText,
                            createdAt = it.createdAt
                        )
                    }
                )
            }

            ChatScreen(
                title = peerName,
                subtitle = "Direct Message",
                isDirectMessage = true,
                showBackButton = showBackButton,
                currentUserId = currentUserId,
                messagesState = mappedMessages,
                hasWallpaper = hasWallpaper,
                onBack = onBack,
                onSendMessage = { text, reply ->
                    if (conv != null) onSendDirect(conv, text, reply)
                },
                onDeleteMessage = { msgId ->
                    onDeleteDirect(activeChat.conversationId, msgId)
                },
                onUserProfileClick = onOpenUserProfile,
                onHeaderProfileClick = if (peerId.isNotBlank()) {
                    { onOpenUserProfile(peerId, peerName, peerPhoto) }
                } else {
                    null
                }
            )
        }
        is ActiveChatTarget.ServerChannel -> {
            val server = servers.firstOrNull { it.id == activeChat.serverId }
            val mappedMessages: UiState<List<ChatMessageItem>> = when (serverMessagesState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(serverMessagesState.message)
                is UiState.Success -> UiState.Success(
                    serverMessagesState.data.map {
                        ChatMessageItem(
                            id = it.id,
                            senderId = it.senderId,
                            senderName = it.senderName,
                            senderPhoto = it.senderPhoto,
                            text = it.text,
                            replyToText = it.replyToText,
                            createdAt = it.createdAt
                        )
                    }
                )
            }

            ChatScreen(
                title = activeChat.channelName,
                subtitle = server?.name,
                isDirectMessage = false,
                showBackButton = showBackButton,
                currentUserId = currentUserId,
                messagesState = mappedMessages,
                hasWallpaper = hasWallpaper,
                onBack = onBack,
                onSendMessage = { text, reply ->
                    if (server != null) onSendServer(server, activeChat.channelName, text, reply)
                },
                onDeleteMessage = { msgId ->
                    onDeleteServer(activeChat.serverId, msgId)
                },
                onUserProfileClick = onOpenUserProfile
            )
        }
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (hasWallpaper) Color.Transparent else CordlyCanvasBg),
                contentAlignment = Alignment.Center
            ) {
                EmptyPlaceholder(
                    icon = Icons.Rounded.Forum,
                    title = "Select a Friend or Channel",
                    subtitle = "Choose a Direct Message or a Server #channel from the sidebar to start chatting."
                )
            }
        }
    }
}
