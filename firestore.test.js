const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const MALLORY_UID = "mallory_uid_003";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated user cannot read or write users, handles, chats, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastTime();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("handles").doc("alice").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice",
      pin: "1234",
      online: true,
      lastSeenAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User accounts: owner create, partial update, cross-user denial, and shadow field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastTime(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Vance",
      handle: "alice_v",
      pin: "4821",
      online: true,
      lastSeenAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Presence heartbeat update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      online: false,
      lastSeenAt: pastTime(1000),
      updatedAt: pastTime(1000),
    })
  );

  // Cross-user read/update denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastTime(1000),
    })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTime(1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      online: true,
      lastSeenAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Handles registry: exact get allowed, blanket list blocked, handle hijack blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastTime(5000);

  await assertSucceeds(
    aliceDb.collection("handles").doc("alice_v").set({
      userId: ALICE_UID,
      handle: "alice_v",
      displayName: "Alice Vance",
      pin: "4821",
      online: true,
      lastSeenAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Exact handle lookup succeeds for Bob
  await assertSucceeds(bobDb.collection("handles").doc("alice_v").get());

  // Blanket list of all handles fails
  await assertFails(bobDb.collection("handles").get());

  // Filtered own-handle list succeeds
  await assertSucceeds(
    aliceDb.collection("handles").where("userId", "==", ALICE_UID).get()
  );

  // Bob cannot overwrite or update Alice's handle
  await assertFails(
    bobDb.collection("handles").doc("alice_v").update({
      displayName: "Hijacked",
      updatedAt: pastTime(1000),
    })
  );
});

test("Chats & Messages: member isolation, aligned queries, replies, read receipts, and soft delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  const created = pastTime(8000);
  const chatId = "grp_cipher_squad";

  // Alice creates a group chat with Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatType: "GROUP",
      name: "Cipher Squad",
      creatorId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      memberLabels: {
        [ALICE_UID]: "alice_v|Alice Vance",
        [BOB_UID]: "bob_k|Bob Klein",
      },
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageAt: created,
      readByUids: [ALICE_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Aligned member query succeeds for Alice and Bob
  await assertSucceeds(
    aliceDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Unfiltered chats list fails
  await assertFails(aliceDb.collection("chats").get());

  // Non-member Mallory cannot read or update chat
  await assertFails(malloryDb.collection("chats").doc(chatId).get());
  await assertFails(
    malloryDb.collection("chats").doc(chatId).update({
      readByUids: [MALLORY_UID],
      updatedAt: pastTime(1000),
    })
  );

  // Alice sends a message (with optional reply fields omitted)
  const msgTime = pastTime(4000);
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice Vance",
      senderHandle: "alice_v",
      text: "MEET AT ZERO HOUR",
      memberUids: [ALICE_UID, BOB_UID],
      readByUids: [ALICE_UID],
      deleted: false,
      createdAt: msgTime,
      updatedAt: msgTime,
    })
  );

  // Bob sends a reply message (with reply fields included)
  const replyTime = pastTime(3000);
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob Klein",
      senderHandle: "bob_k",
      text: "ROGER THAT",
      memberUids: [ALICE_UID, BOB_UID],
      readByUids: [BOB_UID],
      deleted: false,
      replyToMessageId: "msg_1",
      replyToText: "MEET AT ZERO HOUR",
      replyToSenderName: "Alice Vance",
      createdAt: replyTime,
      updatedAt: replyTime,
    })
  );

  // Aligned messages query succeeds for Bob, fails for unfiltered query
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(chatId)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").get()
  );

  // Mallory cannot read or post messages to the chat
  await assertFails(
    malloryDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    malloryDb.collection("chats").doc(chatId).collection("messages").doc("msg_3").set({
      chatId,
      senderId: MALLORY_UID,
      senderName: "Mallory",
      senderHandle: "mallory",
      text: "INTRUSION",
      memberUids: [ALICE_UID, BOB_UID, MALLORY_UID],
      readByUids: [MALLORY_UID],
      deleted: false,
      createdAt: pastTime(1000),
      updatedAt: pastTime(1000),
    })
  );

  // Bob marks Alice's message as read
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      readByUids: [ALICE_UID, BOB_UID],
      updatedAt: pastTime(1000),
    })
  );

  // Bob cannot delete Alice's message
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").delete()
  );

  // Alice can soft-delete her own message
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      deleted: true,
      text: "[DELETED]",
      updatedAt: pastTime(1000),
    })
  );

  // Creator Alice can rename group and update members
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).update({
      name: "Swiss Grid Ops",
      memberUids: [ALICE_UID, BOB_UID, MALLORY_UID],
      memberLabels: {
        [ALICE_UID]: "alice_v|Alice Vance",
        [BOB_UID]: "bob_k|Bob Klein",
        [MALLORY_UID]: "mallory|Mallory",
      },
      updatedAt: pastTime(1000),
    })
  );

  // Non-creator Bob cannot rename group, but CAN leave group
  await assertFails(
    bobDb.collection("chats").doc(chatId).update({
      name: "Bobs Takeover",
      updatedAt: pastTime(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).update({
      memberUids: [ALICE_UID, MALLORY_UID],
      memberLabels: {
        [ALICE_UID]: "alice_v|Alice Vance",
        [MALLORY_UID]: "mallory|Mallory",
      },
      updatedAt: pastTime(1000),
    })
  );
});

test("Direct chat creation: participant can check non-existent deterministic DM ID before creating, outsider cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  const dmId = `dm_${ALICE_UID}_${BOB_UID}`;
  const created = pastTime(3000);

  // Alice checks whether deterministic DM exists before creating (matches createOrOpenDirectChat)
  await assertSucceeds(aliceDb.collection("chats").doc(dmId).get());

  // Outsider Mallory cannot probe Alice/Bob's non-existent DM
  await assertFails(malloryDb.collection("chats").doc(dmId).get());

  // Alice creates the DM
  await assertSucceeds(
    aliceDb.collection("chats").doc(dmId).set({
      chatType: "DIRECT",
      name: "Bob Klein",
      creatorId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      memberLabels: {
        [ALICE_UID]: "alice_v|Alice Vance",
        [BOB_UID]: "bob_k|Bob Klein",
      },
      lastMessageText: "",
      lastMessageSenderId: "",
      lastMessageAt: created,
      readByUids: [ALICE_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice can read it now, Mallory still cannot
  await assertSucceeds(aliceDb.collection("chats").doc(dmId).get());
  await assertFails(malloryDb.collection("chats").doc(dmId).get());
});

