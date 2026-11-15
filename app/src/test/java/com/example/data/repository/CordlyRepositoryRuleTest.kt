package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CordlyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_awardsInitialPey_andLookupFriendByEmail_succeeds() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "alice_$suffix@gmail.com"
        val bobEmail = "bob_$suffix@gmail.com"

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CordlyRepository(firestore, auth)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        assertEquals(aliceUid, aliceProfile.userId)
        assertTrue(aliceProfile.friendCode.startsWith("NEV-"))
        assertEquals(200L, aliceProfile.peyPoints)

        signInTestUser(bobEmail)
        val bobRepo = CordlyRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }

        // Bob can find Alice by her Gmail address without reading her private PII doc
        val foundAlice = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.findUserByEmailOrCode(aliceEmail).getOrThrow()
        }
        assertNotNull(foundAlice)
        assertEquals(aliceUid, foundAlice?.userId)
    }

    @Test
    fun friendRequest_awardsPey_andShopPurchase_andDirectMessage_realtimeFlowWorks() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "alice_dm_$suffix@gmail.com"
        val bobEmail = "bob_dm_$suffix@gmail.com"

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CordlyRepository(firestore, auth)
        val aliceUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }

        val bobUid = signInTestUser(bobEmail)
        val bobRepo = CordlyRepository(firestore, auth)
        val bobUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }

        // Alice sends friend request to Bob (+200 P E Y -> 400 P E Y)
        signInTestUser(aliceEmail)
        val bobLookup = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.findUserByEmailOrCode(bobEmail).getOrThrow()!!
        }
        val friendshipId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendFriendRequest(aliceUser, bobLookup).getOrThrow()
        }
        val aliceAfterFriend = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeOwnProfile(aliceUid).first { it?.peyPoints == 400L }
        }
        assertEquals(400L, aliceAfterFriend?.peyPoints)

        // Alice buys a 150 P E Y frame in the shop (400 - 150 = 250 P E Y)
        val newBalance = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.purchaseAndEquipFrame("neon_cyber", 150L).getOrThrow()
        }
        assertEquals(250L, newBalance)

        // Alice buys a 100 P E Y built-in 5s profile video effect (250 - 100 = 150 P E Y)
        val afterVideoBalance = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.purchaseAndEquipProfileEffect("cyber_wave", 100L).getOrThrow()
        }
        assertEquals(150L, afterVideoBalance)

        // Alice uploads a custom 5s video effect (free equip once unlocked / or custom upload)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.purchaseAndEquipProfileEffect("custom_video", 150L, "data:video/mp4;base64,AAAAIGZ0eXBpc29t").getOrThrow()
        }

        // Bob observes Alice's PublicProfile and sees her equipped 5s profile video effect
        signInTestUser(bobEmail)
        val bobRepoAliceCard = CordlyRepository(firestore, auth)
        val alicePublic = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepoAliceCard.observePublicProfile(aliceUid).first { it?.equippedProfileEffect == "custom_video" }
        }
        assertEquals("data:video/mp4;base64,AAAAIGZ0eXBpc29t", alicePublic?.profileVideoData)
        assertEquals("neon_cyber", alicePublic?.equippedFrame)

        // Bob observes pending friendship and accepts (+200 P E Y -> 400 P E Y)
        signInTestUser(bobEmail)
        val friendships = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeFriendships(bobUid).first { list -> list.any { it.id == friendshipId } }
        }
        val request = friendships.first { it.id == friendshipId }
        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.acceptFriendRequest(request).getOrThrow()
        }
        val bobAfterAccept = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeOwnProfile(bobUid).first { it?.peyPoints == 400L }
        }
        assertEquals(400L, bobAfterAccept?.peyPoints)

        // Bob updates banner and avatar
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.updateProfileStatus(
                status = "dnd",
                customStatus = "Gaming!",
                displayName = "Bob Pro",
                photoUrl = "data:image/jpeg;base64,bobavatar",
                bannerUrl = "data:image/jpeg;base64,bobbanner"
            ).getOrThrow()
        }

        // Bob sends a DM to Alice
        val convs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeConversations(bobUid).first { list -> list.any { it.id == convId } }
        }
        val conv = convs.first { it.id == convId }
        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.sendDirectMessage(conv, bobUser, "Salam Alice!").getOrThrow()
        }

        // Alice observes DM in real time
        signInTestUser(aliceEmail)
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeDirectMessages(convId, aliceUser.userId)
                .first { list -> list.any { it.id == msgId } }
        }
        assertTrue(messages.any { it.id == msgId && it.text == "Salam Alice!" })
    }

    @Test
    fun createServer_updateIcon_andJoinByNameOnly_succeeds() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "alice_srv_$suffix@gmail.com"
        val bobEmail = "bob_srv_$suffix@gmail.com"
        val serverName = "Neov Arena $suffix"

        signInTestUser(aliceEmail)
        val aliceRepo = CordlyRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }
        val server = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createServer(
                name = serverName,
                description = "Hangout",
                iconEmoji = "🎮",
                accentHex = "#5865F2",
                iconUrl = "data:image/jpeg;base64,srvicon"
            ).getOrThrow()
        }
        assertEquals("data:image/jpeg;base64,srvicon", server.iconUrl)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateServerIcon(server, "data:image/jpeg;base64,srvicon2", "🔥").getOrThrow()
        }

        val bobUid = signInTestUser(bobEmail)
        val bobRepo = CordlyRepository(firestore, auth)
        val bobUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }

        // Bob joins by typing ONLY the Server Name!
        val joinedServer = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinServerByInvite(serverName).getOrThrow()
        }
        assertTrue(joinedServer.memberUids.contains(bobUid))
        assertEquals("data:image/jpeg;base64,srvicon2", joinedServer.iconUrl)

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.sendServerMessage(joinedServer, "general", bobUser, "Hey squad!").getOrThrow()
        }

        val serverMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeServerMessages(joinedServer.id, bobUid)
                .first { list -> list.any { it.id == msgId } }
        }
        assertTrue(serverMsgs.any { it.id == msgId && it.text == "Hey squad!" })
    }

    @Test
    fun observeServers_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CordlyRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeServers("unauth_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun crossUserPrivateProfileRead_failsWithPermissionDenied() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "alice_iso_$suffix@gmail.com"
        val bobEmail = "bob_iso_$suffix@gmail.com"

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CordlyRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(checkNotNull(auth.currentUser)).getOrThrow()
        }

        signInTestUser(bobEmail)
        val bobRepo = CordlyRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeOwnProfile(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when reading another user's private profile")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
