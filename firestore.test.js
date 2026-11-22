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

test("Unauthenticated user cannot read or write profiles, tasks, or checkins", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "喝一口水",
        effortLevel: "micro",
        completed: false,
        completedDate: "",
        note: "",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid RehabProfile create and update succeed; ghost fields, negative antibodies & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "小廢",
      feifeiNickname: "主治醫師",
      todayDate: "2026-11-18",
      todayCount: 0,
      totalCompleted: 0,
      antibodyBalance: 60,
      ownedItemIds: [],
      equippedItemIds: [],
      wardPlacements: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      todayDate: "2026-11-18",
      todayCount: 1,
      totalCompleted: 1,
      antibodyBalance: 75,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Redeem and place pharmacy item in ward
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      antibodyBalance: 45,
      ownedItemIds: ["pill_victory"],
      equippedItemIds: ["pill_victory"],
      wardPlacements: ["pill_victory:0.78:0.56"],
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Reject negative antibody balance
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      antibodyBalance: -10,
      updatedAt: new Date(Date.now() - 600),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      todayCount: 99,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: valid RehabTask create, toggle, shrink, and query succeed; invalid writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create without @DocumentId 'id' field
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        title: "站起來伸懶腰 10 秒",
        effortLevel: "micro",
        completed: false,
        completedDate: "",
        note: "不用做到完美，動一下就好",
        createdAt: created,
        updatedAt: created,
      })
  );

  // List subcollection succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );

  // Partial update: toggle completion
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        completed: true,
        completedDate: "2026-11-18",
        updatedAt: new Date(Date.now() - 2000),
      })
  );

  // Partial update: shrink/edit task & toggle split mode with morning/noon/evening flags
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        title: "先深呼吸一次就好",
        effortLevel: "micro",
        note: "廢廢幫你切小塊了",
        completed: true,
        timeSlotsEnabled: true,
        morningDone: true,
        noonDone: false,
        eveningDone: true,
        morningEnabled: true,
        noonEnabled: false,
        eveningEnabled: true,
        resetHour: 4,
        deadlineDate: "2026-11-25",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Partial update: reorder sortOrder
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        sortOrder: 3,
        updatedAt: new Date(Date.now() - 900),
      })
  );

  // Reject invalid resetHour > 23
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .update({
        resetHour: 25,
        updatedAt: new Date(Date.now() - 800),
      })
  );

  // Reject @DocumentId 'id' in payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_2")
      .set({
        id: "task_2",
        userId: ALICE_UID,
        title: "Extra ID",
        effortLevel: "micro",
        completed: false,
        completedDate: "",
        note: "",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject invalid effortLevel enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_3")
      .set({
        userId: ALICE_UID,
        title: "Bad effort",
        effortLevel: "extreme",
        completed: false,
        completedDate: "",
        note: "",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_4")
      .set({
        userId: ALICE_UID,
        title: "Future task",
        effortLevel: "micro",
        completed: false,
        completedDate: "",
        note: "",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .delete()
  );
});

test("Authenticated owner: valid RehabCheckin create, update, and list succeed; cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("checkins")
      .doc("2026-11-18")
      .set({
        userId: ALICE_UID,
        dateKey: "2026-11-18",
        energyLevel: "flat",
        energyPercent: 12,
        reflection: "今天沙發引力特別強，但我有喝水了！",
        wardSnapshot: ["pill_victory:0.78:0.56"],
        doctorPoseSnapshot: "SIPPING",
        completedTaskTitles: ["喝一口水"],
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("checkins")
      .doc("2026-11-18")
      .update({
        energyLevel: "low",
        energyPercent: 34,
        reflection: "慢慢醒過來中",
        wardSnapshot: ["pill_victory:0.78:0.56", "miracle_banner:0.50:0.15"],
        doctorPoseSnapshot: "APPROVE",
        completedTaskTitles: ["喝一口水", "站起來伸懶腰 10 秒"],
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject invalid energyPercent > 100
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("checkins")
      .doc("2026-11-18")
      .update({
        energyPercent: 150,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("checkins").get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("checkins")
      .doc("2026-11-18")
      .get()
  );
});
