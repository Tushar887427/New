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
    displayName: "Admin",
    bio: "Government Office Staff",
    photoDataUrl: "",
    weeklyOff: 0,
    shiftStart: "09:30",
    darkMode: false,
    infoFields: [
      { label: "ID", value: "GOV-1042" },
      { label: "Office", value: "Main Block" },
    ],
    sessionDate: "2026-11-24",
    punchedIn: false,
    sessionInTime: "",
    sessionOutTime: "",
    onBreak: false,
    breakStartMs: 0,
    totalBreakMins: 0,
    githubConfigUrl: "",
    githubConfigVersion: "1.0.0",
    sampleDataSeeded: true,
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

test("Unauthenticated users cannot read or write any Work'ed collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
});

test("UserProfile: owner create, partial update, darkMode toggle, and security rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      darkMode: true,
      displayName: "Tushar",
      githubConfigUrl: "https://raw.githubusercontent.com/user/repo/main/worked-config.json",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field injection (Shadow Update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      darkMode: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject cross-user read/update and blanket user listing
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Attendance: Kotlin create, update, list, and invalid payload rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-24").set({
      dateKey: "2026-11-24",
      status: "Present",
      leaveType: "",
      inTime: "09:20",
      outTime: "17:05",
      note: "On time",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-24").update({
      status: "Leave",
      leaveType: "Casual Leave",
      inTime: "",
      outTime: "",
      note: "Family errand",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List subcollection succeeds for owner, fails for Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  // Reject invalid status or mismatched dateKey or extra `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-25").set({
      id: "2026-11-25",
      dateKey: "2026-11-25",
      status: "Present",
      leaveType: "",
      inTime: "09:30",
      outTime: "17:00",
      note: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-26").set({
      dateKey: "2026-11-26",
      status: "InvalidStatus",
      leaveType: "",
      inTime: "",
      outTime: "",
      note: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Expenses, Finances, OutOnWork, Reminders, Tasks, Colleagues: full Kotlin CRUD parity & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const later = new Date(Date.now() - 1000);

  // Out On Work
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("out_on_work").doc("oow_1").set({
      date: "2026-11-20",
      mode: "Full Day",
      startTime: "10:00",
      endTime: "16:30",
      meetingWith: "DEO Office",
      location: "Collectorate",
      remarks: "Submitted report",
      attachmentName: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("out_on_work").get()
  );

  // Expenses
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      date: "2026-11-20",
      title: "Office Stationery",
      category: "Office",
      amount: 1450,
      owner: "headmaster",
      recovered: false,
      note: "Voucher #112",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").update({
      recovered: true,
      updatedAt: later,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").delete()
  );

  // Finances
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("finances").doc("fin_1").set({
      date: "2026-11-01",
      title: "Monthly Salary",
      type: "income",
      amount: 45000,
      status: "received",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("finances").doc("fin_1").update({
      status: "pending",
      updatedAt: later,
    })
  );

  // Reminders (Key Dates)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set({
      title: "Fire Safety NOC",
      category: "Compliance",
      validFrom: "2025-12-05",
      validUntil: "2026-12-05",
      note: "Inspect soon",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Tasks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_1").set({
      text: "Verify attendance register",
      done: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_1").update({
      done: true,
      updatedAt: later,
    })
  );

  // Colleagues
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("colleagues").doc("col_1").set({
      name: "Rajesh Sharma",
      role: "Headmaster",
      theme: "sky",
      photoDataUrl: "",
      bgPhotoDataUrl: "",
      infoFields: [{ label: "Mobile", value: "+91 98765 43210" }],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("colleagues").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("colleagues").get()
  );
});

test("Disabled /app_config endpoint is completely denied by default-deny rules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext("owner_uid", {
    email: "tusharwasnik887427@gmail.com",
    email_verified: true,
  }).firestore();

  const ts = new Date(Date.now() - 5000);
  const payload = { configVersion: "1.0.0", createdAt: ts, updatedAt: ts };

  await assertFails(unauthDb.collection("app_config").doc("live").get());
  await assertFails(aliceDb.collection("app_config").doc("live").get());
  await assertFails(ownerDb.collection("app_config").doc("live").get());
  await assertFails(unauthDb.collection("app_config").doc("live").set(payload));
  await assertFails(aliceDb.collection("app_config").doc("live").set(payload));
  await assertFails(ownerDb.collection("app_config").doc("live").set(payload));
});

test("Atomic batch Punch In and Punch Out commit profile session and attendance together", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const punchInTs = new Date(Date.now() - 2000);
  const punchOutTs = new Date(Date.now() - 500);

  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const attRef = profileRef.collection("attendance").doc("2026-11-24");

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, ts)));

  // Atomic Punch In batch (profile update + attendance create)
  const punchInBatch = aliceDb.batch();
  punchInBatch.update(profileRef, {
    sessionDate: "2026-11-24",
    punchedIn: true,
    sessionInTime: "09:20",
    sessionOutTime: "",
    onBreak: false,
    breakStartMs: 0,
    totalBreakMins: 0,
    updatedAt: punchInTs,
  });
  punchInBatch.set(attRef, {
    dateKey: "2026-11-24",
    status: "Present",
    leaveType: "",
    inTime: "09:20",
    outTime: "",
    note: "",
    createdAt: punchInTs,
    updatedAt: punchInTs,
  });
  await assertSucceeds(punchInBatch.commit());

  // Atomic Punch Out batch (profile update + attendance update)
  const punchOutBatch = aliceDb.batch();
  punchOutBatch.update(profileRef, {
    sessionDate: "2026-11-24",
    punchedIn: false,
    sessionInTime: "09:20",
    sessionOutTime: "17:15",
    onBreak: false,
    breakStartMs: 0,
    totalBreakMins: 15,
    updatedAt: punchOutTs,
  });
  punchOutBatch.update(attRef, {
    status: "Present",
    leaveType: "",
    inTime: "09:20",
    outTime: "17:15",
    note: "",
    updatedAt: punchOutTs,
  });
  await assertSucceeds(punchOutBatch.commit());
});


