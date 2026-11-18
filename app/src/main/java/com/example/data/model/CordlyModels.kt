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
    val ownedProfileEffects: List<String> = listOf("none"),
    val equippedProfileEffect: String = "none",
    val profileVideoData: String? = null,
    val rewardedFriendUids: List<String> = emptyList(),
    val dailyClaimsCount: Long = 0L,
    val lastDailyClaimAtMs: Long = 0L,
    val channelRewardClaimed: Boolean = false,
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
    val equippedProfileEffect: String = "none",
    val profileVideoData: String? = null,
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
    val text: String = "",
    val replyToText: String? = null,
    val edited: Boolean? = null,
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
    val text: String = "",
    val replyToText: String? = null,
    val edited: Boolean? = null,
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
    val audioLevel: Double = 0.0,
    val videoEnabled: Boolean = false,
    val muted: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
