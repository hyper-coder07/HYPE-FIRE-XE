const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts) {
  return {
    userId: uid,
    callsign: "ApexWolf",
    diamonds: 600,
    goldCoins: 2500,
    booyahs: 0,
    eliminations: 0,
    matchesPlayed: 0,
    equippedOutfitId: "outfit_recruit",
    equippedWeaponSkinId: "weapon_factory",
    equippedParachuteId: "chute_standard",
    ownedCosmeticIds: ["outfit_recruit", "weapon_factory", "chute_standard"],
    graphicsPreset: "BALANCED",
    targetFps: 45,
    resolutionScale: 0.85,
    shadowsEnabled: true,
    particlesEnabled: true,
    aimAssistEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write profiles or matches", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
});

test("Authenticated owner can create profile, equip cosmetics, buy shop items, and log match", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Action: Purchase Cosmetic
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      diamonds: 300,
      ownedCosmeticIds: [
        "outfit_recruit",
        "weapon_factory",
        "chute_standard",
        "outfit_crimson_striker",
      ],
      equippedOutfitId: "outfit_crimson_striker",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Action: Update Graphics Settings
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      graphicsPreset: "ULTRA_LOW",
      targetFps: 30,
      resolutionScale: 0.65,
      shadowsEnabled: false,
      particlesEnabled: false,
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Action: Record Match Rewards
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      diamonds: 350,
      goldCoins: 2900,
      booyahs: 1,
      eliminations: 7,
      matchesPlayed: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create MatchRecord subcollection doc (no @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_001").set({
      userId: ALICE_UID,
      placement: 1,
      eliminations: 7,
      damageDealt: 1450,
      survivalSeconds: 185,
      goldEarned: 400,
      diamondsEarned: 50,
      mode: "BERMUDA_BLITZ",
      booyah: true,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // List own matches succeeds (even without extra filter, scoped by path wildcard)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
});

test("Reject shadow update, immutable createdAt mutation, and @DocumentId id field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Shadow/ghost field injection rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      diamonds: 999,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Unexpected `id` field in match payload rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_bad").set({
      id: "match_bad",
      userId: ALICE_UID,
      placement: 2,
      eliminations: 3,
      damageDealt: 600,
      survivalSeconds: 120,
      goldEarned: 150,
      diamondsEarned: 10,
      mode: "BERMUDA_BLITZ",
      booyah: false,
      createdAt: created,
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's profile or matches", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("m1").set({
      userId: ALICE_UID,
      placement: 3,
      eliminations: 4,
      damageDealt: 800,
      survivalSeconds: 140,
      goldEarned: 200,
      diamondsEarned: 15,
      mode: "CLASH_DROP",
      booyah: false,
      createdAt: created,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      diamonds: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("matches").get());
  await assertFails(bobDb.collection("users").get());
});
