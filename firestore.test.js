const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_explorer";
const BOB_UID = "bob_friend";
const CHARLIE_UID = "charlie_stranger";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("meadowRooms").where("openForFriends", "==", true).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarId: "maya",
      stars: 0,
      butterfliesCaught: 0,
      discoveredIds: [],
      activeQuestId: "quest_butterflies",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Explorer profile: owner create, partial update, cross-user denial, and shadow field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Little Maya",
      avatarId: "maya",
      stars: 10,
      butterfliesCaught: 2,
      discoveredIds: ["pinky_monarch"],
      activeQuestId: "quest_butterflies",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      stars: 25,
      butterfliesCaught: 5,
      discoveredIds: ["pinky_monarch", "pink_lotus"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow field or @DocumentId `id` field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      stars: 30,
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      stars: 999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User quests subcollection: owner CRUD, list isolation, and terminal completion lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quests").doc("quest_butterflies").set({
      questId: "quest_butterflies",
      title: "Befriend 3 Pinky Butterflies",
      targetCount: 3,
      currentCount: 1,
      completed: false,
      rewardStars: 25,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owner list succeeds even on subcollection
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("quests").get());

  // Cross-user list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("quests").get());

  // Complete quest
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quests").doc("quest_butterflies").update({
      currentCount: 3,
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock prevents modifying completed quest
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("quests").doc("quest_butterflies").update({
      currentCount: 0,
      completed: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Multiplayer meadowRooms & players: filtered list queries, safe self-join, and anti-hijack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates a private friend room PINKY1
  await assertSucceeds(
    aliceDb.collection("meadowRooms").doc("PINKY1").set({
      roomCode: "PINKY1",
      roomName: "Pinky Blossom Picnic",
      hostUid: ALICE_UID,
      biomeTheme: "meadow",
      openForFriends: false,
      memberUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Unfiltered list query on meadowRooms must fail
  await assertFails(bobDb.collection("meadowRooms").get());

  // Filtered query by memberUids succeeds for Alice
  await assertSucceeds(
    aliceDb.collection("meadowRooms").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Filtered query for open rooms succeeds for Bob
  await assertSucceeds(
    bobDb.collection("meadowRooms").where("openForFriends", "==", true).get()
  );

  // Bob cannot kick Alice when joining PINKY1
  await assertFails(
    bobDb.collection("meadowRooms").doc("PINKY1").update({
      memberUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can safely add himself to memberUids
  await assertSucceeds(
    bobDb.collection("meadowRooms").doc("PINKY1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot update host settings
  await assertFails(
    bobDb.collection("meadowRooms").doc("PINKY1").update({
      roomName: "Hijacked Room",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice writes her player position inside PINKY1
  await assertSucceeds(
    aliceDb.collection("meadowRooms").doc("PINKY1").collection("players").doc(ALICE_UID).set({
      roomId: "PINKY1",
      playerUid: ALICE_UID,
      displayName: "Maya",
      avatarId: "maya",
      posX: 4.5,
      posZ: -2.0,
      yaw: 90.0,
      emote: "wave",
      roomOpen: false,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Charlie (not a member) cannot list players in private room PINKY1
  await assertFails(
    charlieDb
      .collection("meadowRooms")
      .doc("PINKY1")
      .collection("players")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );

  // Charlie cannot create a player doc before joining meadowRooms/PINKY1
  await assertFails(
    charlieDb.collection("meadowRooms").doc("PINKY1").collection("players").doc(CHARLIE_UID).set({
      roomId: "PINKY1",
      playerUid: CHARLIE_UID,
      displayName: "Stranger",
      avatarId: "pinky",
      posX: 0,
      posZ: 0,
      yaw: 0,
      emote: "",
      roomOpen: false,
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob (member) CAN list players in PINKY1 using memberUids filter
  await assertSucceeds(
    bobDb
      .collection("meadowRooms")
      .doc("PINKY1")
      .collection("players")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );

  // Unsafe free-text emote rejected by allowlist
  await assertFails(
    aliceDb.collection("meadowRooms").doc("PINKY1").collection("players").doc(ALICE_UID).update({
      emote: "unsafe_custom_text",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
