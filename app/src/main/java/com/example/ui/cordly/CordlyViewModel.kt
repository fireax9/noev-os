package com.example.ui.cordly

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.CallSignal
import com.example.data.model.CallStreamFrame
import com.example.data.model.Conversation
import com.example.data.model.CustomQuest
import com.example.data.model.CustomSticker
import com.example.data.model.DeveloperTicket
import com.example.data.model.DirectMessage
import com.example.data.model.Friendship
import com.example.data.model.LookupKey
import com.example.data.model.PublicProfile
import com.example.data.model.Server
import com.example.data.model.ServerEvent
import com.example.data.model.ServerMessage
import com.example.data.model.UserPrivate
import com.example.data.repository.CordlyRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ActiveDestination {
    data object Home : ActiveDestination
    data class ServerSpace(val serverId: String, val channelName: String) : ActiveDestination
}

sealed interface ActiveChatTarget {
    data class DirectChat(val conversationId: String) : ActiveChatTarget
    data class ServerChannel(val serverId: String, val channelName: String) : ActiveChatTarget
}

sealed interface WelcomeBootstrapState {
    data object Hidden : WelcomeBootstrapState
    data class NewUserWelcome(val displayName: String) : WelcomeBootstrapState
    data class ReturningUserWelcomeBack(val displayName: String) : WelcomeBootstrapState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CordlyViewModel(
    private val repository: CordlyRepository,
    val currentUserId: String
) : ViewModel() {

    private val _destination = MutableStateFlow<ActiveDestination>(ActiveDestination.Home)
    val destination: StateFlow<ActiveDestination> = _destination.asStateFlow()

    private val _activeChat = MutableStateFlow<ActiveChatTarget?>(null)
    val activeChat: StateFlow<ActiveChatTarget?> = _activeChat.asStateFlow()

    private val _homeTab = MutableStateFlow(0) // 0 = Messages, 1 = Friends
    val homeTab: StateFlow<Int> = _homeTab.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _friendSearchState = MutableStateFlow<UiState<LookupKey?>?>(null)
    val friendSearchState: StateFlow<UiState<LookupKey?>?> = _friendSearchState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _welcomeBanner = MutableStateFlow<WelcomeBootstrapState>(WelcomeBootstrapState.Hidden)
    val welcomeBanner: StateFlow<WelcomeBootstrapState> = _welcomeBanner.asStateFlow()

    data class ProfileViewerTarget(
        val userId: String,
        val fallbackName: String,
        val fallbackPhoto: String?,
        val onMessageClick: (() -> Unit)? = null
    )

    private val _profileViewerTarget = MutableStateFlow<ProfileViewerTarget?>(null)
    val profileViewerTarget: StateFlow<ProfileViewerTarget?> = _profileViewerTarget.asStateFlow()

    private val _joinedCallId = MutableStateFlow<String?>(null)
    val joinedCallId: StateFlow<String?> = _joinedCallId.asStateFlow()

    private val streamPublishMutex = Mutex()
    private val lastSentVideoFrameByCall = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null && firebaseUser.uid == currentUserId) {
                repository.ensureUserProfile(firebaseUser)
                    .onSuccess { bootstrap ->
                        _welcomeBanner.value = if (bootstrap.isNewAccount) {
                            WelcomeBootstrapState.NewUserWelcome(bootstrap.user.displayName)
                        } else {
                            WelcomeBootstrapState.ReturningUserWelcomeBack(bootstrap.user.displayName)
                        }
                    }
                    .onFailure { err ->
                        Log.w(TAG, "Profile bootstrap failed", err)
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to initialize profile"
                    }
            }
        }
    }

    fun dismissWelcomeBanner() {
        _welcomeBanner.value = WelcomeBootstrapState.Hidden
    }

    val ownProfile: StateFlow<UiState<UserPrivate?>> = repository.observeOwnProfile(currentUserId)
        .map<UserPrivate?, UiState<UserPrivate?>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapError(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val friendships: StateFlow<UiState<List<Friendship>>> = repository.observeFriendships(currentUserId)
        .map<List<Friendship>, UiState<List<Friendship>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapError(error, "Failed to load friends")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val conversations: StateFlow<UiState<List<Conversation>>> = repository.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapError(error, "Failed to load direct messages")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val servers: StateFlow<UiState<List<Server>>> = repository.observeServers(currentUserId)
        .map<List<Server>, UiState<List<Server>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapError(error, "Failed to load servers")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val ultraDirectoryMembers: StateFlow<List<PublicProfile>> = repository.observeUltraDirectory()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val customQuests: StateFlow<List<CustomQuest>> = repository.observeCustomQuests()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val developerTickets: StateFlow<List<DeveloperTicket>> = repository.observeDeveloperMessages(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val directMessages: StateFlow<UiState<List<DirectMessage>>> = _activeChat
        .flatMapLatest { target ->
            if (target is ActiveChatTarget.DirectChat) {
                repository.observeDirectMessages(target.conversationId, currentUserId)
                    .map<List<DirectMessage>, UiState<List<DirectMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        emit(UiState.Error(unwrapError(error, "Failed to load messages")))
                    }
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val serverMessages: StateFlow<UiState<List<ServerMessage>>> = _activeChat
        .flatMapLatest { target ->
            if (target is ActiveChatTarget.ServerChannel) {
                repository.observeServerMessages(target.serverId, currentUserId)
                    .map<List<ServerMessage>, UiState<List<ServerMessage>>> { list ->
                        UiState.Success(list.filter { it.channelName == target.channelName })
                    }
                    .catch { error ->
                        emit(UiState.Error(unwrapError(error, "Failed to load channel messages")))
                    }
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val viewedPublicProfile: StateFlow<UiState<PublicProfile?>?> = _profileViewerTarget
        .flatMapLatest { target ->
            if (target == null) {
                flowOf(null)
            } else {
                repository.observePublicProfile(target.userId)
                    .map<PublicProfile?, UiState<PublicProfile?>> { UiState.Success(it) }
                    .catch { error ->
                        emit(UiState.Error(unwrapError(error, "Failed to load user profile")))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val activeCalls: StateFlow<List<CallSession>> = repository.observeActiveCalls(currentUserId)
        .catch { error ->
            Log.w(TAG, "Calls observer error", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val callStreams: StateFlow<List<CallStreamFrame>> = _joinedCallId
        .flatMapLatest { callId ->
            if (callId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeCallStreams(callId, currentUserId)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val incomingCallSignals: StateFlow<List<CallSignal>> = _joinedCallId
        .flatMapLatest { callId ->
            if (callId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeIncomingCallSignals(callId, currentUserId)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeServerEvents: StateFlow<List<ServerEvent>> = _destination
        .flatMapLatest { dest ->
            if (dest is ActiveDestination.ServerSpace) {
                repository.observeServerEvents(dest.serverId, currentUserId)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userCustomStickers: StateFlow<List<CustomSticker>> = repository.observeUserCustomStickers(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val serverCustomStickers: StateFlow<List<CustomSticker>> = _activeChat
        .flatMapLatest { target ->
            if (target is ActiveChatTarget.ServerChannel) {
                repository.observeServerCustomStickers(target.serverId, currentUserId)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun selectHome(tabIndex: Int? = null) {
        _destination.value = ActiveDestination.Home
        if (tabIndex != null) {
            _homeTab.value = tabIndex
        }
        _activeChat.value = null
    }

    fun setHomeTab(tabIndex: Int) {
        _homeTab.value = tabIndex
    }

    fun selectServer(server: Server) {
        val defaultChannel = server.channels.firstOrNull() ?: "general"
        _destination.value = ActiveDestination.ServerSpace(server.id, defaultChannel)
        _activeChat.value = null
    }

    fun openServerChannel(serverId: String, channelName: String) {
        _destination.value = ActiveDestination.ServerSpace(serverId, channelName)
        _activeChat.value = ActiveChatTarget.ServerChannel(serverId, channelName)
    }

    fun openDirectChat(conversationId: String) {
        _activeChat.value = ActiveChatTarget.DirectChat(conversationId)
    }

    fun closeActiveChat() {
        _activeChat.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearFriendSearch() {
        _friendSearchState.value = null
    }

    fun searchFriend(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _friendSearchState.value = UiState.Error("Enter a Gmail address or neov chat Friend Code.")
            return
        }
        _friendSearchState.value = UiState.Loading
        viewModelScope.launch {
            repository.findUserByEmailOrCode(trimmed)
                .onSuccess { lookup ->
                    if (lookup == null) {
                        _friendSearchState.value = UiState.Error("No neov chat user found for \"$trimmed\". Ask them to sign in once first.")
                    } else if (lookup.userId == currentUserId) {
                        _friendSearchState.value = UiState.Error("That's your own account!")
                    } else {
                        _friendSearchState.value = UiState.Success(lookup)
                    }
                }
                .onFailure { err ->
                    _friendSearchState.value = UiState.Error(err.localizedMessage ?: "Lookup failed.")
                }
        }
    }

    fun sendFriendRequest(target: LookupKey, onDone: () -> Unit) {
        val profile = (ownProfile.value as? UiState.Success)?.data
        if (profile == null) {
            _snackbarMessage.value = "Profile still loading, try again."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            repository.sendFriendRequest(profile, target)
                .onSuccess { (_, awarded) ->
                    _snackbarMessage.value = if (awarded > 0L) {
                        "Friend request sent to ${target.displayName} (+$awarded P E Y)"
                    } else {
                        "Friend request sent to ${target.displayName}"
                    }
                    _friendSearchState.value = null
                    onDone()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not send friend request."
                }
            _isBusy.value = false
        }
    }

    fun acceptFriendRequest(friendship: Friendship) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.acceptFriendRequest(friendship)
                .onSuccess { (convId, awarded) ->
                    _snackbarMessage.value = if (awarded > 0L) {
                        "You are now friends! +$awarded P E Y earned!"
                    } else {
                        "You are now friends! Chat unlocked."
                    }
                    _activeChat.value = ActiveChatTarget.DirectChat(convId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to accept request."
                }
            _isBusy.value = false
        }
    }

    fun removeFriendship(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriendship(friendshipId)
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update friendship."
                }
        }
    }

    fun startChatWithFriend(friendship: Friendship) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        val friendId = if (friendship.requesterId == currentUserId) friendship.recipientId else friendship.requesterId
        val friendName = if (friendship.requesterId == currentUserId) friendship.recipientName else friendship.requesterName
        val friendPhoto = if (friendship.requesterId == currentUserId) friendship.recipientPhoto else friendship.requesterPhoto

        _isBusy.value = true
        viewModelScope.launch {
            repository.getOrCreateConversation(profile, friendId, friendName, friendPhoto)
                .onSuccess { convId ->
                    _activeChat.value = ActiveChatTarget.DirectChat(convId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not open chat."
                }
            _isBusy.value = false
        }
    }

    fun sendDirectMessage(
        conversation: Conversation,
        text: String,
        replyToText: String? = null,
        messageType: String = "text",
        mediaData: String? = null,
        mediaDurationMs: Long? = null
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendDirectMessage(
                conversation = conversation,
                sender = profile,
                text = text,
                replyToText = replyToText,
                messageType = messageType,
                mediaData = mediaData,
                mediaDurationMs = mediaDurationMs
            )
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to send message."
                }
        }
    }

    fun deleteDirectMessage(conversationId: String, messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            repository.deleteDirectMessage(conversationId, messageId, forEveryone)
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to delete message."
                }
        }
    }

    fun createServer(
        name: String,
        description: String?,
        iconEmoji: String,
        accentHex: String,
        iconUrl: String? = null,
        onCreated: () -> Unit
    ) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.createServer(name, description, iconEmoji, accentHex, iconUrl)
                .onSuccess { server ->
                    _destination.value = ActiveDestination.ServerSpace(server.id, server.channels.firstOrNull() ?: "general")
                    _activeChat.value = null
                    _snackbarMessage.value = "Server '${server.name}' created! Friends can join by name."
                    onCreated()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to create server."
                }
            _isBusy.value = false
        }
    }

    fun updateServerIcon(server: Server, newIconUrl: String?, newEmoji: String? = null) {
        val profile = (ownProfile.value as? UiState.Success)?.data
        val isNeoPlus = CordlyRepository.effectiveRank(profile) == CordlyRepository.RANK_NEO_PLUS
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateServerIcon(server, newIconUrl, newEmoji, isNeoPlusMember = isNeoPlus)
                .onSuccess {
                    _snackbarMessage.value = "Server icon updated!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update server icon."
                }
            _isBusy.value = false
        }
    }

    fun joinServerByInvite(codeOrName: String, onJoined: () -> Unit) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.joinServerByInvite(codeOrName)
                .onSuccess { server ->
                    _destination.value = ActiveDestination.ServerSpace(server.id, server.channels.firstOrNull() ?: "general")
                    _activeChat.value = null
                    _snackbarMessage.value = "Joined ${server.name}!"
                    onJoined()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not join server."
                }
            _isBusy.value = false
        }
    }

    fun addChannel(server: Server, channelName: String, onAdded: () -> Unit) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.addChannelToServer(server, channelName)
                .onSuccess {
                    _snackbarMessage.value = "Channel created!"
                    onAdded()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not add channel."
                }
            _isBusy.value = false
        }
    }

    fun deleteChannel(server: Server, channelName: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.deleteChannelFromServer(server, channelName)
                .onSuccess {
                    val remaining = server.channels.filter { it != channelName }
                    val dest = _destination.value
                    if (dest is ActiveDestination.ServerSpace && dest.serverId == server.id && dest.channelName == channelName) {
                        _destination.value = ActiveDestination.ServerSpace(server.id, remaining.firstOrNull() ?: "general")
                    }
                    val chat = _activeChat.value
                    if (chat is ActiveChatTarget.ServerChannel && chat.serverId == server.id && chat.channelName == channelName) {
                        _activeChat.value = null
                    }
                    _snackbarMessage.value = "Channel #$channelName removed."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not delete channel."
                }
            _isBusy.value = false
        }
    }

    fun updateServerSettings(
        server: Server,
        name: String,
        description: String?,
        iconEmoji: String,
        accentHex: String,
        iconUrl: String?,
        onSaved: () -> Unit
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data
        val isNeoPlus = CordlyRepository.effectiveRank(profile) == CordlyRepository.RANK_NEO_PLUS
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateServerSettings(
                server = server,
                name = name,
                description = description,
                iconEmoji = iconEmoji,
                accentHex = accentHex,
                iconUrl = iconUrl,
                isNeoPlusMember = isNeoPlus
            )
                .onSuccess {
                    _snackbarMessage.value = "Server updated!"
                    onSaved()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update server."
                }
            _isBusy.value = false
        }
    }

    fun createServerEvent(
        server: Server,
        title: String,
        description: String?,
        scheduledLabel: String,
        onCreated: () -> Unit
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        _isBusy.value = true
        viewModelScope.launch {
            repository.createServerEvent(server, profile, title, description, scheduledLabel)
                .onSuccess { ev ->
                    _snackbarMessage.value = "Event created: ${ev.title}"
                    onCreated()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not create event."
                }
            _isBusy.value = false
        }
    }

    fun deleteServerEvent(serverId: String, eventId: String) {
        viewModelScope.launch {
            repository.deleteServerEvent(serverId, eventId)
                .onSuccess { _snackbarMessage.value = "Event deleted." }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not delete event."
                }
        }
    }

    fun createCustomSticker(
        title: String,
        emoji: String,
        accentHex: String,
        imageData: String?,
        serverId: String? = null,
        onCreated: () -> Unit = {}
    ) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.createCustomSticker(title, emoji, accentHex, imageData, serverId)
                .onSuccess { sticker ->
                    _snackbarMessage.value = "Sticker '${sticker.title}' saved!"
                    onCreated()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not create sticker."
                }
            _isBusy.value = false
        }
    }

    fun deleteCustomSticker(stickerId: String, serverId: String? = null) {
        viewModelScope.launch {
            repository.deleteCustomSticker(stickerId, serverId)
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not delete sticker."
                }
        }
    }

    fun removeMemberFromServer(server: Server, memberUid: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.removeMemberFromServer(server, memberUid)
                .onSuccess {
                    _snackbarMessage.value = "Member removed from ${server.name}."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to remove member."
                }
            _isBusy.value = false
        }
    }

    fun leaveServer(serverId: String) {
        viewModelScope.launch {
            repository.leaveServer(serverId)
                .onSuccess {
                    selectHome()
                    _snackbarMessage.value = "Left server."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not leave server."
                }
        }
    }

    fun sendServerMessage(
        server: Server,
        channelName: String,
        text: String,
        replyToText: String? = null,
        messageType: String = "text",
        mediaData: String? = null,
        mediaDurationMs: Long? = null
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendServerMessage(
                server = server,
                channelName = channelName,
                sender = profile,
                text = text,
                replyToText = replyToText,
                messageType = messageType,
                mediaData = mediaData,
                mediaDurationMs = mediaDurationMs
            )
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to send message."
                }
        }
    }

    fun deleteServerMessage(serverId: String, messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            repository.deleteServerMessage(serverId, messageId, forEveryone)
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to delete message."
                }
        }
    }

    fun updateStatus(
        status: String,
        customStatus: String?,
        displayName: String?,
        photoUrl: String? = null,
        bannerUrl: String? = null,
        clearBanner: Boolean = false,
        onSaved: () -> Unit
    ) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateProfileStatus(status, customStatus, displayName, photoUrl, bannerUrl, clearBanner)
                .onSuccess {
                    _snackbarMessage.value = "Profile saved"
                    onSaved()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update profile."
                }
            _isBusy.value = false
        }
    }

    fun updateProfileMedia(
        photoUrl: String? = null,
        bannerUrl: String? = null,
        clearBanner: Boolean = false,
        label: String = "Profile image"
    ) {
        val current = (ownProfile.value as? UiState.Success)?.data
        val status = current?.status ?: "online"
        val customStatus = current?.customStatus
        val displayName = current?.displayName
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateProfileStatus(
                status = status,
                customStatus = customStatus,
                displayName = displayName,
                photoUrl = photoUrl,
                bannerUrl = bannerUrl,
                clearBanner = clearBanner
            )
                .onSuccess {
                    _snackbarMessage.value = "$label updated!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update $label."
                }
            _isBusy.value = false
        }
    }

    fun activateFreeTrial(tier: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.activateFreeTrial(tier)
                .onSuccess {
                    val label = if (tier == CordlyRepository.RANK_NEO_PLUS) "Neo Plus (1 day)" else "Ultra (3 days)"
                    _snackbarMessage.value = "Free Trial Activated: $label!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Trial unavailable."
                }
            _isBusy.value = false
        }
    }

    fun purchaseRank(tier: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.purchaseRank(tier)
                .onSuccess { remaining ->
                    val label = if (tier == CordlyRepository.RANK_NEO_PLUS) "Neo Plus" else "Ultra"
                    _snackbarMessage.value = "Welcome to $label! ($remaining P E Y left)"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Rank purchase failed."
                }
            _isBusy.value = false
        }
    }

    fun updateProfileThemeColor(hex: String?) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateProfileThemeColor(hex)
                .onSuccess {
                    _snackbarMessage.value = if (hex != null) "Profile theme color updated!" else "Profile theme color cleared"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update profile color."
                }
            _isBusy.value = false
        }
    }

    fun updateAvatarVideo(videoDataUri: String?) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.updateAvatarVideo(videoDataUri)
                .onSuccess {
                    _snackbarMessage.value = if (videoDataUri != null) "Neo+ Video Avatar saved!" else "Video Avatar cleared."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update video avatar."
                }
            _isBusy.value = false
        }
    }

    fun purchaseAndEquipFrame(frame: ProfileFrameItem) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.purchaseAndEquipFrame(frame.id, frame.price)
                .onSuccess { remaining ->
                    _snackbarMessage.value = "Equipped ${frame.title}! ($remaining P E Y left)"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Purchase failed."
                }
            _isBusy.value = false
        }
    }

    fun equipFrame(frameId: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.equipFrame(frameId)
                .onSuccess {
                    val item = PeyShopCatalog.findById(frameId)
                    _snackbarMessage.value = "Frame equipped: ${item.title}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not equip frame."
                }
            _isBusy.value = false
        }
    }

    fun purchaseAndEquipCardFrame(cardFrame: ProfileCardFrameItem) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.purchaseAndEquipCardFrame(cardFrame.id, cardFrame.price)
                .onSuccess { remaining ->
                    _snackbarMessage.value = "Equipped Profile Frame: ${cardFrame.title}! ($remaining P E Y left)"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Purchase failed."
                }
            _isBusy.value = false
        }
    }

    fun equipCardFrame(cardFrameId: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.equipCardFrame(cardFrameId)
                .onSuccess {
                    val item = PeyShopCatalog.findCardFrameById(cardFrameId)
                    _snackbarMessage.value = "Profile Frame equipped: ${item.title}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not equip profile frame."
                }
            _isBusy.value = false
        }
    }

    fun purchaseAndEquipProfileEffect(
        effect: ProfileVideoEffectItem,
        videoDataUri: String? = null,
        localOnlyNotice: Boolean = false
    ) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.purchaseAndEquipProfileEffect(effect.id, effect.price, videoDataUri)
                .onSuccess { remaining ->
                    val kind = if (effect.isUnlimitedDuration) "unlimited video" else "5s video"
                    val syncSuffix = if (localOnlyNotice) " (full video saved locally)" else ""
                    _snackbarMessage.value = "Equipped $kind: ${effect.title}! ($remaining P E Y left)$syncSuffix"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Purchase failed."
                }
            _isBusy.value = false
        }
    }

    fun equipProfileEffect(
        effectId: String,
        videoDataUri: String? = null,
        localOnlyNotice: Boolean = false
    ) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.equipProfileEffect(effectId, videoDataUri)
                .onSuccess {
                    val item = PeyShopCatalog.findEffectById(effectId)
                    val kind = if (item.isUnlimitedDuration) "unlimited intro" else "5s intro"
                    val syncSuffix = if (localOnlyNotice) " (saved locally; smaller <500KB clips sync to friends)" else ""
                    _snackbarMessage.value = "Equipped $kind: ${item.title}$syncSuffix"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not equip video effect."
                }
            _isBusy.value = false
        }
    }

    fun openProfileViewer(
        userId: String,
        fallbackName: String,
        fallbackPhoto: String?,
        onMessageClick: (() -> Unit)? = null
    ) {
        if (userId.isBlank()) return
        _profileViewerTarget.value = ProfileViewerTarget(
            userId = userId,
            fallbackName = fallbackName,
            fallbackPhoto = fallbackPhoto,
            onMessageClick = onMessageClick
        )
    }

    fun closeProfileViewer() {
        _profileViewerTarget.value = null
    }

    fun claimDailyReward() {
        _isBusy.value = true
        viewModelScope.launch {
            repository.claimDailyPeyReward()
                .onSuccess { awarded ->
                    _snackbarMessage.value = "Daily Reward Claimed: +$awarded P E Y!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Daily reward is not ready yet."
                }
            _isBusy.value = false
        }
    }

    fun claimChannelReward(enteredName: String) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.claimChannelNameReward(enteredName)
                .onSuccess { awarded ->
                    _snackbarMessage.value = "fire ax9 Verified! +$awarded P E Y added!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not claim channel reward."
                }
            _isBusy.value = false
        }
    }

    fun createCustomQuest(
        title: String,
        description: String,
        questType: String,
        targetCount: Long,
        rewardPerStep: Long,
        onCreated: () -> Unit = {}
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        _isBusy.value = true
        viewModelScope.launch {
            repository.createCustomQuest(profile, title, description, questType, targetCount, rewardPerStep)
                .onSuccess {
                    _snackbarMessage.value = "Quest published: ${it.title} (+${it.totalReward} P E Y)!"
                    onCreated()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not create quest."
                }
            _isBusy.value = false
        }
    }

    fun claimCustomQuest(quest: CustomQuest, acceptedFriendsCount: Int) {
        _isBusy.value = true
        viewModelScope.launch {
            repository.claimCustomQuest(quest, acceptedFriendsCount)
                .onSuccess { awarded ->
                    _snackbarMessage.value = "Quest Complete! +$awarded P E Y added!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Quest not ready yet."
                }
            _isBusy.value = false
        }
    }

    fun sendDeveloperMessage(subject: String, message: String, onSent: () -> Unit = {}) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        _isBusy.value = true
        viewModelScope.launch {
            repository.sendDeveloperMessage(profile, subject, message)
                .onSuccess {
                    _snackbarMessage.value = "Sent directly to the developer!"
                    onSent()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not send message."
                }
            _isBusy.value = false
        }
    }

    fun startDirectCall(
        calleeId: String,
        calleeName: String,
        calleePhoto: String?,
        conversationId: String,
        callType: String
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        if (calleeId.isBlank()) return
        _isBusy.value = true
        viewModelScope.launch {
            repository.startCall(
                caller = profile,
                targetType = "dm",
                targetId = conversationId,
                channelName = null,
                calleeId = calleeId,
                calleeName = calleeName,
                calleePhoto = calleePhoto,
                callType = callType,
                participantIds = listOf(currentUserId, calleeId)
            )
                .onSuccess { call ->
                    _joinedCallId.value = call.id
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not start call."
                }
            _isBusy.value = false
        }
    }

    fun startOrJoinServerCall(
        server: Server,
        channelName: String,
        callType: String
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        val existingServerCall = activeCalls.value.firstOrNull {
            it.targetType == "server" && it.targetId == server.id
        }
        if (existingServerCall != null) {
            _joinedCallId.value = existingServerCall.id
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            repository.startCall(
                caller = profile,
                targetType = "server",
                targetId = server.id,
                channelName = channelName,
                calleeId = server.id,
                calleeName = "${server.name} • #$channelName",
                calleePhoto = server.iconUrl,
                callType = callType,
                participantIds = server.memberUids
            )
                .onSuccess { call ->
                    _joinedCallId.value = call.id
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not start voice/video call."
                }
            _isBusy.value = false
        }
    }

    fun answerCall(call: CallSession, withVideo: Boolean) {
        viewModelScope.launch {
            repository.answerCall(call.id, withVideo)
                .onSuccess {
                    _joinedCallId.value = call.id
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not answer call."
                }
        }
    }

    fun joinOngoingCall(call: CallSession) {
        _joinedCallId.value = call.id
    }

    fun endOrDeclineCall(call: CallSession, newStatus: String) {
        if (_joinedCallId.value == call.id) {
            _joinedCallId.value = null
        }
        viewModelScope.launch {
            repository.endOrDeclineCall(call.id, newStatus)
        }
    }

    fun leaveCurrentCallOnly() {
        _joinedCallId.value = null
    }

    fun updateCallMediaState(call: CallSession, muted: Boolean, videoOn: Boolean) {
        val isCaller = call.callerId == currentUserId
        viewModelScope.launch {
            repository.updateCallMediaState(call.id, isCaller, muted, videoOn)
        }
    }

    fun updateCallVideoQuality(call: CallSession, quality: String) {
        viewModelScope.launch {
            repository.updateCallVideoQuality(call.id, quality)
        }
    }

    fun addParticipantToCall(call: CallSession, friendUid: String, friendName: String) {
        viewModelScope.launch {
            repository.addParticipantToCall(call, friendUid)
                .onSuccess {
                    _snackbarMessage.value = "Added $friendName to the call!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not add participant."
                }
        }
    }

    fun sendCallSignal(
        call: CallSession,
        recipientId: String,
        type: String,
        sdp: String? = null,
        candidate: String? = null,
        sdpMid: String? = null,
        sdpMLineIndex: Int? = null
    ) {
        viewModelScope.launch {
            repository.sendCallSignal(
                callId = call.id,
                recipientId = recipientId,
                participantIds = call.participantIds,
                type = type,
                sdp = sdp,
                candidate = candidate,
                sdpMid = sdpMid,
                sdpMLineIndex = sdpMLineIndex
            )
        }
    }

    fun deleteCallSignal(callId: String, signalId: String) {
        viewModelScope.launch {
            repository.deleteCallSignal(callId, signalId)
        }
    }

    fun publishCallStreamSample(
        call: CallSession,
        frameData: String?,
        audioData: String?,
        audioSeq: Long,
        voiceEffect: String,
        faceFilter: String,
        videoQuality: String,
        audioLevel: Double,
        videoEnabled: Boolean,
        muted: Boolean
    ) {
        val profile = (ownProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            // Audio & video media flow directly over real-time P2P WebRTC; only lightweight state syncs here.
            repository.publishCallStreamFrame(
                callId = call.id,
                sender = profile,
                participantIds = call.participantIds,
                frameData = null,
                audioData = null,
                audioSeq = 0L,
                voiceEffect = voiceEffect,
                faceFilter = faceFilter,
                videoQuality = videoQuality,
                audioLevel = audioLevel,
                videoEnabled = videoEnabled,
                muted = muted
            )
        }
    }

    private fun unwrapError(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return root.message ?: fallback
    }

    private companion object {
        const val TAG = "NeovVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
