package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

data class UserBootstrapResult(
    val user: UserPrivate,
    val isNewAccount: Boolean
)

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

    suspend fun ensureUserProfile(firebaseUser: FirebaseUser): Result<UserBootstrapResult> {
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
            val isNewAccount = !existingSnapshot.exists()
            val existingUser = existingSnapshot.toObject(
                UserPrivate::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            val nowMs = System.currentTimeMillis()
            val resolvedRank = effectiveRank(existingUser, nowMs)
            val resolvedExpiresAtMs = if (resolvedRank == RANK_NORMAL) 0L else (existingUser?.rankExpiresAtMs ?: 0L)

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
            val ownedCardFrames = existingUser?.ownedCardFrames?.takeIf { it.isNotEmpty() } ?: listOf("none")
            val equippedCardFrame = existingUser?.equippedCardFrame?.takeIf { it.isNotBlank() } ?: "none"
            val ownedProfileEffects = existingUser?.ownedProfileEffects?.takeIf { it.isNotEmpty() } ?: listOf("none")
            val equippedProfileEffect = existingUser?.equippedProfileEffect?.takeIf { it.isNotBlank() } ?: "none"
            val profileVideoData = existingUser?.profileVideoData?.takeIf { it.isNotBlank() }
            val avatarVideoData = existingUser?.avatarVideoData?.takeIf { it.isNotBlank() }
            val profileThemeColorHex = existingUser?.profileThemeColorHex?.takeIf { it.isNotBlank() }

            if (isNewAccount) {
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
                    put("ownedCardFrames", listOf("none"))
                    put("equippedCardFrame", "none")
                    put("ownedProfileEffects", listOf("none"))
                    put("equippedProfileEffect", "none")
                    put("rank", RANK_NORMAL)
                    put("rankExpiresAtMs", 0L)
                    put("ultraTrialUsed", false)
                    put("neoPlusTrialUsed", false)
                    put("rewardedFriendUids", emptyList<String>())
                    put("dailyClaimsCount", 0L)
                    put("lastDailyClaimAtMs", 0L)
                    put("channelRewardClaimed", false)
                    put("claimedCustomQuests", emptyList<String>())
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
                    put("ownedCardFrames", ownedCardFrames)
                    put("equippedCardFrame", equippedCardFrame)
                    put("ownedProfileEffects", ownedProfileEffects)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    if (avatarVideoData != null) put("avatarVideoData", avatarVideoData)
                    put("rank", resolvedRank)
                    put("rankExpiresAtMs", resolvedExpiresAtMs)
                    if (profileThemeColorHex != null) put("profileThemeColorHex", profileThemeColorHex)
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
                    put("equippedCardFrame", equippedCardFrame)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    if (avatarVideoData != null) put("avatarVideoData", avatarVideoData)
                    put("rank", resolvedRank)
                    put("rankExpiresAtMs", resolvedExpiresAtMs)
                    if (profileThemeColorHex != null) put("profileThemeColorHex", profileThemeColorHex)
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
                    put("equippedCardFrame", equippedCardFrame)
                    put("equippedProfileEffect", equippedProfileEffect)
                    if (profileVideoData != null) put("profileVideoData", profileVideoData)
                    if (avatarVideoData != null) put("avatarVideoData", avatarVideoData)
                    put("rank", resolvedRank)
                    put("rankExpiresAtMs", resolvedExpiresAtMs)
                    if (profileThemeColorHex != null) put("profileThemeColorHex", profileThemeColorHex)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                publicDocRef.update(publicUpdate).await()
            }

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
                ownedCardFrames = ownedCardFrames,
                equippedCardFrame = equippedCardFrame,
                ownedProfileEffects = ownedProfileEffects,
                equippedProfileEffect = equippedProfileEffect,
                profileVideoData = profileVideoData,
                avatarVideoData = avatarVideoData,
                rank = resolvedRank,
                rankExpiresAtMs = resolvedExpiresAtMs,
                ultraTrialUsed = existingUser?.ultraTrialUsed ?: false,
                neoPlusTrialUsed = existingUser?.neoPlusTrialUsed ?: false,
                profileThemeColorHex = profileThemeColorHex,
                dailyClaimsCount = existingUser?.dailyClaimsCount ?: 0L,
                lastDailyClaimAtMs = existingUser?.lastDailyClaimAtMs ?: 0L,
                channelRewardClaimed = existingUser?.channelRewardClaimed ?: false,
                claimedCustomQuests = existingUser?.claimedCustomQuests ?: emptyList(),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            if (isOwnerChannelNameMatch(finalName)) {
                tryAwardFirstOwnerPeyBonus(uid)
            }
            Result.success(UserBootstrapResult(user = refreshed, isNewAccount = isNewAccount))
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
            if (isOwnerChannelNameMatch(trimmedName)) {
                tryAwardFirstOwnerPeyBonus(uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    private suspend fun tryAwardFirstOwnerPeyBonus(uid: String) {
        val lockRef = db.collection("app_meta").document("owner_name_reward")
        val userRef = db.collection("users").document(uid)
        try {
            db.runTransaction { tx ->
                val lockSnap = tx.get(lockRef)
                if (!lockSnap.exists()) {
                    val userSnap = tx.get(userRef)
                    val currentPey = userSnap.getLong("peyPoints") ?: 0L
                    tx.set(
                        lockRef,
                        mapOf(
                            "claimedBy" to uid,
                            "claimedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        userRef,
                        mapOf(
                            "peyPoints" to (currentPey + OWNER_NAME_REWARD_PEY),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Silently ignore if already claimed or lock cannot be acquired
        }
    }

    suspend fun activateFreeTrial(tier: String, nowMs: Long = System.currentTimeMillis()): Result<String> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val durationMs: Long
            val updateMap = mutableMapOf<String, Any>(
                "rank" to tier,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            when (tier) {
                RANK_ULTRA -> {
                    if (current.ultraTrialUsed) {
                        return Result.failure(IllegalStateException("Ultra 3-day free trial already used."))
                    }
                    durationMs = ULTRA_TRIAL_DAYS * ONE_DAY_MS
                    updateMap["ultraTrialUsed"] = true
                }
                RANK_NEO_PLUS -> {
                    if (current.neoPlusTrialUsed) {
                        return Result.failure(IllegalStateException("Neo Plus 1-day free trial already used."))
                    }
                    durationMs = NEO_PLUS_TRIAL_DAYS * ONE_DAY_MS
                    updateMap["neoPlusTrialUsed"] = true
                }
                else -> return Result.failure(IllegalArgumentException("Unknown rank tier"))
            }

            val expiresAt = nowMs + durationMs
            updateMap["rankExpiresAtMs"] = expiresAt
            userRef.update(updateMap).await()
            publicRef.update(
                mapOf(
                    "rank" to tier,
                    "rankExpiresAtMs" to expiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(tier)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun purchaseRank(tier: String, nowMs: Long = System.currentTimeMillis()): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val cost = when (tier) {
                RANK_ULTRA -> ULTRA_COST_PEY
                RANK_NEO_PLUS -> NEO_PLUS_COST_PEY
                else -> return Result.failure(IllegalArgumentException("Invalid rank"))
            }
            if (current.peyPoints < cost) {
                return Result.failure(IllegalStateException("Not enough P E Y! Need $cost P E Y."))
            }

            val baseTime = if (effectiveRank(current, nowMs) == tier && current.rankExpiresAtMs > nowMs) {
                current.rankExpiresAtMs
            } else {
                nowMs
            }
            val expiresAt = baseTime + RANK_DURATION_MS
            val remaining = current.peyPoints - cost

            userRef.update(
                mapOf(
                    "peyPoints" to remaining,
                    "rank" to tier,
                    "rankExpiresAtMs" to expiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            publicRef.update(
                mapOf(
                    "rank" to tier,
                    "rankExpiresAtMs" to expiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(remaining)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateProfileThemeColor(hex: String?): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            val activeRank = effectiveRank(current)
            if (activeRank == RANK_NORMAL) {
                return Result.failure(IllegalStateException("Ultra or Neo Plus rank required for full profile colors."))
            }
            val cleanHex = hex?.trim()?.take(12)?.takeIf { it.startsWith("#") }
            val privateMap = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            val publicMap = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (cleanHex != null) {
                privateMap["profileThemeColorHex"] = cleanHex
                publicMap["profileThemeColorHex"] = cleanHex
            } else {
                privateMap["profileThemeColorHex"] = FieldValue.delete()
                publicMap["profileThemeColorHex"] = FieldValue.delete()
            }
            userRef.update(privateMap).await()
            publicRef.update(publicMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateAvatarVideo(videoDataUri: String?): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            if (effectiveRank(current) != RANK_NEO_PLUS) {
                return Result.failure(IllegalStateException("Neo Plus rank required for Video Avatar."))
            }
            val safe = videoDataUri?.takeIf { it.isNotBlank() && it.length <= 740_000 }
            val privateMap = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            val publicMap = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (safe != null) {
                privateMap["avatarVideoData"] = safe
                publicMap["avatarVideoData"] = safe
            } else {
                privateMap["avatarVideoData"] = FieldValue.delete()
                publicMap["avatarVideoData"] = FieldValue.delete()
            }
            userRef.update(privateMap).await()
            publicRef.update(publicMap).await()
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
                    mapOf("equippedFrame" to frameId, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                publicRef.update(
                    mapOf("equippedFrame" to frameId, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                return Result.success(current.peyPoints)
            }

            if (current.peyPoints < cost) {
                return Result.failure(IllegalStateException("Not enough P E Y points! Need $cost P E Y."))
            }

            val updatedBalance = current.peyPoints - cost
            val updatedFrames = (owned + frameId).distinct().take(40)

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
                mapOf("equippedFrame" to frameId, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            publicRef.update(
                mapOf("equippedFrame" to frameId, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipCardFrame(cardFrameId: String, cost: Long): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")

            val owned = current.ownedCardFrames.ifEmpty { listOf("none") }
            if (owned.contains(cardFrameId)) {
                userRef.update(
                    mapOf("equippedCardFrame" to cardFrameId, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                publicRef.update(
                    mapOf("equippedCardFrame" to cardFrameId, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                return Result.success(current.peyPoints)
            }

            if (current.peyPoints < cost) {
                return Result.failure(IllegalStateException("Not enough P E Y points! Need $cost P E Y."))
            }

            val updatedBalance = current.peyPoints - cost
            val updatedCardFrames = (owned + cardFrameId).distinct().take(40)

            userRef.update(
                mapOf(
                    "peyPoints" to updatedBalance,
                    "ownedCardFrames" to updatedCardFrames,
                    "equippedCardFrame" to cardFrameId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            publicRef.update(
                mapOf(
                    "equippedCardFrame" to cardFrameId,
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

    suspend fun equipCardFrame(cardFrameId: String): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            val owned = current.ownedCardFrames.ifEmpty { listOf("none") }
            require(cardFrameId == "none" || owned.contains(cardFrameId)) {
                "Unlock this profile card frame in the P E Y Shop first."
            }
            userRef.update(
                mapOf("equippedCardFrame" to cardFrameId, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            publicRef.update(
                mapOf("equippedCardFrame" to cardFrameId, "updatedAt" to FieldValue.serverTimestamp())
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
            val updatedEffects = if (isAlreadyOwned) owned else (owned + effectId).distinct().take(40)
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
                "Unlock this video effect in the P E Y Shop first."
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

    private suspend fun rewardFriendPointsIfEligible(friendUid: String): Long {
        val uid = requireUserId()
        if (friendUid.isBlank() || friendUid == uid) return 0L
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java) ?: return 0L
            if (current.rewardedFriendUids.contains(friendUid) || current.rewardedFriendUids.size >= 200) {
                return 0L
            }
            val awarded = FRIEND_REWARD_PEY_POINTS + neoPlusRewardBonus(current)
            val nextBalance = current.peyPoints + awarded
            val nextRewarded = (current.rewardedFriendUids + friendUid).distinct().take(200)
            userRef.update(
                mapOf(
                    "peyPoints" to nextBalance,
                    "rewardedFriendUids" to nextRewarded,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            awarded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun claimDailyPeyReward(nowMs: Long = System.currentTimeMillis()): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(
                UserPrivate::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found")

            val status = computeDailyRewardStatus(current, nowMs)
            if (!status.isClaimable) {
                val hours = status.remainingMs / 3_600_000L
                val mins = (status.remainingMs % 3_600_000L) / 60_000L
                return Result.failure(
                    IllegalStateException("Daily reward unlocks in ${hours}h ${mins}m")
                )
            }

            val totalAward = status.nextRewardAmount + neoPlusRewardBonus(current, nowMs)
            val nextBalance = current.peyPoints + totalAward
            userRef.update(
                mapOf(
                    "peyPoints" to nextBalance,
                    "dailyClaimsCount" to (current.dailyClaimsCount + 1L),
                    "lastDailyClaimAtMs" to nowMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(totalAward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun claimChannelNameReward(enteredName: String): Result<Long> {
        val uid = requireUserId()
        val cleanInput = enteredName.trim()
        if (!cleanInput.equals(CREATOR_CHANNEL_NAME, ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Enter the exact channel name: $CREATOR_CHANNEL_NAME")
            )
        }
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            if (current.channelRewardClaimed) {
                return Result.failure(
                    IllegalStateException("Channel bonus ($CREATOR_CHANNEL_NAME) already claimed!")
                )
            }
            val totalAward = CHANNEL_REWARD_PEY_POINTS + neoPlusRewardBonus(current)
            val nextBalance = current.peyPoints + totalAward
            userRef.update(
                mapOf(
                    "peyPoints" to nextBalance,
                    "channelRewardClaimed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(totalAward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeUltraDirectory(): Flow<List<PublicProfile>> = flow {
        val path = "public_profiles"
        emitAll(
            db.collection(path)
                .whereIn("rank", listOf(RANK_ULTRA, RANK_NEO_PLUS))
                .snapshots()
                .map { snap ->
                    val nowMs = System.currentTimeMillis()
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { effectiveRank(it, nowMs) != RANK_NORMAL }
                        .sortedWith(
                            compareByDescending<PublicProfile> {
                                if (effectiveRank(it, nowMs) == RANK_NEO_PLUS) 2 else 1
                            }.thenBy { it.displayName.lowercase() }
                        )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, auth.currentUser?.uid ?: "")
                    throw error
                }
        )
    }

    fun observeCustomQuests(): Flow<List<CustomQuest>> = flow {
        val path = "custom_quests"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomQuest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, auth.currentUser?.uid ?: "")
                    throw error
                }
        )
    }

    suspend fun createCustomQuest(
        creator: UserPrivate,
        title: String,
        description: String,
        questType: String,
        targetCount: Long,
        rewardPerStep: Long
    ): Result<CustomQuest> {
        val uid = requireUserId()
        if (effectiveRank(creator) != RANK_NEO_PLUS) {
            return Result.failure(IllegalStateException("Only Neo Plus members can create custom quests."))
        }
        val cleanTitle = title.trim().take(120)
        require(cleanTitle.length >= 2) { "Quest title must be at least 2 characters." }
        val safeTarget = targetCount.coerceIn(1L, 50L)
        val safePerStep = rewardPerStep.coerceIn(50L, 1000L)
        val total = (safeTarget * safePerStep).coerceIn(50L, 10000L)
        val safeDesc = description.trim().take(300)
        val safeType = if (questType == "daily_check") "daily_check" else "add_friends"

        val ref = db.collection("custom_quests").document()
        return try {
            val payload = buildMap<String, Any> {
                put("creatorId", uid)
                put("creatorName", creator.displayName.take(60).ifBlank { "Neo+ Member" })
                put("creatorRank", RANK_NEO_PLUS)
                put("title", cleanTitle)
                if (safeDesc.isNotBlank()) put("description", safeDesc)
                put("questType", safeType)
                put("targetCount", safeTarget)
                put("rewardPerStep", safePerStep)
                put("totalReward", total)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
            Result.success(
                CustomQuest(
                    id = ref.id,
                    creatorId = uid,
                    creatorName = creator.displayName,
                    creatorRank = RANK_NEO_PLUS,
                    title = cleanTitle,
                    description = safeDesc,
                    questType = safeType,
                    targetCount = safeTarget,
                    rewardPerStep = safePerStep,
                    totalReward = total,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun claimCustomQuest(quest: CustomQuest, acceptedFriendsCount: Int): Result<Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(UserPrivate::class.java)
                ?: throw IllegalStateException("Profile not found")
            if (current.claimedCustomQuests.contains(quest.id)) {
                return Result.failure(IllegalStateException("You already claimed this quest reward."))
            }
            if (quest.questType == "add_friends" && acceptedFriendsCount < quest.targetCount) {
                return Result.failure(
                    IllegalStateException("Add ${quest.targetCount - acceptedFriendsCount} more friends to claim!")
                )
            }
            val totalAward = quest.totalReward + neoPlusRewardBonus(current)
            val nextBalance = current.peyPoints + totalAward
            val nextClaimed = (current.claimedCustomQuests + quest.id).distinct().take(200)
            userRef.update(
                mapOf(
                    "peyPoints" to nextBalance,
                    "claimedCustomQuests" to nextClaimed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(totalAward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeDeveloperMessages(uid: String): Flow<List<DeveloperTicket>> = flow {
        val path = "dev_messages"
        emitAll(
            db.collection(path)
                .whereEqualTo("senderId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DeveloperTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendDeveloperMessage(
        sender: UserPrivate,
        subject: String,
        message: String
    ): Result<Unit> {
        val uid = requireUserId()
        if (effectiveRank(sender) != RANK_NEO_PLUS) {
            return Result.failure(IllegalStateException("Only Neo Plus members can contact the developer directly."))
        }
        val cleanSub = subject.trim().take(120)
        val cleanMsg = message.trim().take(1500)
        require(cleanSub.length >= 2 && cleanMsg.length >= 2) { "Subject and message are required." }

        val ref = db.collection("dev_messages").document()
        return try {
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to sender.displayName.take(60).ifBlank { "Neo+ User" },
                "senderRank" to RANK_NEO_PLUS,
                "subject" to cleanSub,
                "message" to cleanMsg,
                "status" to "sent",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            Result.failure(e)
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

    suspend fun sendFriendRequest(currentUser: UserPrivate, target: LookupKey): Result<Pair<String, Long>> {
        val uid = requireUserId()
        require(target.userId != uid) { "You cannot add yourself as a friend." }

        val friendshipId = deterministicPairId(uid, target.userId)
        val docRef = db.collection("friendships").document(friendshipId)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(friendshipId to 0L)
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
            val awarded = rewardFriendPointsIfEligible(target.userId)
            Result.success(friendshipId to awarded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendship: Friendship): Result<Pair<String, Long>> {
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
            val awarded = rewardFriendPointsIfEligible(friendUid)

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
            Result.success(convId to awarded)
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
                        .filter { !it.hiddenFor.contains(uid) }
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
        replyToText: String? = null,
        messageType: String = "text",
        mediaData: String? = null,
        mediaDurationMs: Long? = null
    ): Result<String> {
        val uid = requireUserId()
        val safeMedia = mediaData?.take(495_000)?.takeIf { it.isNotBlank() }
        val cleanText = text.trim().take(2000)
        require(cleanText.isNotEmpty() || safeMedia != null) { "Message cannot be empty" }

        val previewLabel = when (messageType) {
            "image" -> if (cleanText.isNotEmpty()) "📷 $cleanText" else "📷 Photo"
            "audio" -> "🎤 Voice message"
            "sticker" -> "✨ Sticker"
            "gif" -> "🎞️ GIF"
            else -> cleanText
        }

        val msgRef = db.collection("conversations").document(conversation.id)
            .collection("messages").document()
        val convRef = db.collection("conversations").document(conversation.id)
        val senderRank = effectiveRank(sender)

        return try {
            val msgPayload = buildMap<String, Any> {
                put("conversationId", conversation.id)
                put("participantIds", conversation.participantIds)
                put("senderId", uid)
                put("senderName", sender.displayName)
                if (sender.photoUrl != null) put("senderPhoto", sender.photoUrl)
                put("senderRank", senderRank)
                put("text", cleanText)
                put("messageType", messageType)
                if (safeMedia != null) put("mediaData", safeMedia)
                if (mediaDurationMs != null && mediaDurationMs > 0L) put("mediaDurationMs", mediaDurationMs)
                val cleanReply = replyToText?.trim()?.take(200)
                if (!cleanReply.isNullOrEmpty()) put("replyToText", cleanReply)
                put("hiddenFor", emptyList<String>())
                put("deletedForEveryone", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            msgRef.set(msgPayload).await()

            convRef.update(
                mapOf(
                    "lastMessageText" to previewLabel.take(500),
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

    suspend fun deleteDirectMessage(
        conversationId: String,
        messageId: String,
        forEveryone: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        val convRef = db.collection("conversations").document(conversationId)
        return try {
            if (forEveryone) {
                msgRef.update(
                    mapOf(
                        "deletedForEveryone" to true,
                        "messageType" to "deleted",
                        "text" to "",
                        "mediaData" to FieldValue.delete(),
                        "mediaDurationMs" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Recompute latest visible message so conversation list doesn't retain deleted message preview
                val msgsSnap = db.collection("conversations").document(conversationId)
                    .collection("messages")
                    .whereArrayContains("participantIds", uid)
                    .get()
                    .await()
                val latestValid = msgsSnap.toObjects(
                    DirectMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .filter { it.id != messageId && it.deletedForEveryone != true && it.messageType != "deleted" }
                    .maxByOrNull { it.createdAt?.seconds ?: 0L }

                val nextPreview = when (latestValid?.messageType) {
                    null -> ""
                    "image" -> if (latestValid.text.isNotBlank()) "📷 ${latestValid.text}" else "📷 Photo"
                    "audio" -> "🎤 Voice message"
                    "sticker" -> "✨ Sticker"
                    "gif" -> "🎞️ GIF"
                    else -> latestValid.text.take(500)
                }
                convRef.update(
                    mapOf(
                        "lastMessageText" to nextPreview,
                        "lastMessageSenderId" to (latestValid?.senderId ?: uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                msgRef.update(
                    mapOf(
                        "hiddenFor" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
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

    suspend fun updateServerIcon(
        server: Server,
        newIconUrl: String?,
        newEmoji: String? = null,
        isNeoPlusMember: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        require(server.ownerId == uid || isNeoPlusMember) { "Only the server owner or Neo+ can edit server." }
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

            if (server.ownerId == uid) {
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

            if (upperCode.length in 4..24 && !trimmed.contains(" ")) {
                val codeSnap = db.collection("server_invites").document(upperCode).get().await()
                if (codeSnap.exists()) {
                    invite = codeSnap.toObject(ServerInvite::class.java)
                }
            }

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

    suspend fun deleteChannelFromServer(server: Server, channelName: String): Result<Unit> {
        requireUserId()
        require(server.channels.size > 1) { "A server must keep at least 1 text channel." }
        val updated = server.channels.filter { it != channelName }
        require(updated.isNotEmpty()) { "A server must keep at least 1 text channel." }
        val serverRef = db.collection("servers").document(server.id)
        return try {
            serverRef.update(
                mapOf(
                    "channels" to updated,
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

    suspend fun updateServerSettings(
        server: Server,
        name: String,
        description: String?,
        iconEmoji: String,
        accentHex: String,
        iconUrl: String?,
        isNeoPlusMember: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        require(server.ownerId == uid || isNeoPlusMember) { "Only the server owner or Neo+ can edit server settings." }
        val cleanName = name.trim().take(50)
        require(cleanName.length >= 2) { "Server name must be at least 2 characters." }
        val cleanEmoji = iconEmoji.trim().take(16).ifBlank { server.iconEmoji }
        val cleanDesc = description?.trim()?.take(200)?.takeIf { it.isNotBlank() }
        val cleanAccent = accentHex.trim().take(10).ifBlank { "#5865F2" }
        val cleanUrl = iconUrl?.take(120000)?.takeIf { it.isNotBlank() }

        val serverRef = db.collection("servers").document(server.id)
        return try {
            val payload = mutableMapOf<String, Any>(
                "name" to cleanName,
                "iconEmoji" to cleanEmoji,
                "accentHex" to cleanAccent,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanDesc != null) payload["description"] = cleanDesc else payload["description"] = FieldValue.delete()
            if (cleanUrl != null) payload["iconUrl"] = cleanUrl else payload["iconUrl"] = FieldValue.delete()
            serverRef.update(payload).await()

            if (server.ownerId == uid) {
                val inviteUpdate = mutableMapOf<String, Any>(
                    "serverName" to cleanName,
                    "iconEmoji" to cleanEmoji,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanUrl != null) inviteUpdate["iconUrl"] = cleanUrl else inviteUpdate["iconUrl"] = FieldValue.delete()
                db.collection("server_invites").document(server.inviteCode).update(inviteUpdate).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, serverRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeServerEvents(serverId: String, uid: String): Flow<List<ServerEvent>> = flow {
        val path = "servers/$serverId/events"
        emitAll(
            db.collection("servers").document(serverId).collection("events")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServerEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createServerEvent(
        server: Server,
        creator: UserPrivate,
        title: String,
        description: String?,
        scheduledLabel: String
    ): Result<ServerEvent> {
        val uid = requireUserId()
        val isNeoPlus = effectiveRank(creator) == RANK_NEO_PLUS
        require(server.ownerId == uid || isNeoPlus) { "Only the server owner or Neo+ can create events." }
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.length >= 2) { "Event title must be at least 2 characters." }
        val cleanSchedule = scheduledLabel.trim().take(60).ifBlank { "Soon" }
        val cleanDesc = description?.trim()?.take(300)?.takeIf { it.isNotBlank() }

        val eventRef = db.collection("servers").document(server.id).collection("events").document()
        return try {
            val payload = buildMap<String, Any> {
                put("serverId", server.id)
                put("creatorId", uid)
                put("creatorName", creator.displayName.take(60).ifBlank { "Member" })
                put("title", cleanTitle)
                if (cleanDesc != null) put("description", cleanDesc)
                put("scheduledLabel", cleanSchedule)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            eventRef.set(payload).await()
            Result.success(
                ServerEvent(
                    id = eventRef.id,
                    serverId = server.id,
                    creatorId = uid,
                    creatorName = creator.displayName,
                    title = cleanTitle,
                    description = cleanDesc,
                    scheduledLabel = cleanSchedule,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, eventRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteServerEvent(serverId: String, eventId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("servers").document(serverId).collection("events").document(eventId)
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

    fun observeUserCustomStickers(uid: String): Flow<List<CustomSticker>> = flow {
        val path = "users/$uid/custom_stickers"
        emitAll(
            db.collection("users").document(uid).collection("custom_stickers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomSticker::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeServerCustomStickers(serverId: String, uid: String): Flow<List<CustomSticker>> = flow {
        val path = "servers/$serverId/custom_stickers"
        emitAll(
            db.collection("servers").document(serverId).collection("custom_stickers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomSticker::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createCustomSticker(
        title: String,
        emoji: String,
        accentHex: String,
        imageData: String?,
        serverId: String? = null
    ): Result<CustomSticker> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(40).ifBlank { "Sticker" }
        val cleanEmoji = emoji.trim().take(16).ifBlank { "✨" }
        val cleanAccent = accentHex.trim().take(10).ifBlank { "#5865F2" }
        val cleanImg = imageData?.take(118_000)?.takeIf { it.isNotBlank() }

        val targetCol = if (!serverId.isNullOrBlank()) {
            db.collection("servers").document(serverId).collection("custom_stickers")
        } else {
            db.collection("users").document(uid).collection("custom_stickers")
        }
        val docRef = targetCol.document()
        return try {
            val payload = buildMap<String, Any> {
                put("creatorId", uid)
                if (!serverId.isNullOrBlank()) put("scopeId", serverId)
                put("title", cleanTitle)
                put("emoji", cleanEmoji)
                put("accentHex", cleanAccent)
                if (cleanImg != null) put("imageData", cleanImg)
                put("createdAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(
                CustomSticker(
                    id = docRef.id,
                    creatorId = uid,
                    scopeId = serverId,
                    title = cleanTitle,
                    emoji = cleanEmoji,
                    accentHex = cleanAccent,
                    imageData = cleanImg,
                    createdAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomSticker(stickerId: String, serverId: String? = null): Result<Unit> {
        val uid = requireUserId()
        val docRef = if (!serverId.isNullOrBlank()) {
            db.collection("servers").document(serverId).collection("custom_stickers").document(stickerId)
        } else {
            db.collection("users").document(uid).collection("custom_stickers").document(stickerId)
        }
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromServer(server: Server, targetUid: String): Result<Unit> {
        val uid = requireUserId()
        require(targetUid != server.ownerId) { "Cannot remove the server owner." }
        val serverRef = db.collection("servers").document(server.id)
        return try {
            serverRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(targetUid),
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
                        .filter { !it.hiddenFor.contains(uid) }
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
        replyToText: String? = null,
        messageType: String = "text",
        mediaData: String? = null,
        mediaDurationMs: Long? = null
    ): Result<String> {
        val uid = requireUserId()
        val safeMedia = mediaData?.take(495_000)?.takeIf { it.isNotBlank() }
        val cleanText = text.trim().take(2000)
        require(cleanText.isNotEmpty() || safeMedia != null) { "Message cannot be empty" }

        val msgRef = db.collection("servers").document(server.id)
            .collection("messages").document()
        val senderRank = effectiveRank(sender)
        return try {
            val payload = buildMap<String, Any> {
                put("serverId", server.id)
                put("channelName", channelName)
                put("memberUids", server.memberUids)
                put("senderId", uid)
                put("senderName", sender.displayName)
                if (sender.photoUrl != null) put("senderPhoto", sender.photoUrl)
                put("senderRank", senderRank)
                put("text", cleanText)
                put("messageType", messageType)
                if (safeMedia != null) put("mediaData", safeMedia)
                if (mediaDurationMs != null && mediaDurationMs > 0L) put("mediaDurationMs", mediaDurationMs)
                val cleanReply = replyToText?.trim()?.take(200)
                if (!cleanReply.isNullOrEmpty()) put("replyToText", cleanReply)
                put("hiddenFor", emptyList<String>())
                put("deletedForEveryone", false)
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

    suspend fun deleteServerMessage(
        serverId: String,
        messageId: String,
        forEveryone: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val ref = db.collection("servers").document(serverId)
            .collection("messages").document(messageId)
        return try {
            if (forEveryone) {
                ref.update(
                    mapOf(
                        "deletedForEveryone" to true,
                        "messageType" to "deleted",
                        "text" to "",
                        "mediaData" to FieldValue.delete(),
                        "mediaDurationMs" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "hiddenFor" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            Result.failure(e)
        }
    }

    // --- Real-Time Voice & Video Calls ---

    fun observeActiveCalls(uid: String): Flow<List<CallSession>> = flow {
        val path = "calls"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "ringing" || it.status == "active" }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun startCall(
        caller: UserPrivate,
        targetType: String,
        targetId: String,
        channelName: String?,
        calleeId: String?,
        calleeName: String,
        calleePhoto: String?,
        callType: String,
        participantIds: List<String>,
        videoQuality: String = if (effectiveRank(caller) != RANK_NORMAL) "720p" else "480p"
    ): Result<CallSession> {
        val uid = requireUserId()
        val callRef = db.collection("calls").document()
        val cleanParticipants = (participantIds + uid).distinct().take(25)
        val initialStatus = if (targetType == "server") "active" else "ringing"
        val isVideo = callType == "video"

        return try {
            val payload = buildMap<String, Any> {
                put("callerId", uid)
                put("callerName", caller.displayName.take(60).ifBlank { "neov user" })
                if (!caller.photoUrl.isNullOrBlank()) put("callerPhoto", caller.photoUrl.take(120000))
                if (!calleeId.isNullOrBlank()) put("calleeId", calleeId)
                put("calleeName", calleeName.take(60).ifBlank { "neov call" })
                if (!calleePhoto.isNullOrBlank()) put("calleePhoto", calleePhoto.take(120000))
                put("participantIds", cleanParticipants)
                put("targetType", targetType)
                put("targetId", targetId.take(128))
                if (!channelName.isNullOrBlank()) put("channelName", channelName.take(40))
                put("callType", callType)
                put("status", initialStatus)
                put("videoQuality", videoQuality)
                put("callerMuted", false)
                put("calleeMuted", false)
                put("callerVideoOn", isVideo)
                put("calleeVideoOn", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            callRef.set(payload).await()
            val created = CallSession(
                id = callRef.id,
                callerId = uid,
                callerName = caller.displayName,
                callerPhoto = caller.photoUrl,
                calleeId = calleeId,
                calleeName = calleeName,
                calleePhoto = calleePhoto,
                participantIds = cleanParticipants,
                targetType = targetType,
                targetId = targetId,
                channelName = channelName,
                callType = callType,
                status = initialStatus,
                videoQuality = videoQuality,
                callerMuted = false,
                calleeMuted = false,
                callerVideoOn = isVideo,
                calleeVideoOn = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun answerCall(callId: String, videoOn: Boolean): Result<Unit> {
        requireUserId()
        val callRef = db.collection("calls").document(callId)
        return try {
            callRef.update(
                mapOf(
                    "status" to "active",
                    "calleeVideoOn" to videoOn,
                    "calleeMuted" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun endOrDeclineCall(callId: String, status: String): Result<Unit> {
        requireUserId()
        val callRef = db.collection("calls").document(callId)
        return try {
            callRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateCallMediaState(
        callId: String,
        isCaller: Boolean,
        muted: Boolean,
        videoOn: Boolean
    ): Result<Unit> {
        requireUserId()
        val callRef = db.collection("calls").document(callId)
        return try {
            val payload = if (isCaller) {
                mapOf(
                    "callerMuted" to muted,
                    "callerVideoOn" to videoOn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "calleeMuted" to muted,
                    "calleeVideoOn" to videoOn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            callRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateCallVideoQuality(callId: String, quality: String): Result<Unit> {
        requireUserId()
        val callRef = db.collection("calls").document(callId)
        return try {
            callRef.update(
                mapOf(
                    "videoQuality" to quality,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun addParticipantToCall(call: CallSession, friendUid: String): Result<Unit> {
        requireUserId()
        if (call.participantIds.contains(friendUid) || call.participantIds.size >= 25) {
            return Result.success(Unit)
        }
        val callRef = db.collection("calls").document(call.id)
        return try {
            val updated = (call.participantIds + friendUid).distinct().take(25)
            callRef.update(
                mapOf(
                    "participantIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeCallStreams(callId: String, uid: String): Flow<List<CallStreamFrame>> = flow {
        val path = "calls/$callId/streams"
        emitAll(
            db.collection("calls").document(callId).collection("streams")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallStreamFrame::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun publishCallStreamFrame(
        callId: String,
        sender: UserPrivate,
        participantIds: List<String>,
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
        val uid = auth.currentUser?.uid ?: return
        val streamRef = db.collection("calls").document(callId)
            .collection("streams").document(uid)
        try {
            val payload = buildMap<String, Any> {
                put("callId", callId)
                put("senderId", uid)
                put("senderName", sender.displayName.take(60).ifBlank { "User" })
                put("participantIds", participantIds)
                if (!videoEnabled) {
                    put("frameData", FieldValue.delete())
                } else if (!frameData.isNullOrBlank()) {
                    put("frameData", frameData.take(245_000))
                }
                if (!audioData.isNullOrBlank()) {
                    put("audioData", audioData.take(58_000))
                    put("audioSeq", audioSeq.coerceAtLeast(0L))
                } else if (muted) {
                    put("audioSeq", audioSeq.coerceAtLeast(0L))
                }
                put("voiceEffect", voiceEffect.take(30))
                put("faceFilter", faceFilter.take(30))
                put("videoQuality", videoQuality)
                put("audioLevel", audioLevel.coerceIn(0.0, 1.0))
                put("videoEnabled", videoEnabled)
                put("muted", muted)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            streamRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Transient stream packet drops should not tear down an active call
        }
    }

    fun observeIncomingCallSignals(callId: String, uid: String): Flow<List<CallSignal>> = flow {
        val path = "calls/$callId/signals"
        emitAll(
            db.collection("calls").document(callId).collection("signals")
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun sendCallSignal(
        callId: String,
        recipientId: String,
        participantIds: List<String>,
        type: String,
        sdp: String? = null,
        candidate: String? = null,
        sdpMid: String? = null,
        sdpMLineIndex: Int? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        val sigRef = db.collection("calls").document(callId).collection("signals").document()
        try {
            val payload = buildMap<String, Any> {
                put("callId", callId)
                put("senderId", uid)
                put("recipientId", recipientId)
                put("participantIds", participantIds)
                put("type", type)
                if (!sdp.isNullOrBlank()) put("sdp", sdp.take(29_500))
                if (!candidate.isNullOrBlank()) put("candidate", candidate.take(2000))
                if (!sdpMid.isNullOrBlank()) put("sdpMid", sdpMid.take(64))
                if (sdpMLineIndex != null && sdpMLineIndex >= 0) put("sdpMLineIndex", sdpMLineIndex)
                put("createdAt", FieldValue.serverTimestamp())
            }
            sigRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sigRef.path, auth)
        }
    }

    suspend fun deleteCallSignal(callId: String, signalId: String) {
        if (signalId.isBlank()) return
        try {
            db.collection("calls").document(callId).collection("signals").document(signalId).delete().await()
        } catch (_: Exception) {
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

    data class DailyRewardStatus(
        val isClaimable: Boolean,
        val nextRewardAmount: Long,
        val remainingMs: Long,
        val claimsCompleted: Long
    )

    companion object {
        const val INITIAL_PEY_POINTS = 200L
        const val FRIEND_REWARD_PEY_POINTS = 200L
        const val DAILY_FIRST_PHASE_PEY = 100L
        const val DAILY_SUBSEQUENT_PEY = 50L
        const val CHANNEL_REWARD_PEY_POINTS = 200L
        const val CREATOR_CHANNEL_NAME = "fire ax9"
        const val OWNER_NAME_REWARD_PEY = 9_000_000L
        const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

        fun isOwnerChannelNameMatch(name: String?): Boolean {
            return name?.trim()?.equals(CREATOR_CHANNEL_NAME, ignoreCase = true) == true
        }

        const val RANK_NORMAL = "normal"
        const val RANK_ULTRA = "ultra"
        const val RANK_NEO_PLUS = "neo_plus"
        const val ULTRA_COST_PEY = 3500L
        const val NEO_PLUS_COST_PEY = 6000L
        const val RANK_DURATION_MS = 30L * ONE_DAY_MS
        const val ULTRA_TRIAL_DAYS = 3L
        const val NEO_PLUS_TRIAL_DAYS = 1L
        const val NEO_PLUS_QUEST_BONUS_PEY = 100L

        fun effectiveRank(profile: UserPrivate?, nowMs: Long = System.currentTimeMillis()): String {
            if (profile == null) return RANK_NORMAL
            if (profile.rank == RANK_NORMAL) return RANK_NORMAL
            if (profile.rankExpiresAtMs > 0L && nowMs > profile.rankExpiresAtMs) return RANK_NORMAL
            return if (profile.rank == RANK_ULTRA || profile.rank == RANK_NEO_PLUS) profile.rank else RANK_NORMAL
        }

        fun effectiveRank(profile: PublicProfile?, nowMs: Long = System.currentTimeMillis()): String {
            if (profile == null) return RANK_NORMAL
            if (profile.rank == RANK_NORMAL) return RANK_NORMAL
            if (profile.rankExpiresAtMs > 0L && nowMs > profile.rankExpiresAtMs) return RANK_NORMAL
            return if (profile.rank == RANK_ULTRA || profile.rank == RANK_NEO_PLUS) profile.rank else RANK_NORMAL
        }

        fun neoPlusRewardBonus(profile: UserPrivate?, nowMs: Long = System.currentTimeMillis()): Long {
            return if (effectiveRank(profile, nowMs) == RANK_NEO_PLUS) NEO_PLUS_QUEST_BONUS_PEY else 0L
        }

        fun computeDailyRewardStatus(
            profile: UserPrivate?,
            nowMs: Long = System.currentTimeMillis()
        ): DailyRewardStatus {
            if (profile == null) {
                return DailyRewardStatus(
                    isClaimable = false,
                    nextRewardAmount = DAILY_FIRST_PHASE_PEY,
                    remainingMs = ONE_DAY_MS,
                    claimsCompleted = 0L
                )
            }
            val nextAmount = if (profile.dailyClaimsCount < 2L) {
                DAILY_FIRST_PHASE_PEY
            } else {
                DAILY_SUBSEQUENT_PEY
            }
            val anchorMs = when {
                profile.lastDailyClaimAtMs > 0L -> profile.lastDailyClaimAtMs
                profile.createdAt != null -> profile.createdAt.toDate().time
                else -> nowMs
            }
            val unlockAtMs = anchorMs + ONE_DAY_MS
            val remaining = (unlockAtMs - nowMs).coerceAtLeast(0L)
            return DailyRewardStatus(
                isClaimable = remaining == 0L,
                nextRewardAmount = nextAmount,
                remainingMs = remaining,
                claimsCompleted = profile.dailyClaimsCount
            )
        }

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
