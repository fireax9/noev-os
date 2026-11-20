package com.example.ui.cordly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.Friendship
import com.example.data.model.Server
import com.example.data.model.UserPrivate
import com.example.data.repository.CordlyRepository
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
import kotlinx.coroutines.delay
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
    val strings = LocalAppStrings.current
    val profileViewerTarget by viewModel.profileViewerTarget.collectAsStateWithLifecycle()
    val viewedPublicProfileState by viewModel.viewedPublicProfile.collectAsStateWithLifecycle()
    val activeCalls by viewModel.activeCalls.collectAsStateWithLifecycle()
    val callStreams by viewModel.callStreams.collectAsStateWithLifecycle()
    val joinedCallId by viewModel.joinedCallId.collectAsStateWithLifecycle()
    val ultraDirectoryMembers by viewModel.ultraDirectoryMembers.collectAsStateWithLifecycle()
    val customQuests by viewModel.customQuests.collectAsStateWithLifecycle()
    val developerTickets by viewModel.developerTickets.collectAsStateWithLifecycle()
    val welcomeBanner by viewModel.welcomeBanner.collectAsStateWithLifecycle()

    var isCallMinimized by remember { mutableStateOf(false) }
    var hasSeenJoinedCall by remember(joinedCallId) { mutableStateOf(false) }
    LaunchedEffect(joinedCallId) {
        isCallMinimized = false
    }

    val joinedCall = remember(activeCalls, joinedCallId) {
        joinedCallId?.let { id -> activeCalls.firstOrNull { it.id == id } }
    }
    LaunchedEffect(joinedCall, joinedCallId) {
        if (joinedCall != null) {
            hasSeenJoinedCall = true
        } else if (hasSeenJoinedCall && joinedCallId != null) {
            viewModel.leaveCurrentCallOnly()
        }
    }

    val incomingCall = remember(activeCalls, viewModel.currentUserId, joinedCallId) {
        activeCalls.firstOrNull {
            it.status == "ringing" &&
                it.callerId != viewModel.currentUserId &&
                it.id != joinedCallId
        }
    }

    // Android 13+ notification permission + channels setup
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }
    var notifPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(context) {
        CordlyNotificationHelper.ensureChannels(context)
        if (!notifPrompted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPrompted = true
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Auto-dismiss welcome / welcome-back banner
    LaunchedEffect(welcomeBanner) {
        if (welcomeBanner != WelcomeBootstrapState.Hidden) {
            delay(4500L)
            viewModel.dismissWelcomeBanner()
        }
    }

    var wallpaperState by remember {
        mutableStateOf(AppAppearanceStore.peekCached() ?: WallpaperState())
    }
    var layoutPrefs by remember {
        mutableStateOf(AppAppearanceStore.loadLayoutPrefs(context))
    }
    var currentLogoVariant by remember {
        mutableStateOf(AppAppearanceStore.loadLogoVariant(context))
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
    var showShopDialog by remember { mutableStateOf(false) }
    var showQuestsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val ownProfile = (ownProfileState as? UiState.Success)?.data
    val effectiveRank = remember(ownProfile?.rank, ownProfile?.rankExpiresAtMs) {
        CordlyRepository.effectiveRank(ownProfile)
    }
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val acceptedFriends = remember(friendships) {
        friendships.filter { it.status == "accepted" }
    }
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val servers = (serversState as? UiState.Success)?.data.orEmpty()

    val incomingRequests = remember(friendships, viewModel.currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientId == viewModel.currentUserId }
    }

    // Play incoming message sound + post Android notification when a friend sends a new message
    val seenConvMessages = remember { mutableStateMapOf<String, String>() }
    var convTrackerSeeded by remember { mutableStateOf(false) }
    LaunchedEffect(conversations) {
        if (conversationsState !is UiState.Success) return@LaunchedEffect
        if (!convTrackerSeeded) {
            conversations.forEach { c ->
                seenConvMessages[c.id] = c.lastMessageText.orEmpty()
            }
            convTrackerSeeded = true
            return@LaunchedEffect
        }
        conversations.forEach { c ->
            val currentMsg = c.lastMessageText.orEmpty()
            val prevMsg = seenConvMessages[c.id]
            if (currentMsg.isNotBlank() && currentMsg != prevMsg) {
                seenConvMessages[c.id] = currentMsg
                if (c.lastMessageSenderId != viewModel.currentUserId) {
                    SoundEffectsManager.play(AppSoundEffect.MESSAGE_IN)
                    val isCurrentlyViewing = (activeChat as? ActiveChatTarget.DirectChat)?.conversationId == c.id
                    if (!isCurrentlyViewing) {
                        val isA = c.participantAId == viewModel.currentUserId
                        val senderName = if (isA) c.participantBName else c.participantAName
                        CordlyNotificationHelper.notifyIncomingMessage(context, senderName, currentMsg)
                    }
                }
            }
        }
    }

    LaunchedEffect(incomingCall?.id) {
        incomingCall?.let { call ->
            CordlyNotificationHelper.notifyIncomingCall(
                context = context,
                callerName = call.callerName,
                isVideo = call.callType == "video"
            )
        }
    }

    val inviteableFriends = remember(acceptedFriends, joinedCall, viewModel.currentUserId) {
        val currentParticipants = joinedCall?.participantIds.orEmpty().toSet()
        acceptedFriends.mapNotNull { fr ->
            val isReq = fr.requesterId == viewModel.currentUserId
            val peerId = if (isReq) fr.recipientId else fr.requesterId
            if (currentParticipants.contains(peerId)) {
                null
            } else {
                InviteableFriend(
                    uid = peerId,
                    name = if (isReq) fr.recipientName else fr.requesterName,
                    photoUrl = if (isReq) fr.recipientPhoto else fr.requesterPhoto
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppWallpaperBackground(wallpaperState = wallpaperState)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { _ ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 720.dp

                val railComposable: @Composable () -> Unit = {
                    ServerRail(
                        servers = servers,
                        activeDestination = destination,
                        pendingRequestCount = incomingRequests.size,
                        hasWallpaper = hasWallpaper,
                        onSelectHome = { viewModel.selectHome() },
                        onOpenQuestsClick = { showQuestsDialog = true },
                        onSelectServer = { viewModel.selectServer(it) },
                        onCreateServerClick = { showCreateServerDialog = true },
                        onJoinServerClick = { showJoinServerDialog = true }
                    )
                }

                val sidebarComposable: @Composable (Modifier) -> Unit = { sidebarMod ->
                    WorkspaceSidebar(
                        modifier = sidebarMod,
                        currentUserId = viewModel.currentUserId,
                        ownProfile = ownProfile,
                        effectiveRank = effectiveRank,
                        destination = destination,
                        homeTab = homeTab,
                        hasWallpaper = hasWallpaper,
                        friendshipsState = friendshipsState,
                        conversationsState = conversationsState,
                        servers = servers,
                        activeCalls = activeCalls,
                        onSetHomeTab = { viewModel.setHomeTab(it) },
                        onAddFriendClick = { showAddFriendDialog = true },
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            scope.launch { snackbarHostState.showSnackbar("${strings.copiedToast}: $code") }
                        },
                        onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                        onDeclineRequest = { viewModel.removeFriendship(it.id) },
                        onStartChatWithFriend = { viewModel.startChatWithFriend(it) },
                        onStartVoiceCallWithFriend = { friend ->
                            val isReq = friend.requesterId == viewModel.currentUserId
                            val peerId = if (isReq) friend.recipientId else friend.requesterId
                            val peerName = if (isReq) friend.recipientName else friend.requesterName
                            val peerPhoto = if (isReq) friend.recipientPhoto else friend.requesterPhoto
                            val convId = CordlyRepository.deterministicPairId(viewModel.currentUserId, peerId)
                            viewModel.startDirectCall(peerId, peerName, peerPhoto, convId, "voice")
                        },
                        onOpenConversation = { viewModel.openDirectChat(it.id) },
                        onOpenUserProfile = { uid, name, photo, onMsg ->
                            viewModel.openProfileViewer(uid, name, photo, onMsg)
                        },
                        onOpenChannel = { srv, ch -> viewModel.openServerChannel(srv.id, ch) },
                        onAddChannelClick = { showAddChannelForServer = it },
                        onUpdateServerIcon = { srv, url -> viewModel.updateServerIcon(srv, url) },
                        onKickMember = { srv, uid -> viewModel.removeMemberFromServer(srv, uid) },
                        onStartServerCall = { srv, ch, callType ->
                            viewModel.startOrJoinServerCall(srv, ch, callType)
                        },
                        onJoinOngoingCall = { call -> viewModel.joinOngoingCall(call) },
                        onImageError = { msg -> viewModel.showMessage(msg) },
                        onLeaveServer = { viewModel.leaveServer(it.id) },
                        onEditProfileClick = { settingsModalTab = 0 },
                        onOpenShopClick = { showShopDialog = true },
                        onSignOutClick = { signOut(credentialManager, scope) }
                    )
                }

                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (!layoutPrefs.railOnRight) railComposable()
                        sidebarComposable(Modifier.width(316.dp).fillMaxHeight())
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            ActiveChatPane(
                                activeChat = activeChat,
                                showBackButton = false,
                                hasWallpaper = hasWallpaper,
                                currentUserId = viewModel.currentUserId,
                                currentUserRank = effectiveRank,
                                compactCards = layoutPrefs.compactMessageCards,
                                conversations = conversations,
                                servers = servers,
                                directMessagesState = directMessagesState,
                                serverMessagesState = serverMessagesState,
                                onBack = { viewModel.closeActiveChat() },
                                onSendDirect = { conv, text, reply, type, data, dur ->
                                    SoundEffectsManager.play(AppSoundEffect.MESSAGE_OUT)
                                    viewModel.sendDirectMessage(conv, text, reply, type, data, dur)
                                },
                                onDeleteDirect = { convId, msgId, forEveryone ->
                                    viewModel.deleteDirectMessage(convId, msgId, forEveryone)
                                },
                                onSendServer = { srv, ch, text, reply, type, data, dur ->
                                    SoundEffectsManager.play(AppSoundEffect.MESSAGE_OUT)
                                    viewModel.sendServerMessage(srv, ch, text, reply, type, data, dur)
                                },
                                onDeleteServer = { srvId, msgId, forEveryone ->
                                    viewModel.deleteServerMessage(srvId, msgId, forEveryone)
                                },
                                onOpenUserProfile = { uid, name, photo ->
                                    viewModel.openProfileViewer(uid, name, photo, null)
                                },
                                onStartDirectCall = { calleeId, calleeName, calleePhoto, convId, callType ->
                                    viewModel.startDirectCall(calleeId, calleeName, calleePhoto, convId, callType)
                                },
                                onStartServerCall = { srv, ch, callType ->
                                    viewModel.startOrJoinServerCall(srv, ch, callType)
                                },
                                onStatusToast = { viewModel.showMessage(it) }
                            )
                        }
                        if (layoutPrefs.railOnRight) railComposable()
                    }
                } else {
                    val currentChat = activeChat
                    if (currentChat != null) {
                        ActiveChatPane(
                            activeChat = currentChat,
                            showBackButton = true,
                            hasWallpaper = hasWallpaper,
                            currentUserId = viewModel.currentUserId,
                            currentUserRank = effectiveRank,
                            compactCards = layoutPrefs.compactMessageCards,
                            conversations = conversations,
                            servers = servers,
                            directMessagesState = directMessagesState,
                            serverMessagesState = serverMessagesState,
                            onBack = { viewModel.closeActiveChat() },
                            onSendDirect = { conv, text, reply, type, data, dur ->
                                SoundEffectsManager.play(AppSoundEffect.MESSAGE_OUT)
                                viewModel.sendDirectMessage(conv, text, reply, type, data, dur)
                            },
                            onDeleteDirect = { convId, msgId, forEveryone ->
                                viewModel.deleteDirectMessage(convId, msgId, forEveryone)
                            },
                            onSendServer = { srv, ch, text, reply, type, data, dur ->
                                SoundEffectsManager.play(AppSoundEffect.MESSAGE_OUT)
                                viewModel.sendServerMessage(srv, ch, text, reply, type, data, dur)
                            },
                            onDeleteServer = { srvId, msgId, forEveryone ->
                                viewModel.deleteServerMessage(srvId, msgId, forEveryone)
                            },
                            onOpenUserProfile = { uid, name, photo ->
                                viewModel.openProfileViewer(uid, name, photo, null)
                            },
                            onStartDirectCall = { calleeId, calleeName, calleePhoto, convId, callType ->
                                viewModel.startDirectCall(calleeId, calleeName, calleePhoto, convId, callType)
                            },
                            onStartServerCall = { srv, ch, callType ->
                                viewModel.startOrJoinServerCall(srv, ch, callType)
                            },
                            onStatusToast = { viewModel.showMessage(it) }
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (!layoutPrefs.railOnRight) railComposable()
                            sidebarComposable(Modifier.weight(1f).fillMaxHeight())
                            if (layoutPrefs.railOnRight) railComposable()
                        }
                    }
                }
            }
        }

        // Welcome / Welcome-Back, Incoming Call & Minimized Ongoing Call Floating Banners
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            AnimatedVisibility(
                visible = welcomeBanner != WelcomeBootstrapState.Hidden,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                WelcomeStatusBanner(
                    state = welcomeBanner,
                    onDismiss = { viewModel.dismissWelcomeBanner() }
                )
            }

            AnimatedVisibility(visible = incomingCall != null) {
                incomingCall?.let { call ->
                    IncomingCallBanner(
                        call = call,
                        onAnswerVoice = { viewModel.answerCall(call, withVideo = false) },
                        onAnswerVideo = { viewModel.answerCall(call, withVideo = true) },
                        onDecline = { viewModel.endOrDeclineCall(call, "declined") }
                    )
                }
            }

            AnimatedVisibility(visible = joinedCall != null && isCallMinimized) {
                joinedCall?.let { call ->
                    Surface(
                        color = CordlyEmerald.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CordlyEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isCallMinimized = false }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (call.callType == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                                contentDescription = null,
                                tint = CordlyEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.connectedCallStatus}: ${call.calleeName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CordlyTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (call.targetType == "dm" || call.callerId == viewModel.currentUserId) {
                                        viewModel.endOrDeclineCall(call, "ended")
                                    } else {
                                        viewModel.leaveCurrentCallOnly()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.CallEnd, contentDescription = strings.endCall, tint = CordlyCrimson)
                            }
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

    // Isolated Settings & Languages Modal (NO Shop here)
    val modalTab = settingsModalTab
    if (modalTab != null) {
        SettingsAndShopDialog(
            initialTab = modalTab,
            profile = ownProfile,
            wallpaperState = wallpaperState,
            layoutPrefs = layoutPrefs,
            currentLogoVariant = currentLogoVariant,
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
            onUpdateProfileThemeColor = { hex ->
                viewModel.updateProfileThemeColor(hex)
            },
            onUpdateAvatarVideo = { dataUri ->
                viewModel.updateAvatarVideo(dataUri)
            },
            onUpdateAppLogo = { variant ->
                currentLogoVariant = AppAppearanceStore.applyAppLogoVariant(context, variant)
                viewModel.showMessage("Launcher icon updated: ${variant.titleEn}")
            },
            onUpdateLayoutPrefs = { nextPrefs ->
                layoutPrefs = AppAppearanceStore.saveLayoutPrefs(context, nextPrefs)
            },
            onPreviewOwnProfile = {
                val me = ownProfile
                if (me != null) {
                    viewModel.openProfileViewer(me.userId, me.displayName, me.photoUrl, null)
                }
            },
            onOpenShopForRanks = {
                settingsModalTab = null
                showShopDialog = true
            },
            onSignOut = { signOut(credentialManager, scope) },
            onDismiss = { settingsModalTab = null }
        )
    }

    // Isolated Standalone P E Y Shop Modal (Ranks, Circular Avatar Frames, Full Card Frames, Profile Videos)
    if (showShopDialog) {
        PeyShopDialog(
            profile = ownProfile,
            isBusy = isBusy,
            onActivateFreeTrial = { tier -> viewModel.activateFreeTrial(tier) },
            onPurchaseRank = { tier -> viewModel.purchaseRank(tier) },
            onBuyFrame = { frame -> viewModel.purchaseAndEquipFrame(frame) },
            onEquipFrame = { frameId -> viewModel.equipFrame(frameId) },
            onBuyCardFrame = { cardFrame -> viewModel.purchaseAndEquipCardFrame(cardFrame) },
            onEquipCardFrame = { cardFrameId -> viewModel.equipCardFrame(cardFrameId) },
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
            onDismiss = { showShopDialog = false }
        )
    }

    // Standalone Quests & Ultra Directory Modal ("المهام")
    if (showQuestsDialog) {
        QuestsAndUltraDialog(
            profile = ownProfile,
            acceptedFriendsCount = acceptedFriends.size,
            ultraDirectoryMembers = ultraDirectoryMembers,
            customQuests = customQuests,
            developerTickets = developerTickets,
            isBusy = isBusy,
            onClaimDailyReward = { viewModel.claimDailyReward() },
            onClaimChannelReward = { enteredChannel -> viewModel.claimChannelReward(enteredChannel) },
            onCreateCustomQuest = { title, desc, type, target, rewardPerStep ->
                viewModel.createCustomQuest(title, desc, type, target, rewardPerStep)
            },
            onClaimCustomQuest = { quest -> viewModel.claimCustomQuest(quest, acceptedFriends.size) },
            onSendDeveloperMessage = { subject, message ->
                viewModel.sendDeveloperMessage(subject, message)
            },
            onOpenMemberProfile = { member ->
                showQuestsDialog = false
                viewModel.openProfileViewer(member.userId, member.displayName, member.photoUrl) {
                    val convId = CordlyRepository.deterministicPairId(viewModel.currentUserId, member.userId)
                    viewModel.openDirectChat(convId)
                }
            },
            onOpenShopForRanks = {
                showQuestsDialog = false
                showShopDialog = true
            },
            onDismiss = { showQuestsDialog = false }
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

    if (joinedCall != null && !isCallMinimized) {
        ActiveCallOverlayDialog(
            call = joinedCall,
            currentUserId = viewModel.currentUserId,
            currentUserRank = effectiveRank,
            streams = callStreams,
            inviteableFriends = inviteableFriends,
            onToggleMute = { muted ->
                viewModel.updateCallMediaState(joinedCall, muted = muted, videoOn = joinedCall.callType == "video")
            },
            onToggleVideo = { videoOn ->
                viewModel.updateCallMediaState(joinedCall, muted = false, videoOn = videoOn)
            },
            onUpdateVideoQuality = { quality ->
                viewModel.updateCallVideoQuality(joinedCall, quality)
            },
            onAddParticipant = { friendUid, friendName ->
                viewModel.addParticipantToCall(joinedCall, friendUid, friendName)
            },
            onPublishStreamSample = { frameData, audioData, audioSeq, voiceEffect, faceFilter, videoQuality, audioLevel, videoEnabled, muted ->
                viewModel.publishCallStreamSample(
                    call = joinedCall,
                    frameData = frameData,
                    audioData = audioData,
                    audioSeq = audioSeq,
                    voiceEffect = voiceEffect,
                    faceFilter = faceFilter,
                    videoQuality = videoQuality,
                    audioLevel = audioLevel,
                    videoEnabled = videoEnabled,
                    muted = muted
                )
            },
            onStatusToast = { msg -> viewModel.showMessage(msg) },
            onEndCall = {
                if (joinedCall.targetType == "dm" || joinedCall.callerId == viewModel.currentUserId) {
                    viewModel.endOrDeclineCall(joinedCall, "ended")
                } else {
                    viewModel.leaveCurrentCallOnly()
                }
            },
            onMinimize = { isCallMinimized = true }
        )
    }
}

@Composable
private fun WelcomeStatusBanner(
    state: WelcomeBootstrapState,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isNew = state is WelcomeBootstrapState.NewUserWelcome
    val name = when (state) {
        is WelcomeBootstrapState.NewUserWelcome -> state.displayName
        is WelcomeBootstrapState.ReturningUserWelcomeBack -> state.displayName
        WelcomeBootstrapState.Hidden -> ""
    }
    val accent = if (isNew) CordlyCyan else CordlyAmber

    Surface(
        color = CordlySurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, accent),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onDismiss)
            .testTag(if (isNew) "welcome_new_banner" else "welcome_back_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(CordlyBlurple.copy(alpha = 0.24f), accent.copy(alpha = 0.16f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isNew) "${strings.welcomeNewTitle} $name" else "${strings.welcomeBackTitle} $name",
                    style = MaterialTheme.typography.titleMedium,
                    color = CordlyTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (isNew) {
                    Text(
                        text = strings.welcomeBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CordlyCyan
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = strings.closeButton, tint = CordlyTextSecondary)
            }
        }
    }
}

@Composable
private fun WorkspaceSidebar(
    currentUserId: String,
    ownProfile: UserPrivate?,
    effectiveRank: String,
    destination: ActiveDestination,
    homeTab: Int,
    hasWallpaper: Boolean,
    friendshipsState: UiState<List<Friendship>>,
    conversationsState: UiState<List<Conversation>>,
    servers: List<Server>,
    activeCalls: List<CallSession>,
    onSetHomeTab: (Int) -> Unit,
    onAddFriendClick: () -> Unit,
    onCopyCode: (String) -> Unit,
    onAcceptRequest: (Friendship) -> Unit,
    onDeclineRequest: (Friendship) -> Unit,
    onStartChatWithFriend: (Friendship) -> Unit,
    onStartVoiceCallWithFriend: (Friendship) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onOpenUserProfile: (uid: String, name: String, photo: String?, onMessage: (() -> Unit)?) -> Unit,
    onOpenChannel: (Server, String) -> Unit,
    onAddChannelClick: (Server) -> Unit,
    onUpdateServerIcon: (Server, String) -> Unit,
    onKickMember: (Server, String) -> Unit,
    onStartServerCall: (Server, String, String) -> Unit,
    onJoinOngoingCall: (CallSession) -> Unit,
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
                        effectiveRank = effectiveRank,
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
                        onStartVoiceCallWithFriend = onStartVoiceCallWithFriend,
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
                            isNeoPlus = effectiveRank == CordlyRepository.RANK_NEO_PLUS,
                            friendships = (friendshipsState as? UiState.Success)?.data.orEmpty(),
                            selectedChannel = destination.channelName,
                            activeServerCall = activeCalls.firstOrNull {
                                it.targetType == "server" && it.targetId == server.id
                            },
                            onCopyInvite = onCopyCode,
                            onOpenChannel = { ch -> onOpenChannel(server, ch) },
                            onAddChannelClick = { onAddChannelClick(server) },
                            onUpdateServerIcon = { url -> onUpdateServerIcon(server, url) },
                            onKickMember = { memberUid -> onKickMember(server, memberUid) },
                            onStartVoiceCall = {
                                onStartServerCall(server, destination.channelName, "voice")
                            },
                            onStartVideoCall = {
                                onStartServerCall(server, destination.channelName, "video")
                            },
                            onJoinOngoingCall = onJoinOngoingCall,
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
    effectiveRank: String,
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
    onStartVoiceCallWithFriend: (Friendship) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onOpenUserProfile: (uid: String, name: String, photo: String?, onMessage: (() -> Unit)?) -> Unit
) {
    val strings = LocalAppStrings.current
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
                    color = Color(0xFF5865F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
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
                            contentDescription = strings.shopButton,
                            tint = CordlyAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.shopButton,
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
                    Text(strings.addButton, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Discord-style Shareable Friend Code + P E Y Card
        if (ownProfile != null) {
            val profileThemeColor = remember(ownProfile.profileThemeColorHex, effectiveRank) {
                if (effectiveRank != CordlyRepository.RANK_NORMAL) {
                    ownProfile.profileThemeColorHex?.takeIf { it.isNotBlank() }?.let { parseHexColor(it) }
                } else {
                    null
                }
            }
            Surface(
                color = CordlyCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.2.dp,
                    (profileThemeColor ?: CordlyBlurple).copy(alpha = 0.55f)
                ),
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
                                        listOf(
                                            (profileThemeColor ?: CordlyBlurple).copy(alpha = 0.24f),
                                            (profileThemeColor ?: CordlyCyan).copy(alpha = 0.12f)
                                        )
                                    )
                                )
                        )
                    }

                    val hasBannerPhoto = !ownProfile.bannerUrl.isNullOrBlank()
                    val codeTint = profileThemeColor ?: if (hasBannerPhoto) Color(0xFF22D3EE) else CordlyCyan
                    val balanceTint = if (hasBannerPhoto) Color(0xFFF0B232) else CordlyAmber

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.codePrefix}: ${ownProfile.friendCode}",
                                style = MaterialTheme.typography.labelLarge,
                                color = codeTint
                            )
                            Text(
                                text = "${strings.balanceLabel}: ${ownProfile.peyPoints} P E Y",
                                style = MaterialTheme.typography.labelSmall,
                                color = balanceTint,
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
                                contentDescription = strings.copyAction,
                                tint = codeTint
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
                label = "${strings.messagesTab} (${conversations.size})",
                selected = homeTab == 0,
                badgeCount = 0,
                testTag = "tab_messages",
                onClick = { onSetHomeTab(0) },
                modifier = Modifier.weight(1f)
            )
            HubTabPill(
                label = "${strings.friendsTab} (${accepted.size})",
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
                        title = strings.noMessagesTitle,
                        subtitle = conversationsState.message
                    )
                }
                is UiState.Success -> {
                    if (conversations.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.Forum,
                            title = strings.noMessagesTitle,
                            subtitle = strings.noMessagesSubtitle,
                            actionLabel = strings.addFriendAction,
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
                        title = strings.noFriendsTitle,
                        subtitle = friendshipsState.message
                    )
                }
                is UiState.Success -> {
                    if (friendships.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.PersonAdd,
                            title = strings.noFriendsTitle,
                            subtitle = strings.noFriendsSubtitle,
                            actionLabel = strings.findFriendAction,
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
                                        text = "${strings.incomingRequests} (${incoming.size})",
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
                                        text = "${strings.allFriendsHint} (${accepted.size})",
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
                                        onVoiceCallClick = { onStartVoiceCallWithFriend(friend) },
                                        onMessageClick = { onStartChatWithFriend(friend) },
                                        onRemoveClick = { onDeclineRequest(friend) }
                                    )
                                }
                            }

                            if (outgoing.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "${strings.pendingSent} (${outgoing.size})",
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
    onVoiceCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val strings = LocalAppStrings.current
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
                onClick = onVoiceCallClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("call_friend_${friendship.id}")
            ) {
                Icon(Icons.Rounded.Call, contentDescription = strings.voiceCallAction, tint = CordlyEmerald)
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
    isNeoPlus: Boolean,
    friendships: List<Friendship>,
    selectedChannel: String,
    activeServerCall: CallSession?,
    onCopyInvite: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAddChannelClick: () -> Unit,
    onUpdateServerIcon: (String) -> Unit,
    onKickMember: (String) -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onJoinOngoingCall: (CallSession) -> Unit,
    onImageError: (String) -> Unit,
    onLeaveServer: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOwner = server.ownerId == currentUserId
    var showMembersManage by remember(server.id) { mutableStateOf(false) }
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
                            text = "${strings.joinByNamePrefix}: \"${server.name}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = CordlyCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(strings.copyAction, style = MaterialTheme.typography.labelSmall, color = CordlyTextSecondary)
                    }
                }
            }
        }

        HorizontalDivider(color = CordlyDivider, thickness = 1.dp)

        // Voice & Video Calls Quick Launch Bar
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = strings.voiceAndVideoHeader,
                style = MaterialTheme.typography.labelSmall,
                color = CordlyCyan
            )
            if (activeServerCall != null) {
                Surface(
                    color = CordlyEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CordlyEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onJoinOngoingCall(activeServerCall) }
                        .testTag("server_join_active_call_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (activeServerCall.callType == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                            contentDescription = null,
                            tint = CordlyEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${strings.groupVoiceCall} (${activeServerCall.callerName})",
                            style = MaterialTheme.typography.labelMedium,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.acceptCall,
                            style = MaterialTheme.typography.labelLarge,
                            color = CordlyEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CordlyEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onStartVoiceCall)
                        .minimumInteractiveComponentSize()
                        .testTag("server_voice_call_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, tint = CordlyEmerald, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.voiceCallAction,
                            style = MaterialTheme.typography.labelMedium,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = CordlyCardSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CordlyCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onStartVideoCall)
                        .minimumInteractiveComponentSize()
                        .testTag("server_video_call_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null, tint = CordlyCyan, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.videoCallAction,
                            style = MaterialTheme.typography.labelMedium,
                            color = CordlyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                text = strings.textChannelsHeader,
                style = MaterialTheme.typography.labelSmall,
                color = CordlyTextSecondary
            )
            if (isOwner) {
                IconButton(
                    onClick = onAddChannelClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = strings.createChannelTitle, tint = CordlyTextPrimary)
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

        // Neo Plus Server Member Kick Management for Server Owners
        if (isOwner && isNeoPlus && server.memberUids.size > 1) {
            HorizontalDivider(color = CordlyDivider, thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showMembersManage = !showMembersManage }
                        .padding(vertical = 6.dp)
                        .testTag("neo_plus_server_members_toggle")
                ) {
                    Text(
                        text = "NEO+ Member Control (${server.memberUids.size - 1})",
                        style = MaterialTheme.typography.labelMedium,
                        color = CordlyAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (showMembersManage) "Hide" else "Manage",
                        style = MaterialTheme.typography.labelSmall,
                        color = CordlyCyan
                    )
                }
                AnimatedVisibility(visible = showMembersManage) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val removableUids = server.memberUids.filter { it != server.ownerId }
                        removableUids.forEach { memberUid ->
                            val friendMatch = friendships.firstOrNull {
                                it.requesterId == memberUid || it.recipientId == memberUid
                            }
                            val displayName = when {
                                friendMatch == null -> "Member ${memberUid.take(6)}"
                                friendMatch.requesterId == memberUid -> friendMatch.requesterName
                                else -> friendMatch.recipientName
                            }
                            Surface(
                                color = CordlyCardSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CordlyTextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onKickMember(memberUid) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("kick_member_$memberUid")
                                    ) {
                                        Icon(
                                            Icons.Rounded.PersonRemove,
                                            contentDescription = "Kick $displayName",
                                            tint = CordlyCrimson,
                                            modifier = Modifier.size(18.dp)
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
}

@Composable
private fun ActiveChatPane(
    activeChat: ActiveChatTarget?,
    showBackButton: Boolean,
    hasWallpaper: Boolean,
    currentUserId: String,
    currentUserRank: String,
    compactCards: Boolean,
    conversations: List<Conversation>,
    servers: List<Server>,
    directMessagesState: UiState<List<com.example.data.model.DirectMessage>>,
    serverMessagesState: UiState<List<com.example.data.model.ServerMessage>>,
    onBack: () -> Unit,
    onSendDirect: (
        conv: Conversation,
        text: String,
        replyToText: String?,
        messageType: String,
        mediaData: String?,
        mediaDurationMs: Long?
    ) -> Unit,
    onDeleteDirect: (String, String, Boolean) -> Unit,
    onSendServer: (
        server: Server,
        channelName: String,
        text: String,
        replyToText: String?,
        messageType: String,
        mediaData: String?,
        mediaDurationMs: Long?
    ) -> Unit,
    onDeleteServer: (String, String, Boolean) -> Unit,
    onOpenUserProfile: (userId: String, name: String, photoUrl: String?) -> Unit,
    onStartDirectCall: (calleeId: String, calleeName: String, calleePhoto: String?, convId: String, callType: String) -> Unit,
    onStartServerCall: (server: Server, channelName: String, callType: String) -> Unit,
    onStatusToast: (String) -> Unit
) {
    val strings = LocalAppStrings.current
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
                            senderRank = it.senderRank,
                            text = it.text,
                            messageType = it.messageType,
                            mediaData = it.mediaData,
                            mediaDurationMs = it.mediaDurationMs,
                            replyToText = it.replyToText,
                            deletedForEveryone = it.deletedForEveryone,
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
                currentUserRank = currentUserRank,
                compactCards = compactCards,
                messagesState = mappedMessages,
                hasWallpaper = hasWallpaper,
                onBack = onBack,
                onSendMessage = { text, reply, type, data, dur ->
                    if (conv != null) onSendDirect(conv, text, reply, type, data, dur)
                },
                onDeleteMessage = { msgId, forEveryone ->
                    onDeleteDirect(activeChat.conversationId, msgId, forEveryone)
                },
                onUserProfileClick = onOpenUserProfile,
                onHeaderProfileClick = if (peerId.isNotBlank()) {
                    { onOpenUserProfile(peerId, peerName, peerPhoto) }
                } else {
                    null
                },
                onStartVoiceCall = if (conv != null && peerId.isNotBlank()) {
                    { onStartDirectCall(peerId, peerName, peerPhoto, conv.id, "voice") }
                } else {
                    null
                },
                onStartVideoCall = if (conv != null && peerId.isNotBlank()) {
                    { onStartDirectCall(peerId, peerName, peerPhoto, conv.id, "video") }
                } else {
                    null
                },
                onStatusToast = onStatusToast
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
                            senderRank = it.senderRank,
                            text = it.text,
                            messageType = it.messageType,
                            mediaData = it.mediaData,
                            mediaDurationMs = it.mediaDurationMs,
                            replyToText = it.replyToText,
                            deletedForEveryone = it.deletedForEveryone,
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
                currentUserRank = currentUserRank,
                compactCards = compactCards,
                messagesState = mappedMessages,
                hasWallpaper = hasWallpaper,
                onBack = onBack,
                onSendMessage = { text, reply, type, data, dur ->
                    if (server != null) onSendServer(server, activeChat.channelName, text, reply, type, data, dur)
                },
                onDeleteMessage = { msgId, forEveryone ->
                    onDeleteServer(activeChat.serverId, msgId, forEveryone)
                },
                onUserProfileClick = onOpenUserProfile,
                onStartVoiceCall = if (server != null) {
                    { onStartServerCall(server, activeChat.channelName, "voice") }
                } else {
                    null
                },
                onStartVideoCall = if (server != null) {
                    { onStartServerCall(server, activeChat.channelName, "video") }
                } else {
                    null
                },
                onStatusToast = onStatusToast
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
                    title = strings.selectChatTitle,
                    subtitle = strings.selectChatSubtitle
                )
            }
        }
    }
}
