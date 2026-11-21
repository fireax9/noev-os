package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserPrivate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val status: String = "online",
    val photoUrl: String? = null,
    val bannerUrl: String? = null,
    val customStatus: String? = null,
    val peyPoints: Long = 200L,
    val ownedFrames: List<String> = listOf("none"),
    val equippedFrame: String = "none",
    val ownedCardFrames: List<String> = listOf("none"),
    val equippedCardFrame: String = "none",
    val ownedProfileEffects: List<String> = listOf("none"),
    val equippedProfileEffect: String = "none",
    val profileVideoData: String? = null,
    val avatarVideoData: String? = null,
    val rank: String = "normal", // "normal" | "ultra" | "neo_plus"
    val rankExpiresAtMs: Long = 0L,
    val ultraTrialUsed: Boolean = false,
    val neoPlusTrialUsed: Boolean = false,
    val profileThemeColorHex: String? = null,
    val rewardedFriendUids: List<String> = emptyList(),
    val dailyClaimsCount: Long = 0L,
    val lastDailyClaimAtMs: Long = 0L,
    val channelRewardClaimed: Boolean = false,
    val claimedCustomQuests: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val status: String = "online",
    val photoUrl: String? = null,
    val bannerUrl: String? = null,
    val customStatus: String? = null,
    val equippedFrame: String = "none",
    val equippedCardFrame: String = "none",
    val equippedProfileEffect: String = "none",
    val profileVideoData: String? = null,
    val avatarVideoData: String? = null,
    val rank: String = "normal", // "normal" | "ultra" | "neo_plus"
    val rankExpiresAtMs: Long = 0L,
    val profileThemeColorHex: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LookupKey(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val requesterId: String = "",
    val recipientId: String = "",
    val requesterName: String = "",
    val recipientName: String = "",
    val requesterCode: String = "",
    val recipientCode: String = "",
    val requesterPhoto: String? = null,
    val recipientPhoto: String? = null,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantAId: String = "",
    val participantAName: String = "",
    val participantAPhoto: String? = null,
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBPhoto: String? = null,
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String? = null,
    val senderRank: String? = null,
    val text: String = "",
    val messageType: String = "text", // "text" | "image" | "audio" | "sticker" | "gif" | "deleted"
    val mediaData: String? = null,
    val mediaDurationMs: Long? = null,
    val replyToText: String? = null,
    val edited: Boolean? = null,
    val hiddenFor: List<String> = emptyList(),
    val deletedForEveryone: Boolean? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Server(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String? = null,
    val iconEmoji: String = "🚀",
    val iconUrl: String? = null,
    val accentHex: String = "#5865F2",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val channels: List<String> = listOf("general"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServerInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val serverId: String = "",
    val serverName: String = "",
    val iconEmoji: String = "🚀",
    val iconUrl: String? = null,
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServerMessage(
    @DocumentId val id: String = "",
    val serverId: String = "",
    val channelName: String = "general",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String? = null,
    val senderRank: String? = null,
    val text: String = "",
    val messageType: String = "text", // "text" | "image" | "audio" | "sticker" | "gif" | "deleted"
    val mediaData: String? = null,
    val mediaDurationMs: Long? = null,
    val replyToText: String? = null,
    val edited: Boolean? = null,
    val hiddenFor: List<String> = emptyList(),
    val deletedForEveryone: Boolean? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhoto: String? = null,
    val calleeId: String? = null,
    val calleeName: String = "",
    val calleePhoto: String? = null,
    val participantIds: List<String> = emptyList(),
    val targetType: String = "dm", // "dm" | "server"
    val targetId: String = "",
    val channelName: String? = null,
    val callType: String = "voice", // "voice" | "video"
    val status: String = "ringing", // "ringing" | "active" | "ended" | "declined"
    val videoQuality: String = "480p",
    val callerMuted: Boolean = false,
    val calleeMuted: Boolean = false,
    val callerVideoOn: Boolean = false,
    val calleeVideoOn: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallStreamFrame(
    @DocumentId val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val frameData: String? = null,
    val audioData: String? = null,
    val audioSeq: Long = 0L,
    val voiceEffect: String = "normal",
    val faceFilter: String = "none",
    val videoQuality: String = "480p",
    val audioLevel: Double = 0.0,
    val videoEnabled: Boolean = false,
    val muted: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomQuest(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorRank: String = "neo_plus",
    val title: String = "",
    val description: String = "",
    val questType: String = "add_friends", // "add_friends" | "daily_check"
    val targetCount: Long = 5L,
    val rewardPerStep: Long = 300L,
    val totalReward: Long = 1500L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DeveloperTicket(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRank: String = "neo_plus",
    val subject: String = "",
    val message: String = "",
    val status: String = "sent",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSignal(
    @DocumentId val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val participantIds: List<String> = emptyList(),
    val type: String = "offer", // "offer" | "answer" | "ice"
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServerEvent(
    @DocumentId val id: String = "",
    val serverId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String? = null,
    val scheduledLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomSticker(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val scopeId: String? = null,
    val title: String = "",
    val emoji: String = "✨",
    val accentHex: String = "#5865F2",
    val imageData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

