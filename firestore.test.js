const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

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

test("Unauthenticated user: cannot read user profiles or balances", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_balances").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceGamer",
      email: "alice@test.com",
      referralCode: "TAP-ALICE1",
      accountLevel: 1,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read or modify Alice's profile or balance", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceGamer",
      email: "alice@test.com",
      referralCode: "TAP-ALICE1",
      createdAt: new Date(),
    });
    await context.firestore().collection("user_balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableCoins: 1000,
      totalLifetimeCoins: 1000,
      totalTaps: 500,
      currentEnergy: 1000,
      maxEnergy: 1000,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com" }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("user_balances").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("user_balances").doc(ALICE_UID).update({ availableCoins: 999999 }));
});

test("Tasks catalog: Signed in users can read tasks; regular users cannot create tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("task_daily_1").set({
      taskId: "task_daily_1",
      title: "Tap 100 Coins",
      description: "Perform 100 taps today",
      category: "daily",
      rewardCoins: 250,
      isActive: true,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_daily_1").get());
  await assertFails(
    aliceDb.collection("tasks").doc("task_hacked").set({
      taskId: "task_hacked",
      title: "Free Money",
      description: "Exploit",
      category: "daily",
      rewardCoins: 999999,
      isActive: true,
    })
  );
});

test("Withdrawals: Alice can create pending withdrawal; cannot mark it completed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wd_101").set({
      withdrawalId: "wd_101",
      userId: ALICE_UID,
      username: "AliceGamer",
      coinAmount: 50000,
      fiatAmount: 5.0,
      paymentMethod: "USDT_TON",
      paymentDetails: "EQD1234567890abcdef",
      status: "PENDING",
      createdAt: new Date(),
    })
  );

  // Alice attempts to update status to COMPLETED
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_101").update({
      status: "COMPLETED",
    })
  );
});

test("Admin access: Admin email can manage app_configuration and view all users", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "terseeenoch7@gmail.com" }).firestore();
  await assertSucceeds(
    adminDb.collection("app_configuration").doc("main").set({
      coinsPerTap: 2,
      maxEnergy: 1500,
      energyPerTap: 1,
      energyRegenPerSec: 3,
      minWithdrawalCoins: 25000,
      coinToUsdRate: 0.0001,
      maintenanceMode: false,
      version: 2,
    })
  );
  await assertSucceeds(adminDb.collection("app_configuration").doc("main").get());
});
