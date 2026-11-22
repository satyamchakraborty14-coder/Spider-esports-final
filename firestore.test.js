const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "satyamchakraborty14@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournamentCategories").where("enabled", "==", true).get());
  await assertFails(unauthDb.collection("tournaments").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("wallets").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile isolation, self-creation bounds, and stat immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "spider_alice",
      displayName: "Alice",
      email: "alice@example.com",
      photoUrl: "",
      playerUid: "SE100101",
      inGameName: "AliceOP",
      inGameUid: "99887766",
      emailVerified: true,
      referralCode: "SPDRALICE",
      referredBy: "",
      language: "EN",
      status: "ACTIVE",
      matchesPlayed: 0,
      wins: 0,
      verifiedKills: 0,
      rankingPoints: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Allowed profile update (mutable fields only)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      inGameName: "AlicePro",
      updatedAt: pastDate(1000),
    })
  );

  // User cannot escalate stats or status
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      rankingPoints: 9999,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user PII read/list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing on admin check is rejected when email_verified is false", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const ts = pastDate();

  await assertFails(
    spoofedAdminDb.collection("tournamentCategories").doc("cat_full_map").set({
      categoryName: "FULL MAP",
      categoryImageUrl: "",
      categoryType: "BATTLE_ROYALE",
      presetKey: "battle_royale",
      enabled: true,
      randomTournament: false,
      displayOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Admin can create categories, banners, and tournaments; regular users cannot modify them", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    adminDb.collection("tournamentCategories").doc("cat_full_map").set({
      categoryName: "FULL MAP",
      categoryImageUrl: "",
      categoryType: "BATTLE_ROYALE",
      presetKey: "battle_royale",
      enabled: true,
      randomTournament: false,
      displayOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("tournamentCategories").doc("cat_hacked").set({
      categoryName: "HACKED",
      categoryImageUrl: "",
      categoryType: "CUSTOM",
      enabled: true,
      randomTournament: false,
      displayOrder: 99,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query succeeds, unfiltered query fails for non-admin
  await assertSucceeds(aliceDb.collection("tournamentCategories").where("enabled", "==", true).get());
  await assertFails(aliceDb.collection("tournamentCategories").get());
});

test("Atomic free-entry tournament registration & room credential protection", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_free_1").set({
      categoryId: "cat_free_match",
      title: "Spider Free Scrim #1",
      shortDescription: "Free entry qualifier",
      gameMode: "FREE MATCH",
      mapName: "Bermuda",
      entryFee: 0,
      prizePool: 0,
      perKillReward: 0,
      maxParticipants: 48,
      registeredCount: 0,
      matchDate: "2026-11-22",
      matchTime: "20:00",
      registrationDeadline: "2026-11-22 19:45",
      rules: "No emulators, fair play.",
      bannerUrl: "",
      presetKey: "battle_royale",
      visibility: "PUBLIC",
      status: "UPCOMING",
      soloFormat: true,
      randomTournament: false,
      roomReleased: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_free_1").collection("credentials").doc("room").set({
      tournamentId: "tour_free_1",
      roomId: "88442211",
      roomPassword: "SPDR",
      released: true,
      releaseNotice: "Join 5 mins early",
      updatedAt: ts,
    })
  );

  // Unregistered Bob cannot read room credentials
  await assertFails(
    bobDb.collection("tournaments").doc("tour_free_1").collection("credentials").doc("room").get()
  );

  // Atomic batch registration for Alice
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("registrations").doc(`tour_free_1_${ALICE_UID}`), {
    tournamentId: "tour_free_1",
    tournamentTitle: "Spider Free Scrim #1",
    categoryId: "cat_free_match",
    userId: ALICE_UID,
    username: "spider_alice",
    inGameName: "AliceOP",
    inGameUid: "99887766",
    entryFeePaid: 0,
    status: "CONFIRMED",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.update(aliceDb.collection("tournaments").doc("tour_free_1"), {
    registeredCount: 1,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Now registered Alice can read room credentials
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tour_free_1").collection("credentials").doc("room").get()
  );
});

test("Wallets and transactions cannot be modified by regular users", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    adminDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      totalBalance: 100,
      depositedBalance: 50,
      winningBalance: 50,
      earningsBalance: 0,
      payoutsTotal: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("wallets").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("wallets").doc(ALICE_UID).update({ totalBalance: 99999, updatedAt: ts }));
  await assertFails(
    aliceDb.collection("transactions").doc("tx_fake").set({
      userId: ALICE_UID,
      type: "DEPOSIT",
      direction: "CREDIT",
      description: "Fake credit",
      amount: 5000,
      status: "COMPLETED",
      resultingBalance: 5100,
      createdAt: ts,
    })
  );
});

test("Unique username, phone, gameUid, and email claims prevent duplicates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Case-insensitive username claim: only lowercase ID allowed
  await assertFails(
    aliceDb.collection("usernames").doc("SpiderX").set({
      userId: ALICE_UID,
      username: "SpiderX",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("usernames").doc("spiderx").set({
      userId: ALICE_UID,
      username: "SpiderX",
      createdAt: ts,
    })
  );
  // Bob cannot claim spiderx
  await assertFails(
    bobDb.collection("usernames").doc("spiderx").set({
      userId: BOB_UID,
      username: "spiderx",
      createdAt: ts,
    })
  );

  // Unique phone hash claim
  await assertSucceeds(
    aliceDb.collection("phoneClaims").doc("sha256_phone_alice").set({
      userId: ALICE_UID,
      createdAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("phoneClaims").doc("sha256_phone_alice").set({
      userId: BOB_UID,
      createdAt: ts,
    })
  );
  // List queries on claim collections are blocked
  await assertFails(bobDb.collection("phoneClaims").get());
  await assertFails(bobDb.collection("emailClaims").get());
});

test("Referral system blocks self-referral and client bonus minting, allows admin bonus transaction", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Alice creates her referral code claim
  await assertSucceeds(
    aliceDb.collection("referralCodes").doc("SPDRALICE1").set({
      userId: ALICE_UID,
      username: "SpiderAlice",
      referralCode: "SPDRALICE1",
      createdAt: ts,
    })
  );

  // Self-referral denied
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referrerUid: ALICE_UID,
      referrerUsername: "SpiderAlice",
      referredUid: ALICE_UID,
      referredUsername: "SpiderAlice",
      referralCode: "SPDRALICE1",
      status: "PENDING",
      bonusAmount: 0,
      transactionId: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Client cannot mint CREDITED status or bonus > 0
  await assertFails(
    bobDb.collection("referrals").doc(BOB_UID).set({
      referrerUid: ALICE_UID,
      referrerUsername: "SpiderAlice",
      referredUid: BOB_UID,
      referredUsername: "SpiderBob",
      referralCode: "SPDRALICE1",
      status: "CREDITED",
      bonusAmount: 100,
      transactionId: "fake_tx",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid PENDING referral creation by Bob succeeds
  await assertSucceeds(
    bobDb.collection("referrals").doc(BOB_UID).set({
      referrerUid: ALICE_UID,
      referrerUsername: "SpiderAlice",
      referredUid: BOB_UID,
      referredUsername: "SpiderBob",
      referralCode: "SPDRALICE1",
      status: "PENDING",
      bonusAmount: 0,
      transactionId: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scoped referral queries succeed
  await assertSucceeds(aliceDb.collection("referrals").where("referrerUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("referrals").get());

  // Admin approves referral & records REFERRAL_BONUS transaction
  await assertSucceeds(
    adminDb.collection("referrals").doc(BOB_UID).update({
      status: "CREDITED",
      bonusAmount: 10,
      transactionId: "tx_ref_bob",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("transactions").doc("tx_ref_bob").set({
      userId: ALICE_UID,
      type: "REFERRAL_BONUS",
      direction: "CREDIT",
      description: "Referral bonus for inviting SpiderBob",
      matchId: "",
      amount: 10,
      status: "COMPLETED",
      resultingBalance: 10,
      createdAt: pastDate(1000),
    })
  );
});

test("Only verified satyamchakraborty14@gmail.com is admin; other users in adminUsers cannot gain admin access", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("adminUsers").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      role: "ADMIN",
      grantedBy: ADMIN_UID,
      createdAt: pastDate(),
    });
  });

  const verifiedBobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(verifiedBobDb.collection("users").get());
  await assertFails(
    verifiedBobDb.collection("adminUsers").doc("fake_admin").set({
      userId: "fake_admin",
      email: "bob@example.com",
      role: "ADMIN",
      grantedBy: BOB_UID,
      createdAt: pastDate(),
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("adminUsers").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      role: "OWNER_ADMIN",
      grantedBy: ADMIN_EMAIL,
      createdAt: pastDate(),
    })
  );
  await assertFails(
    adminDb.collection("adminUsers").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      role: "ADMIN",
      grantedBy: ADMIN_EMAIL,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(adminDb.collection("users").get());
});

test("Admin can configure Payment QR; users can submit custom deposits; withdrawals require winning balance >= 50 and non-banned status", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Admin writes QR wallet config; user cannot overwrite it
  await assertSucceeds(
    adminDb.collection("appConfig").doc("wallet").set({
      qrImageUrl: "https://example.com/qr.png",
      upiId: "spider@upi",
      instructions: "50 Coins = 50 Rupees",
      minWithdraw: 50,
      coinsPerRupee: 1,
      winningOnlyRedeem: true,
      updatedBy: ADMIN_EMAIL,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("appConfig").doc("wallet").get());
  await assertFails(
    aliceDb.collection("appConfig").doc("wallet").set({
      qrImageUrl: "https://evil.com/qr.png",
      upiId: "hacker@upi",
      instructions: "hack",
      minWithdraw: 50,
      coinsPerRupee: 1,
      winningOnlyRedeem: true,
      updatedBy: "alice",
      updatedAt: ts,
    })
  );

  // Alice creates a custom PENDING deposit request (75 coins)
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_alice_75").set({
      userId: ALICE_UID,
      packageName: "Custom 75 Coins (₹75)",
      coinAmount: 75,
      rupeeAmount: 75,
      gateway: "UPI_QR",
      referenceId: "UTR123456789",
      reviewedBy: "",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Alice cannot approve her own deposit
  await assertFails(
    aliceDb.collection("payments").doc("pay_alice_75").update({
      status: "APPROVED",
      reviewedBy: ALICE_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Alice wallet: 200 Deposit + 100 Bonus + 40 Winning (total 340, winning < 50!)
  await assertSucceeds(
    adminDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      totalBalance: 340,
      depositedBalance: 200,
      winningBalance: 40,
      bonusBalance: 100,
      earningsBalance: 140,
      payoutsTotal: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Withdrawal < 50 fails
  await assertFails(
    aliceDb.collection("payoutRequests").doc("out_40").set({
      userId: ALICE_UID,
      amount: 40,
      destinationType: "UPI",
      destinationDetails: "alice@upi",
      status: "PENDING",
      reviewedBy: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Withdrawal >= 50 (e.g. 60) fails when winningBalance is only 40 (Deposit/Bonus cannot be redeemed)
  await assertFails(
    aliceDb.collection("payoutRequests").doc("out_60").set({
      userId: ALICE_UID,
      amount: 60,
      destinationType: "UPI",
      destinationDetails: "alice@upi",
      status: "PENDING",
      reviewedBy: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin credits Winning balance to 80; now withdrawal of 50 succeeds
  await assertSucceeds(
    adminDb.collection("wallets").doc(ALICE_UID).update({
      winningBalance: 80,
      totalBalance: 380,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("payoutRequests").doc("out_50").set({
      userId: ALICE_UID,
      amount: 50,
      destinationType: "UPI",
      destinationDetails: "alice@upi",
      status: "PENDING",
      reviewedBy: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Banned user Bob cannot submit deposit or withdrawal requests
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "banned_bob",
      displayName: "Bob",
      email: "bob@example.com",
      photoUrl: "",
      playerUid: "SE200202",
      emailVerified: true,
      referralCode: "SPDRBOB",
      status: "BANNED",
      matchesPlayed: 0,
      wins: 0,
      verifiedKills: 0,
      rankingPoints: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("wallets").doc(BOB_UID).set({
      userId: BOB_UID,
      totalBalance: 100,
      depositedBalance: 0,
      winningBalance: 100,
      bonusBalance: 0,
      earningsBalance: 100,
      payoutsTotal: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  await assertFails(
    bobDb.collection("payments").doc("pay_bob_50").set({
      userId: BOB_UID,
      packageName: "Custom 50 Coins",
      coinAmount: 50,
      rupeeAmount: 50,
      gateway: "UPI_QR",
      referenceId: "UTR999",
      reviewedBy: "",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("payoutRequests").doc("out_bob_50").set({
      userId: BOB_UID,
      amount: 50,
      destinationType: "UPI",
      destinationDetails: "bob@upi",
      status: "PENDING",
      reviewedBy: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Missing-document get() parity on referrals, registrations, and credentials preserves cross-user isolation", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  // Seed free public upcoming tournament
  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_free_missing_check").set({
      categoryId: "cat_solo",
      title: "Solo Free Open",
      shortDescription: "Free entry open",
      gameMode: "SOLO",
      mapName: "Bermuda",
      entryFee: 0,
      prizePool: 50,
      perKillReward: 0,
      maxParticipants: 48,
      registeredCount: 0,
      matchDate: "2026-11-22",
      matchTime: "20:00",
      registrationDeadline: "2026-11-22 19:45",
      rules: "Fair play only.",
      bannerUrl: "",
      presetKey: "battle_royale",
      visibility: "PUBLIC",
      status: "UPCOMING",
      soloFormat: true,
      randomTournament: false,
      roomReleased: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 1. Missing referrals/{uid}: owner can get() non-existent doc cleanly; other user cannot
  await assertSucceeds(aliceDb.collection("referrals").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("referrals").doc(ALICE_UID).get());

  // 2. Missing registrations/{tourId}_{uid}: owner can pre-read before registration; other user cannot
  const aliceRegId = `tour_free_missing_check_${ALICE_UID}`;
  await assertSucceeds(aliceDb.collection("registrations").doc(aliceRegId).get());
  await assertFails(bobDb.collection("registrations").doc(aliceRegId).get());

  // 3. Unregistered Alice cannot get missing room credentials; once registered via batch (mirroring tx), Alice can get missing credentials
  await assertFails(
    aliceDb.collection("tournaments").doc("tour_free_missing_check").collection("credentials").doc("room").get()
  );

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("registrations").doc(aliceRegId), {
    tournamentId: "tour_free_missing_check",
    tournamentTitle: "Solo Free Open",
    categoryId: "cat_solo",
    userId: ALICE_UID,
    username: "spider_alice",
    inGameName: "AliceOP",
    inGameUid: "99887766",
    entryFeePaid: 0,
    status: "CONFIRMED",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.update(aliceDb.collection("tournaments").doc("tour_free_missing_check"), {
    registeredCount: 1,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tour_free_missing_check").collection("credentials").doc("room").get()
  );
  await assertFails(
    bobDb.collection("tournaments").doc("tour_free_missing_check").collection("credentials").doc("room").get()
  );
});


