const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("servers").where("memberUids", "array-contains", ALICE_UID).get());
});

test("Private user profile isolates PII (email) and enforces P E Y + banner validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      status: "online",
      peyPoints: 200,
      ownedFrames: ["none"],
      equippedFrame: "none",
      rewardedFriendUids: [],
      bannerUrl: "data:image/jpeg;base64,abc123",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Alice can buy and equip a frame + a 5s profile video effect
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      peyPoints: 50,
      ownedFrames: ["none", "neon_cyber"],
      equippedFrame: "neon_cyber",
      ownedProfileEffects: ["none", "cyber_wave", "custom_video"],
      equippedProfileEffect: "custom_video",
      profileVideoData: "data:video/mp4;base64,AAAAIGZ0eXBpc29t",
      updatedAt: serverTimestamp(),
    })
  );

  // Alice syncs public_profiles with her 5s profile video effect & Bob can read it
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      status: "online",
      equippedFrame: "neon_cyber",
      equippedProfileEffect: "custom_video",
      profileVideoData: "data:video/mp4;base64,AAAAIGZ0eXBpc29t",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(bobDb.collection("public_profiles").doc(ALICE_UID).get());

  // Oversized video payload rejected (> 750000 chars)
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      profileVideoData: "a".repeat(750005),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read Alice's private user document
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Alice cannot inject ghost field (shadow update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );
  // Negative P E Y points rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      peyPoints: -50,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Lookup keys prevent list scraping but allow single-doc get", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("lookup_keys").doc("code_NEV-AL1C").set({
      userId: ALICE_UID,
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob can look up exact key via get
  await assertSucceeds(bobDb.collection("lookup_keys").doc("code_NEV-AL1C").get());
  // Bob cannot list/scrape lookup_keys
  await assertFails(bobDb.collection("lookup_keys").get());
  // Bob cannot overwrite Alice's lookup key
  await assertFails(
    bobDb.collection("lookup_keys").doc("code_NEV-AL1C").set({
      userId: BOB_UID,
      displayName: "Hijacked",
      friendCode: "NEV-AL1C",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Friendships enforce pending->accepted transition by recipient only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const fId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(aliceDb.collection("friendships").doc(fId).get());
  await assertFails(charlieDb.collection("friendships").doc(fId).get());

  await assertSucceeds(
    aliceDb.collection("friendships").doc(fId).set({
      participantIds: [ALICE_UID, BOB_UID],
      requesterId: ALICE_UID,
      recipientId: BOB_UID,
      requesterName: "Alice",
      recipientName: "Bob",
      requesterCode: "NEV-AL1C",
      recipientCode: "NEV-B0B1",
      status: "pending",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(charlieDb.collection("friendships").doc(fId).get());

  await assertSucceeds(
    bobDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: serverTimestamp(),
    })
  );
});

test("DM conversations and messages enforce participant isolation and sender identity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const convId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantAId: ALICE_UID,
      participantAName: "Alice",
      participantBId: BOB_UID,
      participantBName: "Bob",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hey Bob!",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(
    charlieDb.collection("conversations").doc(convId).collection("messages").doc("msg_2").set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Intruder",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Servers allow custom iconUrl, name-based lookup, invite join, and channel messaging", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const serverId = "srv_gaming_01";
  const inviteCode = "NEV777";
  const nameKey = "srvname_1234567890abcdef1234567890abcdef";

  await assertSucceeds(
    aliceDb.collection("servers").doc(serverId).set({
      name: "Gaming Squad",
      iconEmoji: "🎮",
      iconUrl: "data:image/jpeg;base64,srvimg",
      accentHex: "#5865F2",
      ownerId: ALICE_UID,
      inviteCode: inviteCode,
      memberUids: [ALICE_UID],
      channels: ["general", "gaming"],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("server_invites").doc(inviteCode).set({
      inviteCode: inviteCode,
      serverId: serverId,
      serverName: "Gaming Squad",
      iconEmoji: "🎮",
      iconUrl: "data:image/jpeg;base64,srvimg",
      ownerId: ALICE_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("server_invites").doc(nameKey).set({
      inviteCode: nameKey,
      serverId: serverId,
      serverName: "Gaming Squad",
      iconEmoji: "🎮",
      iconUrl: "data:image/jpeg;base64,srvimg",
      ownerId: ALICE_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner updates server iconUrl; non-owner Bob cannot
  await assertSucceeds(
    aliceDb.collection("servers").doc(serverId).update({
      iconUrl: "data:image/jpeg;base64,updated",
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(charlieDb.collection("servers").doc(serverId).get());

  // Bob looks up by nameKey and joins server
  await assertSucceeds(bobDb.collection("server_invites").doc(nameKey).get());
  await assertSucceeds(
    bobDb.collection("servers").doc(serverId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot alter server iconUrl
  await assertFails(
    bobDb.collection("servers").doc(serverId).update({
      iconUrl: "data:image/jpeg;base64,hijack",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("servers").doc(serverId).collection("messages").doc("smsg_1").set({
      serverId: serverId,
      channelName: "general",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Hello squad!",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    charlieDb.collection("servers").doc(serverId).collection("messages").doc("smsg_2").set({
      serverId: serverId,
      channelName: "general",
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Not a member",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("User can claim daily P E Y rewards and fire ax9 channel bonus once", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      status: "online",
      peyPoints: 200,
      dailyClaimsCount: 0,
      lastDailyClaimAtMs: 0,
      channelRewardClaimed: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      peyPoints: 500,
      dailyClaimsCount: 1,
      lastDailyClaimAtMs: 1700000000000,
      channelRewardClaimed: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Voice and video calls enforce participant isolation and sender-matched streams", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const callId = "call_dm_alice_bob";

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      targetType: "dm",
      targetId: `${ALICE_UID}_${BOB_UID}`,
      callType: "video",
      status: "ringing",
      callerMuted: false,
      calleeMuted: false,
      callerVideoOn: true,
      calleeVideoOn: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob can answer the call
  await assertSucceeds(
    bobDb.collection("calls").doc(callId).update({
      status: "active",
      calleeVideoOn: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Charlie cannot read or join the private call
  await assertFails(charlieDb.collection("calls").doc(callId).get());

  // Bob publishes his own stream frame
  await assertSucceeds(
    bobDb.collection("calls").doc(callId).collection("streams").doc(BOB_UID).set({
      callId: callId,
      senderId: BOB_UID,
      senderName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      frameData: "data:image/jpeg;base64,fakeframe",
      audioLevel: 0.45,
      videoEnabled: true,
      muted: false,
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot spoof Alice's stream doc
  await assertFails(
    bobDb.collection("calls").doc(callId).collection("streams").doc(ALICE_UID).set({
      callId: callId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      audioLevel: 0.1,
      videoEnabled: false,
      muted: false,
      updatedAt: serverTimestamp(),
    })
  );

  // Charlie cannot read streams
  await assertFails(
    charlieDb.collection("calls").doc(callId).collection("streams").doc(BOB_UID).get()
  );

  // Alice expands call for multi-person (Neo Plus) & sets 4K quality + Bob sends audio packet
  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).update({
      participantIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      videoQuality: "4K",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("calls").doc(callId).collection("streams").doc(BOB_UID).update({
      participantIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      audioData: "AQIDBAUGBwg=",
      audioSeq: 7,
      voiceEffect: "deep",
      faceFilter: "dog_ears",
      videoQuality: "1080p",
      updatedAt: serverTimestamp(),
    })
  );
});

test("DM Delete-for-Me (hiddenFor) vs Delete-for-Everyone and rich media payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const convId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantAId: ALICE_UID,
      participantAName: "Alice",
      participantBId: BOB_UID,
      participantBName: "Bob",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_rich").set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRank: "ultra",
      text: "Voice clip",
      messageType: "audio",
      mediaData: "UklGRiQAAABXQVZF",
      mediaDurationMs: 2400,
      hiddenFor: [],
      deletedForEveryone: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob can Delete-for-Me (add only his own UID to hiddenFor)
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_rich").update({
      hiddenFor: [BOB_UID],
      updatedAt: serverTimestamp(),
    })
  );

  // Bob CANNOT delete Alice's message for everyone
  await assertFails(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_rich").update({
      deletedForEveryone: true,
      text: "Message deleted",
      updatedAt: serverTimestamp(),
    })
  );

  // Alice CAN delete her own message for everyone
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_rich").update({
      deletedForEveryone: true,
      messageType: "deleted",
      text: "تم حذف هذه الرسالة",
      mediaData: null,
      updatedAt: serverTimestamp(),
    })
  );

  // Charlie cannot hide a DM message he doesn't belong to
  await assertFails(
    charlieDb.collection("conversations").doc(convId).collection("messages").doc("msg_rich").update({
      hiddenFor: [BOB_UID, CHARLIE_UID],
      updatedAt: serverTimestamp(),
    })
  );
});

test("Ranks, Ultra directory query isolation, Neo Plus custom quests, and developer tickets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice is Neo Plus
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      status: "online",
      rank: "neo_plus",
      rankExpiresAtMs: Date.now() + 86400000,
      neoPlusTrialUsed: true,
      profileThemeColorHex: "#FFD166",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      friendCode: "NEV-AL1C",
      status: "online",
      rank: "neo_plus",
      rankExpiresAtMs: Date.now() + 86400000,
      profileThemeColorHex: "#FFD166",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob is Normal rank
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@gmail.com",
      displayName: "Bob",
      friendCode: "NEV-B0B1",
      status: "online",
      rank: "normal",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("public_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      friendCode: "NEV-B0B1",
      status: "online",
      rank: "normal",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Ultra directory query succeeds for ultra/neo_plus filter, unfiltered list fails
  await assertSucceeds(
    bobDb.collection("public_profiles").where("rank", "in", ["ultra", "neo_plus"]).get()
  );
  await assertFails(
    bobDb.collection("public_profiles").get()
  );

  // Alice (Neo Plus) can create custom quests and developer messages
  await assertSucceeds(
    aliceDb.collection("custom_quests").doc("q_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      creatorRank: "neo_plus",
      title: "Add 5 friends for 300 each",
      questType: "add_friends",
      targetCount: 5,
      rewardPerStep: 300,
      totalReward: 1500,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("dev_messages").doc("t_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRank: "neo_plus",
      subject: "Feature idea",
      message: "Great app!",
      status: "sent",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob (Normal) CANNOT create custom quests or dev messages
  await assertFails(
    bobDb.collection("custom_quests").doc("q_2").set({
      creatorId: BOB_UID,
      creatorName: "Bob",
      creatorRank: "neo_plus",
      title: "Fake quest",
      questType: "add_friends",
      targetCount: 5,
      rewardPerStep: 300,
      totalReward: 1500,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    bobDb.collection("dev_messages").doc("t_2").set({
      senderId: BOB_UID,
      senderName: "Bob",
      senderRank: "neo_plus",
      subject: "Spoof",
      message: "Nope",
      status: "sent",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob CAN read custom quests
  await assertSucceeds(bobDb.collection("custom_quests").get());
  // Bob CANNOT read Alice's dev tickets
  await assertFails(bobDb.collection("dev_messages").doc("t_1").get());
});

