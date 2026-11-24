package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.CreatorProfile
import com.example.model.DailySummary
import com.example.model.SavedScript
import com.example.model.ScriptStatus
import com.example.model.VideoPlatform
import com.example.model.ViralTrend
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ViralRadarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com Google.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Usuário autenticado mudou; operação cancelada." }
        }
        return uid
    }

    fun observeProfile(userId: String): Flow<CreatorProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CreatorProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeDailySummary(userId: String, dateKey: String): Flow<DailySummary?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/dailySummaries/$dateKey"
        emitAll(
            db.collection("users").document(uid)
                .collection("dailySummaries").document(dateKey)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            DailySummary::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeTrendsForDate(userId: String, dateKey: String): Flow<List<ViralTrend>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/trends"
        emitAll(
            db.collection("users").document(uid)
                .collection("trends")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ViralTrend::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ViralTrend> { it.estimatedViewsCount }
                            .thenBy { it.rank }
                    )
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeSavedScripts(userId: String): Flow<List<SavedScript>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/scripts"
        emitAll(
            db.collection("users").document(uid)
                .collection("scripts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedScript::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndLogFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveCreatorProfile(
        userId: String,
        displayName: String,
        niche: String,
        targetAudience: String,
        toneStyle: String,
        preferredPlatforms: List<String>,
        weeklyGoal: Long
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim().take(60).ifBlank { "Criador" }
            val cleanNiche = niche.trim().take(80)
            val cleanAudience = targetAudience.trim().take(160)
            val cleanTone = toneStyle.trim().take(60)
            val cleanPlatforms = preferredPlatforms
                .map { VideoPlatform.normalize(it) }
                .distinct()
                .take(5)
                .ifEmpty { listOf(VideoPlatform.INSTAGRAM.label) }
            val boundedGoal = weeklyGoal.coerceIn(1L, 21L)

            require(cleanNiche.length >= 2) { "Informe um nicho com pelo menos 2 caracteres." }
            require(cleanAudience.length >= 2) { "Descreva seu público-alvo." }
            require(cleanTone.length >= 2) { "Escolha um tom de comunicação." }

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "niche" to cleanNiche,
                        "targetAudience" to cleanAudience,
                        "toneStyle" to cleanTone,
                        "preferredPlatforms" to cleanPlatforms,
                        "weeklyGoal" to boundedGoal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "niche" to cleanNiche,
                        "targetAudience" to cleanAudience,
                        "toneStyle" to cleanTone,
                        "preferredPlatforms" to cleanPlatforms,
                        "weeklyGoal" to boundedGoal,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveDailyAnalysis(
        userId: String,
        summary: DailySummary,
        trends: List<ViralTrend>
    ): Result<Unit> {
        val summaryPath = "users/$userId/dailySummaries/${summary.dateKey}"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)

            // 1. Delete older trends from this same dateKey so re-analysis stays clean
            val existingTrends = userDoc.collection("trends")
                .whereEqualTo("dateKey", summary.dateKey)
                .get()
                .await()

            val batch = db.batch()
            existingTrends.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // 2. Write or update daily summary without violating createdAt immutability
            val summaryRef = userDoc.collection("dailySummaries").document(summary.dateKey)
            val existingSummary = summaryRef.get().await()
            val boundedHashtags = summary.topHashtags.map { it.trim() }.filter { it.isNotBlank() }.take(10)
            if (existingSummary.exists()) {
                batch.update(
                    summaryRef,
                    mapOf(
                        "nicheSnapshot" to summary.nicheSnapshot.trim().take(80),
                        "dailyInsight" to summary.dailyInsight.trim().take(600),
                        "bestPostingWindow" to summary.bestPostingWindow.trim().take(80),
                        "topHashtags" to boundedHashtags,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    summaryRef,
                    mapOf(
                        "dateKey" to summary.dateKey,
                        "nicheSnapshot" to summary.nicheSnapshot.trim().take(80),
                        "dailyInsight" to summary.dailyInsight.trim().take(600),
                        "bestPostingWindow" to summary.bestPostingWindow.trim().take(80),
                        "topHashtags" to boundedHashtags,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 3. Create fresh ViralTrend documents (never include "id" key in map!)
            trends.take(20).forEachIndexed { idx, trend ->
                val trendRef = userDoc.collection("trends").document()
                val beats = trend.bodyBeats.map { it.trim() }.filter { it.isNotBlank() }.take(8)
                    .ifEmpty { listOf("Apresente o ponto principal em passos rápidos.") }
                batch.set(
                    trendRef,
                    mapOf(
                        "dateKey" to summary.dateKey,
                        "rank" to (idx + 1).toLong().coerceIn(1L, 20L),
                        "title" to trend.title.trim().take(120),
                        "platform" to VideoPlatform.normalize(trend.platform),
                        "formatType" to trend.formatType.trim().take(60),
                        "estimatedViewsLabel" to trend.estimatedViewsLabel.trim().take(60),
                        "estimatedViewsCount" to trend.estimatedViewsCount.coerceAtLeast(0L),
                        "engagementScore" to trend.engagementScore.coerceIn(0L, 100L),
                        "whyItWorks" to trend.whyItWorks.trim().take(500),
                        "adaptationAngle" to trend.adaptationAngle.trim().take(500),
                        "viralAudioStyle" to trend.viralAudioStyle.trim().take(120),
                        "durationSeconds" to trend.durationSeconds.coerceIn(5L, 180L),
                        "hookText" to trend.hookText.trim().take(400),
                        "bodyBeats" to beats,
                        "ctaText" to trend.ctaText.trim().take(300),
                        "visualTips" to trend.visualTips.trim().take(500),
                        "captionAndHashtags" to trend.captionAndHashtags.trim().take(600),
                        "savedToScripts" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, summaryPath)
            Result.failure(e)
        }
    }

    suspend fun saveScriptFromTrend(userId: String, trend: ViralTrend): Result<Unit> {
        val path = "users/$userId/scripts"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)
            val scriptRef = userDoc.collection("scripts").document()
            val beats = trend.bodyBeats.map { it.trim() }.filter { it.isNotBlank() }.take(8)
                .ifEmpty { listOf("Apresente a ideia principal de forma direta.") }

            val batch = db.batch()
            batch.set(
                scriptRef,
                mapOf(
                    "title" to trend.title.trim().take(120),
                    "platform" to VideoPlatform.normalize(trend.platform),
                    "trendInspirationTitle" to trend.title.trim().take(140),
                    "engagementScore" to trend.engagementScore.coerceIn(0L, 100L),
                    "durationSeconds" to trend.durationSeconds.coerceIn(5L, 180L),
                    "hookText" to trend.hookText.trim().take(400),
                    "bodyBeats" to beats,
                    "ctaText" to trend.ctaText.trim().take(300),
                    "visualTips" to trend.visualTips.trim().take(500),
                    "captionAndHashtags" to trend.captionAndHashtags.trim().take(600),
                    "status" to ScriptStatus.RASCUNHO.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (trend.id.isNotBlank() && !trend.savedToScripts) {
                val trendRef = userDoc.collection("trends").document(trend.id)
                batch.update(
                    trendRef,
                    mapOf(
                        "savedToScripts" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTrendAdaptedScript(
        userId: String,
        trendId: String,
        hookText: String,
        bodyBeats: List<String>,
        ctaText: String,
        visualTips: String,
        captionAndHashtags: String,
        durationSeconds: Long
    ): Result<Unit> {
        val path = "users/$userId/trends/$trendId"
        return try {
            val uid = requireUserId(userId)
            require(trendId.isNotBlank()) { "Trend ID inválido." }
            val beats = bodyBeats.map { it.trim() }.filter { it.isNotBlank() }.take(8)
                .ifEmpty { listOf("Apresente o ponto principal com energia.") }

            db.collection("users").document(uid)
                .collection("trends").document(trendId)
                .update(
                    mapOf(
                        "hookText" to hookText.trim().take(400),
                        "bodyBeats" to beats,
                        "ctaText" to ctaText.trim().take(300),
                        "visualTips" to visualTips.trim().take(500),
                        "captionAndHashtags" to captionAndHashtags.trim().take(600),
                        "durationSeconds" to durationSeconds.coerceIn(5L, 180L),
                        "savedToScripts" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScriptStatus(
        userId: String,
        scriptId: String,
        status: ScriptStatus
    ): Result<Unit> {
        val path = "users/$userId/scripts/$scriptId"
        return try {
            val uid = requireUserId(userId)
            require(scriptId.isNotBlank()) { "Script ID inválido." }
            db.collection("users").document(uid)
                .collection("scripts").document(scriptId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSavedScriptContent(
        userId: String,
        scriptId: String,
        title: String,
        platform: String,
        durationSeconds: Long,
        hookText: String,
        bodyBeats: List<String>,
        ctaText: String,
        visualTips: String,
        captionAndHashtags: String,
        status: ScriptStatus
    ): Result<Unit> {
        val path = "users/$userId/scripts/$scriptId"
        return try {
            val uid = requireUserId(userId)
            require(scriptId.isNotBlank()) { "Script ID inválido." }
            val beats = bodyBeats.map { it.trim() }.filter { it.isNotBlank() }.take(8)
                .ifEmpty { listOf("Apresente o conteúdo principal em passos diretos.") }
            db.collection("users").document(uid)
                .collection("scripts").document(scriptId)
                .update(
                    mapOf(
                        "title" to title.trim().take(120),
                        "platform" to VideoPlatform.normalize(platform),
                        "durationSeconds" to durationSeconds.coerceIn(5L, 180L),
                        "hookText" to hookText.trim().take(400),
                        "bodyBeats" to beats,
                        "ctaText" to ctaText.trim().take(300),
                        "visualTips" to visualTips.trim().take(500),
                        "captionAndHashtags" to captionAndHashtags.trim().take(600),
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedScript(userId: String, scriptId: String): Result<Unit> {
        val path = "users/$userId/scripts/$scriptId"
        return try {
            val uid = requireUserId(userId)
            require(scriptId.isNotBlank()) { "Script ID inválido." }
            db.collection("users").document(uid)
                .collection("scripts").document(scriptId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun unwrapAndLogFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
