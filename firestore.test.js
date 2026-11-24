const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const crypto = require("node:crypto");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-spideres-670c0de6-e0db-4f2d-8edd-fd4ebb9ee279";
const ALICE_UID = "alice_uid_123456";
const BOB_UID = "bob_uid_654321";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256(val) {
  return crypto.createHash("sha256").update(val, "utf8").digest("hex");
}

function dbForUser(uid, email = `${uid}@gmail.com`) {
  return testEnv
    .authenticatedContext(uid, { email, email_verified: true })
    .firestore(DATABASE_ID);
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

async function seedUserAndTournament({
  entryFee = 25,
  maxParticipants = 48,
  registeredCount = 0,
  walletBalance = 100,
  aliceStatus = "ACTIVE",
  seedWallet = true,
} = {}) {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AlicePro",
      displayName: "Alice Pro",
      email: "alice@gmail.com",
      playerUid: "SEALICE1",
      inGameName: "AliceFF",
      inGameUid: "1122334455",
      phoneNumber: "+919876543210",
      phoneVerified: false,
      emailVerified: true,
      acceptedTerms: true,
      onboardingCompleted: true,
      referralCode: "SPDRALICE1",
      referredBy: "",
      language: "EN",
      status: aliceStatus,
      matchesPlayed: 0,
      wins: 0,
      verifiedKills: 0,
      rankingPoints: 0,
      createdAt: now,
      updatedAt: now,
    });

    await adminDb.collection("tournaments").doc("tour_coin_1").set({
      categoryId: "cat_full_map",
      title: "Full Map Coin Clash",
      shortDescription: "Coin entry tournament",
      gameMode: "FULL MAP",
      mapName: "Bermuda",
      entryFee,
      prizePool: 500,
      perKillReward: 10,
      maxParticipants,
      registeredCount,
      matchDate: "Today",
      matchTime: "20:00",
      registrationDeadline: "19:50",
      rules: "Fair play",
      bannerUrl: "",
      presetKey: "battle_royale",
      visibility: "PUBLIC",
      status: "UPCOMING",
      soloFormat: false,
      randomTournament: false,
      roomReleased: false,
      createdAt: now,
      updatedAt: now,
    });

    if (seedWallet) {
      await adminDb.collection("wallets").doc(ALICE_UID).set({
        userId: ALICE_UID,
        totalBalance: walletBalance,
        depositedBalance: walletBalance,
        winningBalance: 0,
        bonusBalance: 0,
        earningsBalance: 0,
        payoutsTotal: 0,
        createdAt: now,
        updatedAt: now,
      });
    }
  });
}

test("Free tournament: authenticated active user can join without wallet", async () => {
  await seedUserAndTournament({ entryFee: 0, seedWallet: false });
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      await tx.get(regRef);
      const tSnap = await tx.get(tourRef);
      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 0,
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      });
      tx.update(tourRef, {
        registeredCount: tSnap.data().registeredCount + 1,
        updatedAt: now,
      });
    })
  );
});

test("Coin tournament: user with sufficient coins joins atomically and deducts wallet", async () => {
  await seedUserAndTournament({ entryFee: 25, walletBalance: 100 });
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      const walletRef = aliceDb.collection("wallets").doc(ALICE_UID);
      const txRef = aliceDb.collection("transactions").doc(`tx_join_tour_coin_1_${ALICE_UID}`);

      await tx.get(regRef);
      const tSnap = await tx.get(tourRef);
      const wSnap = await tx.get(walletRef);

      const nextDep = wSnap.data().depositedBalance - 25;
      const nextTotal = nextDep;

      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 25,
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      });
      tx.update(tourRef, {
        registeredCount: tSnap.data().registeredCount + 1,
        updatedAt: now,
      });
      tx.update(walletRef, {
        totalBalance: nextTotal,
        depositedBalance: nextDep,
        winningBalance: 0,
        bonusBalance: 0,
        earningsBalance: 0,
        updatedAt: now,
      });
      tx.set(txRef, {
        userId: ALICE_UID,
        type: "TOURNAMENT_ENTRY",
        direction: "DEBIT",
        description: "Joined Full Map Coin Clash",
        matchId: "tour_coin_1",
        amount: 25,
        status: "COMPLETED",
        resultingBalance: nextTotal,
        createdAt: now,
      });
    })
  );
});

test("Coin tournament: fails when wallet debit is missing or forged, or when slots are full", async () => {
  await seedUserAndTournament({ entryFee: 25, walletBalance: 100 });
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 1000);

  // Cannot join coin tournament without debiting wallet
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 25,
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      });
      tx.update(tourRef, {
        registeredCount: 1,
        updatedAt: now,
      });
    })
  );

  // Cannot increase own wallet balance
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).update({
      totalBalance: 500,
      depositedBalance: 500,
      winningBalance: 0,
      bonusBalance: 0,
      earningsBalance: 0,
      updatedAt: now,
    })
  );

  // Cannot create fake credit transaction
  await assertFails(
    aliceDb.collection("transactions").doc("fake_credit_tx").set({
      userId: ALICE_UID,
      type: "DEPOSIT",
      direction: "CREDIT",
      description: "Free coins",
      matchId: "",
      amount: 500,
      status: "COMPLETED",
      resultingBalance: 600,
      createdAt: now,
    })
  );
});

test("Coin tournament: fails when slots are full", async () => {
  await seedUserAndTournament({ entryFee: 25, maxParticipants: 10, registeredCount: 10, walletBalance: 100 });
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 1000);

  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      const walletRef = aliceDb.collection("wallets").doc(ALICE_UID);
      const txRef = aliceDb.collection("transactions").doc(`tx_join_tour_coin_1_${ALICE_UID}`);
      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 25,
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      });
      tx.update(tourRef, { registeredCount: 11, updatedAt: now });
      tx.update(walletRef, {
        totalBalance: 75,
        depositedBalance: 75,
        winningBalance: 0,
        bonusBalance: 0,
        earningsBalance: 0,
        updatedAt: now,
      });
      tx.set(txRef, {
        userId: ALICE_UID,
        type: "TOURNAMENT_ENTRY",
        direction: "DEBIT",
        description: "Joined Full Map Coin Clash",
        matchId: "tour_coin_1",
        amount: 25,
        status: "COMPLETED",
        resultingBalance: 75,
        createdAt: now,
      });
    })
  );
});

test("One mobile number per account: phoneClaims blocks second user without needing OTP", async () => {
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const bobDb = dbForUser(BOB_UID, "bob@gmail.com");
  const phone = "+919988776655";
  const hash = sha256(phone);
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.collection("phoneClaims").doc(hash), {
        userId: ALICE_UID,
        createdAt: now,
      });
      tx.set(aliceDb.collection("users").doc(ALICE_UID), {
        userId: ALICE_UID,
        username: "AliceOne",
        displayName: "Alice One",
        email: "alice@gmail.com",
        photoUrl: "",
        playerUid: "SEALICE1",
        inGameName: "AliceOne",
        inGameUid: "99887766",
        phoneNumber: phone,
        phoneVerified: false,
        emailVerified: true,
        acceptedTerms: true,
        onboardingCompleted: true,
        referralCode: "SPDRALICE1",
        referredBy: "",
        language: "EN",
        status: "ACTIVE",
        matchesPlayed: 0,
        wins: 0,
        verifiedKills: 0,
        rankingPoints: 0,
        createdAt: now,
        updatedAt: now,
      });
    })
  );

  // Bob cannot claim the same mobile number
  await assertFails(
    bobDb.collection("phoneClaims").doc(hash).set({
      userId: BOB_UID,
      createdAt: now,
    })
  );

  // Bob cannot delete Alice's phone claim
  await assertFails(bobDb.collection("phoneClaims").doc(hash).delete());

  // Alice cannot spoof phoneVerified=true
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice can update her own mobile number to a fresh unused number without OTP
  const newPhone = "+919111222333";
  const newHash = sha256(newPhone);
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.delete(aliceDb.collection("phoneClaims").doc(hash));
      tx.set(aliceDb.collection("phoneClaims").doc(newHash), {
        userId: ALICE_UID,
        createdAt: new Date(Date.now() - 500),
      });
      tx.update(aliceDb.collection("users").doc(ALICE_UID), {
        phoneNumber: newPhone,
        updatedAt: new Date(Date.now() - 500),
      });
    })
  );
});

test("Coin tournament: multi-bucket deduction (deposit + bonus + winning) succeeds", async () => {
  await seedUserAndTournament({ entryFee: 50, walletBalance: 0, seedWallet: false });
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      totalBalance: 60,
      depositedBalance: 20,
      bonusBalance: 15,
      earningsBalance: 15,
      winningBalance: 25,
      payoutsTotal: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const txNow = new Date(Date.now() - 1000);

  // Deduct 50 Coins: 20 Deposit + 15 Bonus + 15 Winning -> Remaining: 0 Dep, 0 Bonus, 10 Win (Total 10)
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      const walletRef = aliceDb.collection("wallets").doc(ALICE_UID);
      const txRef = aliceDb.collection("transactions").doc(`tx_join_tour_coin_1_${ALICE_UID}`);

      await tx.get(regRef);
      const tSnap = await tx.get(tourRef);
      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 50,
        status: "CONFIRMED",
        createdAt: txNow,
        updatedAt: txNow,
      });
      tx.update(tourRef, {
        registeredCount: tSnap.data().registeredCount + 1,
        updatedAt: txNow,
      });
      tx.update(walletRef, {
        totalBalance: 10,
        depositedBalance: 0,
        winningBalance: 10,
        bonusBalance: 0,
        earningsBalance: 0,
        updatedAt: txNow,
      });
      tx.set(txRef, {
        userId: ALICE_UID,
        type: "TOURNAMENT_ENTRY",
        direction: "DEBIT",
        description: "Joined Full Map Coin Clash (50 Coins)",
        matchId: "tour_coin_1",
        amount: 50,
        status: "COMPLETED",
        resultingBalance: 10,
        createdAt: txNow,
      });
    })
  );
});

test("Results 1-48: admin can write linked and manual position results; users can read verified list but cannot write", async () => {
  await seedUserAndTournament({ entryFee: 0, seedWallet: false });
  const adminDb = dbForUser("admin_uid_1", "satyamchakraborty14@gmail.com");
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 1000);

  // Position #1 linked to Alice
  await assertSucceeds(
    adminDb.collection("results").doc("tour_coin_1_pos_1").set({
      tournamentId: "tour_coin_1",
      tournamentTitle: "Full Map Coin Clash",
      userId: ALICE_UID,
      username: "AliceFF",
      placement: 1,
      kills: 11,
      rewardCoins: 250,
      verified: true,
      declaredBy: "satyamchakraborty14@gmail.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Position #48 manual player name (unlinked userId = "")
  await assertSucceeds(
    adminDb.collection("results").doc("tour_coin_1_pos_48").set({
      tournamentId: "tour_coin_1",
      tournamentTitle: "Full Map Coin Clash",
      userId: "",
      username: "SquadSlot48",
      placement: 48,
      kills: 0,
      rewardCoins: 0,
      verified: true,
      declaredBy: "satyamchakraborty14@gmail.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular user can list verified results to view all 48 positions
  await assertSucceeds(
    aliceDb.collection("results").where("verified", "==", true).get()
  );

  // Regular user cannot create or modify results
  await assertFails(
    aliceDb.collection("results").doc("tour_coin_1_pos_2").set({
      tournamentId: "tour_coin_1",
      tournamentTitle: "Full Map Coin Clash",
      userId: ALICE_UID,
      username: "AliceFF",
      placement: 2,
      kills: 5,
      rewardCoins: 999,
      verified: true,
      declaredBy: "alice@gmail.com",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(adminDb.collection("results").doc("tour_coin_1_pos_48").delete().then(() => aliceDb.collection("results").doc("tour_coin_1_pos_1").delete()));
});

test("Match Details & Joined Members: atomic join writes participant entry and signed-in players can read list; spoofed participants rejected", async () => {
  await seedUserAndTournament({ entryFee: 25, walletBalance: 100 });
  const adminDb = dbForUser("admin_uid_1", "satyamchakraborty14@gmail.com");
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const bobDb = dbForUser(BOB_UID, "bob@gmail.com");
  const now = new Date(Date.now() - 1000);

  // Admin can set rich matchDetails on tournament
  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_coin_1").update({
      matchDetails: "Booyah pays 250 Coins! Room ID releases 10 mins prior. Squads must stay in assigned slots.",
      updatedAt: now,
    })
  );

  // Any signed-in player can list participants even when empty
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_coin_1").collection("participants").get()
  );

  // Spoofed participant write without registration fails
  await assertFails(
    bobDb.collection("tournaments").doc("tour_coin_1").collection("participants").doc(BOB_UID).set({
      tournamentId: "tour_coin_1",
      userId: BOB_UID,
      playerName: "FakeBob",
      username: "BobFake",
      createdAt: now,
    })
  );

  // Alice atomically joins with wallet debit, tx, registration, slot increment, AND participant roster doc
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const regRef = aliceDb.collection("registrations").doc(`tour_coin_1_${ALICE_UID}`);
      const tourRef = aliceDb.collection("tournaments").doc("tour_coin_1");
      const participantRef = tourRef.collection("participants").doc(ALICE_UID);
      const walletRef = aliceDb.collection("wallets").doc(ALICE_UID);
      const txRef = aliceDb.collection("transactions").doc(`tx_join_tour_coin_1_${ALICE_UID}`);

      await tx.get(regRef);
      const tSnap = await tx.get(tourRef);
      const wSnap = await tx.get(walletRef);

      tx.update(walletRef, {
        totalBalance: wSnap.data().totalBalance - 25,
        depositedBalance: wSnap.data().depositedBalance - 25,
        updatedAt: now,
      });
      tx.set(txRef, {
        userId: ALICE_UID,
        type: "TOURNAMENT_ENTRY",
        direction: "DEBIT",
        description: "Joined Full Map Coin Clash (25 Coins)",
        matchId: "tour_coin_1",
        amount: 25,
        status: "COMPLETED",
        resultingBalance: 75,
        createdAt: now,
      });
      tx.set(regRef, {
        tournamentId: "tour_coin_1",
        tournamentTitle: "Full Map Coin Clash",
        categoryId: "cat_full_map",
        userId: ALICE_UID,
        username: "AlicePro",
        inGameName: "AliceFF",
        inGameUid: "1122334455",
        entryFeePaid: 25,
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      });
      tx.set(participantRef, {
        tournamentId: "tour_coin_1",
        userId: ALICE_UID,
        playerName: "AliceFF",
        username: "AlicePro",
        createdAt: now,
      });
      tx.update(tourRef, {
        registeredCount: tSnap.data().registeredCount + 1,
        updatedAt: now,
      });
    })
  );

  // Signed-in Bob can read joined members list
  const snap = await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_coin_1").collection("participants").get()
  );
  if (snap.size !== 1 || snap.docs[0].data().playerName !== "AliceFF") {
    throw new Error("Expected AliceFF in tournament participants subcollection");
  }
});

test("Map Banners: admin can save/update/delete map banners; regular users can read/list only", async () => {
  const adminDb = dbForUser("admin_uid_1", "satyamchakraborty14@gmail.com");
  const aliceDb = dbForUser(ALICE_UID, "alice@gmail.com");
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("mapBanners").doc("map_bermuda").set({
      mapName: "Bermuda",
      bannerUrl: "https://example.com/bermuda.png",
      presetKey: "battle_royale",
      displayOrder: 1,
      enabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin update preserving createdAt succeeds
  await assertSucceeds(
    adminDb.collection("mapBanners").doc("map_bermuda").update({
      bannerUrl: "https://example.com/bermuda_v2.png",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Signed-in user can read/list mapBanners
  await assertSucceeds(aliceDb.collection("mapBanners").get());

  // Signed-in regular user cannot create or delete mapBanners
  await assertFails(
    aliceDb.collection("mapBanners").doc("map_kalahari").set({
      mapName: "Kalahari",
      bannerUrl: "https://example.com/kalahari.png",
      presetKey: "battle_royale",
      displayOrder: 2,
      enabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("mapBanners").doc("map_bermuda").delete());
});

