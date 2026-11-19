const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_farmer_01";
const BOB_UID = "bob_farmer_02";
const ADMIN_UID = "admin_user_01";
const ADMIN_EMAIL = "avishkargorad60@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, now) {
  return {
    userId: uid,
    fullName: "Ramesh Patil",
    mobile: "9876543210",
    email: "ramesh@farm.in",
    state: "Maharashtra",
    district: "Pune",
    village: "Baramati",
    preferredLanguage: "mr",
    farmSizeAcres: 4.5,
    mainCrops: "Soybean, Sugarcane",
    soilType: "Black Soil (Regur)",
    role: "FARMER",
    active: true,
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("soil_analyses").get());
  await assertFails(unauthDb.collection("platform_activity").get());
});

test("Farmer can create, read, and partially update own profile, but cannot escalate role or spoof id", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@farm.in", email_verified: true }).firestore();
  const past = new Date(Date.now() - 5000);

  // Valid create
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      district: "Nashik",
      farmSizeAcres: 6.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject role self-escalation on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject creating profile with ADMIN role directly
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      ...validProfile(BOB_UID, past),
      role: "ADMIN",
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, or write Alice's profile or subcollections", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past));
    await db.collection("users").doc(ALICE_UID).collection("soil_analyses").doc("soil_1").set({
      userId: ALICE_UID,
      soilTypeInput: "Black Soil (Regur)",
      moistureNote: "Moist",
      predictedCategory: "Black Soil (Regur)",
      confidenceNote: "Gemini analysis",
      cropRecommendations: "Soybean, Cotton",
      improvementTips: "Add compost",
      liveAi: true,
      createdAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@farm.in", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("soil_analyses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("soil_analyses").doc("soil_1").delete());
});

test("Authenticated farmer can create, list, and delete all user-scoped subcollections matching Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@farm.in", email_verified: true }).firestore();
  const past = new Date(Date.now() - 3000);

  // Soil analysis (optional imageUri & phValue omitted when null)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("soil_analyses").doc("soil_1").set({
      userId: ALICE_UID,
      soilTypeInput: "Black Soil (Regur)",
      moistureNote: "Medium",
      predictedCategory: "Black Soil (Regur)",
      confidenceNote: "Live Gemini classification",
      cropRecommendations: "Soybean, Cotton",
      improvementTips: "Use FYM",
      liveAi: true,
      createdAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("soil_analyses").get());

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("soil_analyses").doc("soil_future").set({
      userId: ALICE_UID,
      soilTypeInput: "Red Soil",
      moistureNote: "Dry",
      predictedCategory: "Red Soil",
      confidenceNote: "Test",
      cropRecommendations: "Groundnut",
      improvementTips: "Mulch",
      liveAi: true,
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Disease detection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("disease_detections").doc("dis_1").set({
      userId: ALICE_UID,
      cropName: "Soybean",
      symptomsNote: "Yellow spots",
      predictedDisease: "Soybean Rust",
      confidenceNote: "High visual confidence",
      symptomsCauses: "Fungal spores",
      preventionSteps: "Improve airflow",
      treatmentGuidance: "Consult KVK",
      liveAi: true,
      createdAt: past,
    })
  );

  // Crop recommendation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("crop_recommendations").doc("rec_1").set({
      userId: ALICE_UID,
      soilType: "Black Soil (Regur)",
      temperatureC: 28.5,
      rainfallMm: 650.0,
      season: "Kharif (Monsoon)",
      state: "Maharashtra",
      district: "Pune",
      irrigationType: "Borewell / Open Well",
      farmSizeAcres: 4.5,
      topCropName: "Soybean",
      topScore: 95,
      recommendedCropsSummary: "Soybean (95%)",
      createdAt: past,
    })
  );

  // Chat message
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      message: "When should I irrigate soybean?",
      followUps: "",
      liveAi: false,
      createdAt: past,
    })
  );

  // Weather advisory
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weather_advisories").doc("wx_1").set({
      userId: ALICE_UID,
      locationName: "Pune, Maharashtra",
      temperatureC: 29.0,
      humidityPct: 62,
      windKmh: 11.4,
      rainfallMm: 0.0,
      advisorySummary: "Plan morning irrigation.",
      createdAt: past,
    })
  );

  // Saved crop
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_crops").doc("crop_1").set({
      userId: ALICE_UID,
      cropName: "Soybean",
      season: "Kharif (Monsoon)",
      notes: "Score 95%",
      createdAt: past,
    })
  );

  // Activity log + platform_activity create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity_logs").doc("act_1").set({
      userId: ALICE_UID,
      module: "Crop Advisor",
      actionTitle: "Generated Kharif Plan",
      details: "Top match: Soybean",
      createdAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("platform_activity").doc("plat_1").set({
      userId: ALICE_UID,
      module: "Crop Advisor",
      actionTitle: "Generated Kharif Plan",
      details: "Top match: Soybean",
      createdAt: past,
    })
  );
});

test("Admin email spoofing rejected when email_verified is false; verified admin can manage users & read platform_activity", async () => {
  const past = new Date(Date.now() - 4000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, past));
  });

  // Unverified spoofed email must FAIL
  const spoofedAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: false }).firestore();
  await assertFails(spoofedAdminDb.collection("users").get());
  await assertFails(spoofedAdminDb.collection("platform_activity").get());

  // Verified admin succeeds
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(realAdminDb.collection("users").get());
  await assertSucceeds(realAdminDb.collection("platform_activity").get());
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
