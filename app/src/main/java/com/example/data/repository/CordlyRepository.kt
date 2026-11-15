package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Friendship
import com.example.data.model.LookupKey
import com.example.data.model.PublicProfile
import com.example.data.model.Server
import com.example.data.model.ServerInvite
import com.example.data.model.ServerMessage
import com.example.data.model.UserPrivate
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class CordlyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(firebaseUser: FirebaseUser): Result<UserPrivate> {
        val uid = requireUserId()
        require(uid == firebaseUser.uid) { "Authenticated UID mismatch" }

        val userDocRef = db.collection("users").document(uid)
        val publicDocRef = db.collection("public_profiles").document(uid)
        val email = (firebaseUser.email ?: "").trim().ifBlank { "$uid@neov.user" }
        val fallbackName = (firebaseUser.displayName ?: email.substringBefore("@"))
            .trim()
            .take(60)
            .ifBlank { "neov user" }
        val googlePhotoUrl = firebaseUser.photoUrl?.toString()?.take(1024)
        val defaultFriendCode = generateFriendCode(uid)

        return try {
            val existingSnapshot = userDocRef.get().await()
            val existingUser = existingSnapshot.toObject(
                UserPrivate::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            val friendCode = existingUser?.friendCode?.takeIf { it.isNotBlank() } ?: defaultFriendCode
            val status = existingUser?.status?.takeIf { it.isNotBlank() } ?: "online"
            val customStatus = existingUser?.customStatus
            val finalPhotoUrl = existingUser?.photoUrl?.takeIf { it.isNotBlank() } ?: googlePhotoUrl
            val bannerUrl = existingUser?.bannerUrl?.takeIf { it.isNotBlank() }
            val finalName = existingUser?.displayName?.ifBlank { fallbackName } ?: fallbackName
            val initialPeyPoints = if (existingSnapshot.exists() && existingSnapshot.contains("peyPoints")) {
                existingUser?.peyPoints ?: INITIAL_PEY_POINTS
            } else {
                INITIAL_PEY_POINTS
            }
            val ownedFrames = existingUser?.ownedFrames?.takeIf { it.isNotEmpty() } ?: listOf("none")
            val equippedFrame = existingUser?.equippedFrame?.takeIf { it.isNotBlank() } ?: "none"
            val ownedProfileEffects = existingUser?.ownedProfileEffects?.takeIf { it.isNotEmpty() } ?: listOf("none")
            val equippedProfileEffect = existingUser?.equippedProfileEffect?.takeIf { it.isNotBlank() } ?: "none"
            val profileVideoData = existingUser?.profileVideoData?.takeIf { it.isNotBlank() }

            if (!existingSnapshot.exists()) {
                val privatePayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("email", email)
                    put("displayName", finalName)
                    put("friendCode", friendCode)
                    put("status", status)
                    if (finalPhotoUrl != null) put("photoUrl", finalPhotoUrl)
                    if (bannerUrl != null) put("bannerUrl", bannerUrl)
                    put("peyPoints", INITIAL_PEY_POINTS)
                    put("ownedFrames", listOf("none"))
                    put("equippedFrame", "none")
                    put("ownedProfileEffects", listOf("none"))
                    put("equippedProfileEffect", "none")
                    put("rewardedFriendUids", emptyList<String>())
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.set(privatePayload).await()
            } else {
                val updatePayload = buildMap<String, Any> {
                    put("email", email)
                    put("displayName", finalName)
                    put("friendCode", friendCode)
                    put("status", status)
                    if (finalPhotoUrl != null) put("photoUrl", finalPhotoUrl)
                    if (bannerUrl != null) put("bannerUrl", bannerUrl)
                    if (customStatus != null) put("customStatus", customStatus)
                    put("peyPoints", initialPeyPoints)
                    put("ownedFrames", ownedFrames)
                    put("equippedFrame", equippedFrame)
                    put("ownedProfileEffects", ownedProfileEffects)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.update(updatePayload).await()
            }

            val publicSnap = publicDocRef.get().await()
            if (!publicSnap.exists()) {
                val publicPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", finalName)
                    put("friendCode", friendCode)
                    put("status", status)
                    if (finalPhotoUrl != null) put("photoUrl", finalPhotoUrl)
                    if (bannerUrl != null) put("bannerUrl", bannerUrl)
                    if (customStatus != null) put("customStatus", customStatus)
                    put("equippedFrame", equippedFrame)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                publicDocRef.set(publicPayload).await()
            } else {
                val publicUpdate = buildMap<String, Any> {
                    put("displayName", finalName)
                    put("friendCode", friendCode)
                    put("status", status)
                    if (finalPhotoUrl != null) put("photoUrl", finalPhotoUrl)
                    if (bannerUrl != null) put("bannerUrl", bannerUrl)
                    if (customStatus != null) put("customStatus", customStatus)
                    put("equippedFrame", equippedFrame)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                publicDocRef.update(publicUpdate).await()
            }

            // Upsert privacy-safe lookup keys (email SHA-256 + friend code)
            upsertLookupKey("email_${sha256(email.lowercase())}", uid, finalName, friendCode, finalPhotoUrl)
            upsertLookupKey("code_${friendCode.uppercase()}", uid, finalName, friendCode, finalPhotoUrl)

            val refreshed = userDocRef.get().await().toObject(
                UserPrivate::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserPrivate(
                id = uid,
                userId = uid,
                email = email,
                displayName = finalName,
                friendCode = friendCode,
                status = status,
                photoUrl = finalPhotoUrl,
                bannerUrl = bannerUrl,
                customStatus = customStatus,
                peyPoints = initialPeyPoints,
                ownedFrames = ownedFrames,
                equippedFrame = equippedFrame,
                ownedProfileEffects = ownedProfileEffects,
                equippedProfileEffect = equippedProfileEffect,
                profileVideoData = profileVideoData,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(refreshed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path, auth)
            Result.failure(e)
        }
    }

    private suspend fun upsertLookupKey(
        keyId: String,
        uid: String,
        displayName: String,
        friendCode: String,
        photoUrl: String?
    ) {
        val ref = db.collection("lookup_keys").document(keyId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("displayName", displayName)
                put("friendCode", friendCode)
                if (photoUrl != null) put("photoUrl", photoUrl)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
        } else {
            val update = buildMap<String, Any> {
                put("displayName", displayName)
                put("friendCode", friendCode)
                if (photoUrl != null) put("photoUrl", photoUrl)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(update).await()
        }
    }

    suspend fun updateProfileStatus(
        status: String,
        customStatus: String?,
        displayName: String? = null,
        photoUrl: String? = null,
        bannerUrl: String? = null,
        clearBanner: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val userSnap = userRef.get().await()
            val current = userSnap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val trimmedName = displayName?.trim()?.take(60)?.takeIf { it.isNotBlank() } ?: current.displayName
            val trimmedCustom = customStatus?.trim()?.take(120)?.takeIf { it.isNotBlank() }
            val nextPhoto = photoUrl?.take(120000) ?: current.photoUrl
            val nextBanner = when {
                clearBanner -> null
                bannerUrl != null -> bannerUrl.take(120000)
                else -> current.bannerUrl
            }

            val privateUpdate = mutableMapOf<String, Any>(
                "status" to status,
                "displayName" to trimmedName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (trimmedCustom != null) {
                privateUpdate["customStatus"] = trimmedCustom
            } else {
                privateUpdate["customStatus"] = FieldValue.delete()
            }
            if (nextPhoto != null) {
                privateUpdate["photoUrl"] = nextPhoto
            }
            if (clearBanner) {
                privateUpdate["bannerUrl"] = FieldValue.delete()
            } else if (nextBanner != null) {
                privateUpdate["bannerUrl"] = nextBanner
            }
            userRef.update(privateUpdate).await()

            val publicUpdate = mutableMapOf<String, Any>(
                "status" to status,
                "displayName" to trimmedName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (trimmedCustom != null) {
                publicUpdate["customStatus"] = trimmedCustom
            } else {
                publicUpdate["customStatus"] = FieldValue.delete()
            }
            if (nextPhoto != null) {
                publicUpdate["photoUrl"] = nextPhoto
            }
            if (clearBanner) {
                publicUpdate["bannerUrl"] = FieldValue.delete()
            } else if (nextBanner != null) {
                publicUpdate["bannerUrl"] = nextBanner
            }
            publicRef.update(publicUpdate).await()

            if (displayName != null || photoUrl != null) {
                upsertLookupKey("email_${sha256(current.email.lowercase())}", uid, trimmedName, current.friendCode, nextPhoto)
                upsertLookupKey("code_${current.friendCode.uppercase()}", uid, trimmedName, current.friendCode, nextPhoto)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipFrame(frameId: String, cost: Long): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val owned = current.ownedFrames.ifEmpty { listOf("none") }
            if (owned.contains(frameId)) {
                userRef.update(
                    mapOf(
                        "equippedFrame" to frameId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                publicRef.update(
                    mapOf(
                        "equippedFrame" to frameId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success(current.peyPoints)
            }

            if (current.peyPoints < cost) {
                return Result.failure(IllegalStateException("Not enough P E Y points! Need $cost P E Y."))
            }

            val updatedBalance = current.peyPoints - cost
            val updatedFrames = (owned + frameId).distinct().take(30)

            userRef.update(
                mapOf(
                    "peyPoints" to updatedBalance,
                    "ownedFrames" to updatedFrames,
                    "equippedFrame" to frameId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            publicRef.update(
                mapOf(
                    "equippedFrame" to frameId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(updatedBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun equipFrame(frameId: String): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            val owned = current.ownedFrames.ifEmpty { listOf("none") }
            require(frameId == "none" || owned.contains(frameId)) {
                "Unlock this frame in the P E Y Shop first."
            }
            userRef.update(
                mapOf(
                    "equippedFrame" to frameId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            publicRef.update(
                mapOf(
                    "equippedFrame" to frameId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipProfileEffect(
        effectId: String,
        cost: Long,
        videoDataUri: String? = null
    ): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val owned = current.ownedProfileEffects.ifEmpty { listOf("none") }
            val isAlreadyOwned = effectId == "none" || owned.contains(effectId)
            if (!isAlreadyOwned && current.peyPoints < cost) {
                return Result.failure(IllegalStateException("Not enough P E Y points! Need $cost P E Y."))
            }

            val updatedBalance = if (isAlreadyOwned) current.peyPoints else current.peyPoints - cost
            val updatedEffects = if (isAlreadyOwned) owned else (owned + effectId).distinct().take(30)
            val safeVideoData = videoDataUri?.takeIf { it.isNotBlank() && it.length <= 740_000 }

            val privateUpdate = mutableMapOf<String, Any>(
                "peyPoints" to updatedBalance,
                "ownedProfileEffects" to updatedEffects,
                "equippedProfileEffect" to effectId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeVideoData != null) {
                privateUpdate["profileVideoData"] = safeVideoData
            }
            userRef.update(privateUpdate).await()

            val publicUpdate = mutableMapOf<String, Any>(
                "equippedProfileEffect" to effectId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeVideoData != null) {
                publicUpdate["profileVideoData"] = safeVideoData
            }
            publicRef.update(publicUpdate).await()

            Result.success(updatedBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun equipProfileEffect(effectId: String, videoDataUri: String? = null): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            val owned = current.ownedProfileEffects.ifEmpty { listOf("none") }
            require(effectId == "none" || owned.contains(effectId)) {
                "Unlock this 5s profile animation in the P E Y Shop first."
            }
            val safeVideoData = videoDataUri?.takeIf { it.isNotBlank() && it.length <= 740_000 }

            val privateUpdate = mutableMapOf<String, Any>(
                "equippedProfileEffect" to effectId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeVideoData != null) {
                privateUpdate["profileVideoData"] = safeVideoData
            }
            userRef.update(privateUpdate).await()

            val publicUpdate = mutableMapOf<String, Any>(
                "equippedProfileEffect" to effectId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeVideoData != null) {
                publicUpdate["profileVideoData"] = safeVideoData
            }
            publicRef.update(publicUpdate).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    private suspend fun rewardFriendPointsIfEligible(friendUid: String): Boolean {
        val uid = requireUserId()
        if (friendUid.isBlank() || friendUid == uid) return false
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java) ?: return false
            if (current.rewardedFriendUids.contains(friendUid) || current.rewardedFriendUids.size >= 200) {
                return false
            }
            val nextBalance = current.peyPoints + FRIEND_REWARD_PEY_POINTS
            val nextRewarded = (current.rewardedFriendUids + friendUid).distinct().take(200)
            userRef.update(
                mapOf(
                    "peyPoints" to nextBalance,
                    "rewardedFriendUids" to nextRewarded,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    fun observeOwnProfile(uid: String): Flow<UserPrivate?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserPrivate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePublicProfile(uid: String): Flow<PublicProfile?> = flow {
        val path = "public_profiles/$uid"
        emitAll(
            db.collection("public_profiles").document(uid)
                .snapshots()
                .map { it.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, auth.currentUser?.uid ?: uid)
                    throw error
                }
        )
    }

    suspend fun findUserByEmailOrCode(rawQuery: String): Result<LookupKey?> {
        requireUserId()
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return Result.success(null)

        val keyId = if (trimmed.contains("@")) {
            "email_${sha256(trimmed.lowercase())}"
        } else {
            val normalizedCode = trimmed.uppercase().replace(Regex("[^A-Z0-9\\-_]"), "")
            if (normalizedCode.isBlank()) return Result.success(null)
            "code_$normalizedCode"
        }

        val docRef = db.collection("lookup_keys").document(keyId)
        return try {
            val snap = docRef.get().await()
            val lookup = snap.toObject(LookupKey::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(lookup)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeFriendships(uid: String): Flow<List<Friendship>> = flow {
        val path = "friendships"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendFriendRequest(currentUser: UserPrivate, target: LookupKey): Result<String> {
        val uid = requireUserId()
        require(target.userId != uid) { "You cannot add yourself as a friend." }

        val friendshipId = deterministicPairId(uid, target.userId)
        val docRef = db.collection("friendships").document(friendshipId)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(friendshipId)
            }
            val payload = buildMap<String, Any> {
                put("participantIds", listOf(uid, target.userId).sorted())
                put("requesterId", uid)
                put("recipientId", target.userId)
                put("requesterName", currentUser.displayName)
                put("recipientName", target.displayName)
                put("requesterCode", currentUser.friendCode)
                put("recipientCode", target.friendCode)
                if (currentUser.photoUrl != null) put("requesterPhoto", currentUser.photoUrl)
                if (target.photoUrl != null) put("recipientPhoto", target.photoUrl)
                put("status", "pending")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            rewardFriendPointsIfEligible(target.userId)
            Result.success(friendshipId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendship: Friendship): Result<String> {
        val uid = requireUserId()
        require(friendship.id.isNotBlank()) { "Friendship ID cannot be blank" }
        val friendshipRef = db.collection("friendships").document(friendship.id)
        val convId = deterministicPairId(friendship.requesterId, friendship.recipientId)
        val convRef = db.collection("conversations").document(convId)

        return try {
            friendshipRef.update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val friendUid = if (friendship.requesterId == uid) friendship.recipientId else friendship.requesterId
            rewardFriendPointsIfEligible(friendUid)

            val convSnap = convRef.get().await()
            if (!convSnap.exists()) {
                val sortedParticipants = listOf(friendship.requesterId, friendship.recipientId).sorted()
                val convPayload = buildMap<String, Any> {
                    put("participantIds", sortedParticipants)
                    put("participantAId", friendship.requesterId)
                    put("participantAName", friendship.requesterName)
                    if (friendship.requesterPhoto != null) put("participantAPhoto", friendship.requesterPhoto)
                    put("participantBId", friendship.recipientId)
                    put("participantBName", friendship.recipientName)
                    if (friendship.recipientPhoto != null) put("participantBPhoto", friendship.recipientPhoto)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                convRef.set(convPayload).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, friendshipRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("friendships").document(friendshipId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            Result.failure(e)
        }
    }

    fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun getOrCreateConversation(
        currentUser: UserPrivate,
        friendId: String,
        friendName: String,
        friendPhoto: String?
    ): Result<String> {
        val uid = requireUserId()
        val convId = deterministicPairId(uid, friendId)
        val convRef = db.collection("conversations").document(convId)

        return try {
            val snap = convRef.get().await()
            if (!snap.exists()) {
                val payload = buildMap<String, Any> {
                    put("participantIds", listOf(uid, friendId).sorted())
                    put("participantAId", uid)
                    put("participantAName", currentUser.displayName)
                    if (currentUser.photoUrl != null) put("participantAPhoto", currentUser.photoUrl)
                    put("participantBId", friendId)
                    put("participantBName", friendName)
                    if (friendPhoto != null) put("participantBPhoto", friendPhoto)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                convRef.set(payload).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, convRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeDirectMessages(conversationId: String, uid: String): Flow<List<DirectMessage>> = flow {
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendDirectMessage(
        conversation: Conversation,
        sender: UserPrivate,
        text: String,
        replyToText: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(2000)
        require(cleanText.isNotEmpty()) { "Message text cannot be empty" }

        val msgRef = db.collection("conversations").document(conversation.id)
            .collection("messages").document()
        val convRef = db.collection("conversations").document(conversation.id)

        return try {
            val msgPayload = buildMap<String, Any> {
                put("conversationId", conversation.id)
                put("participantIds", conversation.participantIds)
                put("senderId", uid)
                put("senderName", sender.displayName)
                if (sender.photoUrl != null) put("senderPhoto", sender.photoUrl)
                put("text", cleanText)
                val cleanReply = replyToText?.trim()?.take(200)
                if (!cleanReply.isNullOrEmpty()) put("replyToText", cleanReply)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            msgRef.set(msgPayload).await()

            convRef.update(
                mapOf(
                    "lastMessageText" to cleanText.take(500),
                    "lastMessageSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteDirectMessage(conversationId: String, messageId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            Result.failure(e)
        }
    }

    fun observeServers(uid: String): Flow<List<Server>> = flow {
        val path = "servers"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Server::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createServer(
        name: String,
        description: String?,
        iconEmoji: String,
        accentHex: String,
        iconUrl: String? = null,
        channels: List<String> = listOf("general", "chill-lounge", "clips")
    ): Result<Server> {
        val uid = requireUserId()
        val cleanName = name.trim().take(50)
        require(cleanName.length >= 2) { "Server name must be at least 2 characters." }
        val cleanEmoji = iconEmoji.trim().take(16).ifBlank { "🚀" }
        val cleanIconUrl = iconUrl?.take(120000)?.takeIf { it.isNotBlank() }
        val cleanChannels = channels.map { it.trim().lowercase().replace(Regex("[^a-z0-9\\-_]"), "-") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(15)
            .ifEmpty { listOf("general") }

        val serverRef = db.collection("servers").document()
        val inviteCode = generateInviteCode()
        val inviteRef = db.collection("server_invites").document(inviteCode)
        val nameLookupKey = serverNameLookupKey(cleanName)
        val nameLookupRef = db.collection("server_invites").document(nameLookupKey)

        return try {
            val serverPayload = buildMap<String, Any> {
                put("name", cleanName)
                val cleanDesc = description?.trim()?.take(200)?.takeIf { it.isNotBlank() }
                if (cleanDesc != null) put("description", cleanDesc)
                put("iconEmoji", cleanEmoji)
                if (cleanIconUrl != null) put("iconUrl", cleanIconUrl)
                put("accentHex", accentHex)
                put("ownerId", uid)
                put("inviteCode", inviteCode)
                put("memberUids", listOf(uid))
                put("channels", cleanChannels)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val invitePayload = buildMap<String, Any> {
                put("inviteCode", inviteCode)
                put("serverId", serverRef.id)
                put("serverName", cleanName)
                put("iconEmoji", cleanEmoji)
                if (cleanIconUrl != null) put("iconUrl", cleanIconUrl)
                put("ownerId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            serverRef.set(serverPayload).await()
            inviteRef.set(invitePayload).await()

            // Register server name lookup so friends can join by typing the Server Name alone
            val existingNameDoc = nameLookupRef.get().await()
            if (!existingNameDoc.exists() || existingNameDoc.getString("ownerId") == uid) {
                val nameInvitePayload = buildMap<String, Any> {
                    put("inviteCode", nameLookupKey)
                    put("serverId", serverRef.id)
                    put("serverName", cleanName)
                    put("iconEmoji", cleanEmoji)
                    if (cleanIconUrl != null) put("iconUrl", cleanIconUrl)
                    put("ownerId", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                if (!existingNameDoc.exists()) {
                    nameLookupRef.set(nameInvitePayload).await()
                }
            }

            val created = serverRef.get().await().toObject(
                Server::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: Server(
                id = serverRef.id,
                name = cleanName,
                description = description,
                iconEmoji = cleanEmoji,
                iconUrl = cleanIconUrl,
                accentHex = accentHex,
                ownerId = uid,
                inviteCode = inviteCode,
                memberUids = listOf(uid),
                channels = cleanChannels,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, serverRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateServerIcon(server: Server, newIconUrl: String?, newEmoji: String? = null): Result<Unit> {
        val uid = requireUserId()
        require(server.ownerId == uid) { "Only the server owner can change the server icon." }
        val serverRef = db.collection("servers").document(server.id)
        val nextEmoji = newEmoji?.trim()?.take(16)?.ifBlank { server.iconEmoji } ?: server.iconEmoji
        val nextUrl = newIconUrl?.take(120000)?.takeIf { it.isNotBlank() }

        return try {
            val serverUpdate = mutableMapOf<String, Any>(
                "iconEmoji" to nextEmoji,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nextUrl != null) {
                serverUpdate["iconUrl"] = nextUrl
            } else {
                serverUpdate["iconUrl"] = FieldValue.delete()
            }
            serverRef.update(serverUpdate).await()

            val inviteUpdate = mutableMapOf<String, Any>(
                "iconEmoji" to nextEmoji,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nextUrl != null) {
                inviteUpdate["iconUrl"] = nextUrl
            } else {
                inviteUpdate["iconUrl"] = FieldValue.delete()
            }
            db.collection("server_invites").document(server.inviteCode).update(inviteUpdate).await()

            val nameRef = db.collection("server_invites").document(serverNameLookupKey(server.name))
            val nameSnap = nameRef.get().await()
            if (nameSnap.exists() && nameSnap.getString("ownerId") == uid) {
                nameRef.update(inviteUpdate).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, serverRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun joinServerByInvite(rawCodeOrName: String): Result<Server> {
        val uid = requireUserId()
        val trimmed = rawCodeOrName.trim()
        require(trimmed.length >= 2) { "Enter a valid server name or invite code." }

        val upperCode = trimmed.uppercase().replace(Regex("[^A-Z0-9\\-_]"), "")
        val nameKey = serverNameLookupKey(trimmed)

        return try {
            var invite: ServerInvite? = null

            // 1. Try direct invite code match first (e.g. CRD-8F2A1)
            if (upperCode.length in 4..24 && !trimmed.contains(" ")) {
                val codeSnap = db.collection("server_invites").document(upperCode).get().await()
                if (codeSnap.exists()) {
                    invite = codeSnap.toObject(ServerInvite::class.java)
                }
            }

            // 2. Fallback to exact Server Name lookup (e.g. "Squad HQ" or "سيرفر الأصدقاء")
            if (invite == null) {
                val nameSnap = db.collection("server_invites").document(nameKey).get().await()
                if (nameSnap.exists()) {
                    invite = nameSnap.toObject(ServerInvite::class.java)
                }
            }

            val resolvedInvite = invite
                ?: return Result.failure(IllegalArgumentException("No server found for '$trimmed'."))

            val serverRef = db.collection("servers").document(resolvedInvite.serverId)
            serverRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val joinedServer = serverRef.get().await().toObject(
                Server::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Could not load joined server."))

            Result.success(joinedServer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "server_invites", auth)
            Result.failure(e)
        }
    }

    suspend fun addChannelToServer(server: Server, rawChannelName: String): Result<Unit> {
        requireUserId()
        val channel = rawChannelName.trim().lowercase().replace(Regex("[^a-z0-9\\-_]"), "-").take(30)
        require(channel.isNotBlank()) { "Channel name cannot be empty." }
        if (server.channels.contains(channel)) return Result.success(Unit)
        require(server.channels.size < 15) { "Maximum 15 channels per server." }

        val serverRef = db.collection("servers").document(server.id)
        return try {
            serverRef.update(
                mapOf(
                    "channels" to (server.channels + channel),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, serverRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun leaveServer(serverId: String): Result<Unit> {
        val uid = requireUserId()
        val serverRef = db.collection("servers").document(serverId)
        return try {
            serverRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, serverRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeServerMessages(serverId: String, uid: String): Flow<List<ServerMessage>> = flow {
        val path = "servers/$serverId/messages"
        emitAll(
            db.collection("servers").document(serverId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServerMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendServerMessage(
        server: Server,
        channelName: String,
        sender: UserPrivate,
        text: String,
        replyToText: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(2000)
        require(cleanText.isNotEmpty()) { "Message text cannot be empty" }

        val msgRef = db.collection("servers").document(server.id)
            .collection("messages").document()
        return try {
            val payload = buildMap<String, Any> {
                put("serverId", server.id)
                put("channelName", channelName)
                put("memberUids", server.memberUids)
                put("senderId", uid)
                put("senderName", sender.displayName)
                if (sender.photoUrl != null) put("senderPhoto", sender.photoUrl)
                put("text", cleanText)
                val cleanReply = replyToText?.trim()?.take(200)
                if (!cleanReply.isNullOrEmpty()) put("replyToText", cleanReply)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            msgRef.set(payload).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteServerMessage(serverId: String, messageId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("servers").document(serverId)
            .collection("messages").document(messageId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
            Result.failure(e)
        }
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path, auth)
        }
    }

    companion object {
        const val INITIAL_PEY_POINTS = 200L
        const val FRIEND_REWARD_PEY_POINTS = 200L

        fun deterministicPairId(uidA: String, uidB: String): String {
            return if (uidA <= uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }

        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun serverNameLookupKey(serverName: String): String {
            val normalized = serverName.trim().lowercase()
            return "srvname_${sha256(normalized).take(32)}"
        }

        fun generateFriendCode(uid: String): String {
            val hash = sha256(uid).uppercase().take(6)
            return "NEV-$hash"
        }

        fun generateInviteCode(): String {
            return "NEV-${UUID.randomUUID().toString().replace("-", "").uppercase().take(5)}"
        }
    }
}
