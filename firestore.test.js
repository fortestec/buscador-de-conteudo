const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_123";
const BOB_UID = "bob_creator_456";

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

function validProfilePayload(uid, created) {
  return {
    userId: uid,
    displayName: "Ana Criadora",
    niche: "Tecnologia e Produtividade",
    targetAudience: "Profissionais e estudantes 20-35 anos",
    toneStyle: "Direto e Energético",
    preferredPlatforms: ["Instagram Reels", "TikTok"],
    weeklyGoal: 5,
    createdAt: created,
    updatedAt: created,
  };
}

function validSummaryPayload(dateKey, created) {
  return {
    dateKey,
    nicheSnapshot: "Tecnologia e Produtividade",
    dailyInsight: "Vídeos curtos com gancho visual nos primeiros 2 segundos e tela dividida estão dominando hoje.",
    bestPostingWindow: "18h30 - 20h30",
    topHashtags: ["#produtividade", "#reelsbrasil", "#dicasdetech"],
    createdAt: created,
    updatedAt: created,
  };
}

function validTrendPayload(dateKey, created) {
  return {
    dateKey,
    rank: 1,
    title: "3 Apps Secretos que Economizam 2h por Dia",
    platform: "Instagram Reels",
    formatType: "Lista Rápida + Tela Dividida",
    estimatedViewsLabel: "1.4M - 2.8M views em alta",
    engagementScore: 94,
    whyItWorks: "Promessa prática imediata com curiosidade aberta nos primeiros 3 segundos.",
    adaptationAngle: "Mostre 3 ferramentas reais do seu dia a dia com gravação de tela rápida.",
    viralAudioStyle: "Batida Lo-Fi acelerada (120 BPM)",
    durationSeconds: 28,
    hookText: "Se você ainda faz isso manualmente, pare agora e veja esses 3 apps.",
    bodyBeats: [
      "[00s-03s] Gancho direto olhando para a câmera com texto grande na tela.",
      "[03s-12s] App 1: mostre o antes e depois em 1 clique.",
      "[12s-22s] App 2 e 3 com cortes secos de 2 segundos.",
    ],
    ctaText: "Comenta 'LISTA' que eu te mando os links ou salva para testar hoje!",
    visualTips: "Grave em pé com boa luz natural, legenda dinâmica centralizada e zoom leve a cada frase.",
    captionAndHashtags: "Esses 3 atalhos mudaram minha rotina!\n\n#produtividade #dicas #reels",
    savedToScripts: false,
    createdAt: created,
    updatedAt: created,
  };
}

function validScriptPayload(created) {
  return {
    title: "3 Apps Secretos que Economizam 2h",
    platform: "Instagram Reels",
    trendInspirationTitle: "3 Apps Secretos que Economizam 2h por Dia",
    engagementScore: 94,
    durationSeconds: 28,
    hookText: "Se você ainda faz isso manualmente, pare agora e veja esses 3 apps.",
    bodyBeats: [
      "[00s-03s] Gancho direto olhando para a câmera.",
      "[03s-20s] Demonstração rápida dos 3 apps na tela.",
    ],
    ctaText: "Salva esse vídeo para não esquecer!",
    visualTips: "Cortes rápidos sem pausas para respirar.",
    captionAndHashtags: "Salve para testar depois! #produtividade #reels",
    status: "RASCUNHO",
    createdAt: created,
    updatedAt: created,
  };
}

test("Unauthenticated user cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const created = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("trends").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("scripts").get());
});

test("Cross-user isolation blocks Bob from accessing or modifying Alice's data", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created));
    await db.collection("users").doc(ALICE_UID).collection("dailySummaries").doc("2026-11-24").set(validSummaryPayload("2026-11-24", created));
    await db.collection("users").doc(ALICE_UID).collection("trends").doc("trend_1").set(validTrendPayload("2026-11-24", created));
    await db.collection("users").doc(ALICE_UID).collection("scripts").doc("script_1").set(validScriptPayload(created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailySummaries").doc("2026-11-24").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trends").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scripts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_1").update({
      status: "PUBLICADO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, query, and partially update CreatorProfile, DailySummary, ViralTrend, and SavedScript", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Profile create + partial update
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      niche: "Marketing Digital",
      weeklyGoal: 7,
      updatedAt: updated,
    })
  );

  // DailySummary create + subcollection query
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailySummaries").doc("2026-11-24").set(validSummaryPayload("2026-11-24", created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailySummaries").where("dateKey", "==", "2026-11-24").get()
  );

  // ViralTrend create + Kotlin query + partial savedToScripts update (including Facebook Reels and rank 12)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("trends").doc("trend_1").set(validTrendPayload("2026-11-24", created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("trends").doc("trend_fb_12").set({
      ...validTrendPayload("2026-11-24", created),
      rank: 12,
      platform: "Facebook Reels",
      estimatedViewsCount: 2800000,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("trends").where("dateKey", "==", "2026-11-24").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("trends").doc("trend_1").update({
      savedToScripts: true,
      updatedAt: updated,
    })
  );

  // SavedScript create + status update + full script edit
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_1").set(validScriptPayload(created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").orderBy("createdAt", "desc").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_1").update({
      status: "GRAVADO",
      updatedAt: updated,
    })
  );
});

test("Security rules reject ghost fields, @DocumentId 'id' leakage, immutable mutations, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const future = new Date(Date.now() + 60000);

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID, created),
      isAdmin: true,
    })
  );

  // Reject @DocumentId 'id' field in trend payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("trends").doc("trend_bad").set({
      ...validTrendPayload("2026-11-24", created),
      id: "trend_bad",
    })
  );

  // Reject invalid platform enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_bad_platform").set({
      ...validScriptPayload(created),
      platform: "Facebook Watch",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_future").set({
      ...validScriptPayload(future),
    })
  );

  // Reject createdAt mutation on update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_ok").set(validScriptPayload(created))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("script_ok").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
