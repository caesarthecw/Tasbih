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

function validProfile(uid, now) {
  return {
    userId: uid,
    displayName: "أحمد",
    coins: 120,
    totalLifetimeTasbih: 0,
    todayTasbih: 0,
    lastActiveDate: "2026-11-17",
    streakDays: 1,
    activeDhikrId: "subhanallah",
    selectedBeadColorId: "bead_amber",
    customBeadHex: "#C78D3B",
    selectedThemeId: "theme_emerald",
    selectedBackgroundId: "bg_minimal_ivory",
    selectedPlantId: "plant_olive",
    soundEnabled: true,
    soundVolume: 0.75,
    soundTone: "water_drop",
    hapticEnabled: true,
    unlockedItems: ["bead_amber", "theme_emerald", "bg_minimal_ivory", "plant_olive"],
    vipUnlocked: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validDhikr(uid, now) {
  return {
    userId: uid,
    phrase: "سبحان الله",
    subtitle: "تسبيح وتنزيه",
    dailyGoal: 33,
    todayCount: 0,
    totalCount: 0,
    lastUpdatedDate: "2026-11-17",
    accentHex: "#0F6B56",
    sortOrder: 0,
    customEntry: false,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user: denied across profile, adhkar, dailyLogs, and purchases", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("adhkar").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("purchases").get()
  );
});

test("Owner profile create, action-based updates, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create valid profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created))
  );

  // Action 1: Tasbih increment update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalLifetimeTasbih: 33,
      todayTasbih: 33,
      coins: 125,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action 2: Sound & customization preference update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      soundTone: "wood_bead",
      soundVolume: 0.5,
      customBeadHex: "#1E88E5",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 3: Store unlock update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 75,
      unlockedItems: [
        "bead_amber",
        "theme_emerald",
        "bg_minimal_ivory",
        "plant_olive",
        "plant_lotus",
      ],
      selectedPlantId: "plant_lotus",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 200,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read & write fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      coins: 0,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Dhikr subcollection create, list, update, delete, and @DocumentId exclusion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("adhkar")
      .doc("subhanallah")
      .set(validDhikr(ALICE_UID, created))
  );

  // List own adhkar
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("adhkar").get()
  );

  // Update count
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("adhkar")
      .doc("subhanallah")
      .update({
        todayCount: 12,
        totalCount: 12,
        lastUpdatedDate: "2026-11-17",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject payload containing @DocumentId 'id' field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("adhkar")
      .doc("alhamdulillah")
      .set({
        id: "alhamdulillah",
        ...validDhikr(ALICE_UID, created),
      })
  );

  // Cross-user list/write fails
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("adhkar").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("adhkar")
      .doc("subhanallah")
      .delete()
  );
});

test("DailyLogs and immutable Purchases subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyLogs")
      .doc("2026-11-17")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-17",
        totalCount: 33,
        coinsEarned: 5,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Invalid dateKey fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyLogs")
      .doc("bad-date")
      .set({
        userId: ALICE_UID,
        dateKey: "bad-date",
        totalCount: 10,
        coinsEarned: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Purchase record create succeeds, update fails (immutable ledger)
  const purchaseRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("purchases")
    .doc("tx_001");

  await assertSucceeds(
    purchaseRef.set({
      userId: ALICE_UID,
      productId: "ghars_coins_500",
      title: "حزمة ٥٠٠ عملة غرس",
      purchaseType: "play_billing_coins",
      coinDelta: 500,
      priceDisplay: "$1.99",
      orderToken: "play_token_abc",
      createdAt: created,
    })
  );

  await assertFails(
    purchaseRef.update({
      coinDelta: 99999,
    })
  );
});
