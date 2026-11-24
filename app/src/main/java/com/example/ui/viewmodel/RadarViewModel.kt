package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ViralAiService
import com.example.data.ViralRadarRepository
import com.example.model.CreatorProfile
import com.example.model.DailySummary
import com.example.model.SavedScript
import com.example.model.ScriptStatus
import com.example.model.UiState
import com.example.model.ViralTrend
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyAnalysisActionState(
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null
)

data class ScriptAdaptActionState(
    val isAdapting: Boolean = false,
    val errorMessage: String? = null
)

class RadarViewModel(
    private val repository: ViralRadarRepository,
    private val aiService: ViralAiService,
    private val currentUserId: String
) : ViewModel() {

    val todayDateKey: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayFormattedPt: String = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")).format(Date())

    val profileState: StateFlow<UiState<CreatorProfile?>> = repository.observeProfile(currentUserId)
        .map<CreatorProfile?, UiState<CreatorProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar perfil."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val dailySummaryState: StateFlow<UiState<DailySummary?>> = repository.observeDailySummary(currentUserId, todayDateKey)
        .map<DailySummary?, UiState<DailySummary?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing daily summary", root)
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar resumo diário."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val todayTrendsState: StateFlow<UiState<List<ViralTrend>>> = repository.observeTrendsForDate(currentUserId, todayDateKey)
        .map<List<ViralTrend>, UiState<List<ViralTrend>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing today's trends", root)
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar tendências."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val savedScriptsState: StateFlow<UiState<List<SavedScript>>> = repository.observeSavedScripts(currentUserId)
        .map<List<SavedScript>, UiState<List<SavedScript>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrRethrow(error)
            Log.w(TAG, "Error observing saved scripts", root)
            emit(UiState.Error(root.localizedMessage ?: "Erro ao carregar roteiros salvos."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _analysisActionState = MutableStateFlow(DailyAnalysisActionState())
    val analysisActionState: StateFlow<DailyAnalysisActionState> = _analysisActionState.asStateFlow()

    private val _adaptActionState = MutableStateFlow(ScriptAdaptActionState())
    val adaptActionState: StateFlow<ScriptAdaptActionState> = _adaptActionState.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedPlatformFilter: StateFlow<String?> = _selectedPlatformFilter.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var hasAutoTriggeredToday = false

    fun setPlatformFilter(platform: String?) {
        _selectedPlatformFilter.value = platform
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun clearAnalysisError() {
        _analysisActionState.value = _analysisActionState.value.copy(errorMessage = null)
    }

    fun clearAdaptError() {
        _adaptActionState.value = _adaptActionState.value.copy(errorMessage = null)
    }

    fun maybeTriggerDailyAutoAnalysis(profile: CreatorProfile?, summary: DailySummary?) {
        if (hasAutoTriggeredToday || _analysisActionState.value.isAnalyzing) return
        if (profile != null && summary == null) {
            hasAutoTriggeredToday = true
            runDailyAnalysis(profile = profile, focusTopic = null)
        }
    }

    fun saveCreatorProfile(
        displayName: String,
        niche: String,
        targetAudience: String,
        toneStyle: String,
        preferredPlatforms: List<String>,
        weeklyGoal: Long,
        triggerInitialRadar: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.saveCreatorProfile(
                userId = currentUserId,
                displayName = displayName,
                niche = niche,
                targetAudience = targetAudience,
                toneStyle = toneStyle,
                preferredPlatforms = preferredPlatforms,
                weeklyGoal = weeklyGoal
            )
            result.onSuccess {
                _toastMessage.value = "Perfil salvo!"
                if (triggerInitialRadar) {
                    val tempProfile = CreatorProfile(
                        userId = currentUserId,
                        displayName = displayName.trim().ifBlank { "Criador" },
                        niche = niche.trim(),
                        targetAudience = targetAudience.trim(),
                        toneStyle = toneStyle.trim(),
                        preferredPlatforms = preferredPlatforms,
                        weeklyGoal = weeklyGoal
                    )
                    hasAutoTriggeredToday = true
                    runDailyAnalysis(tempProfile, null)
                }
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Não foi possível salvar o perfil."
            }
        }
    }

    fun runDailyAnalysis(profile: CreatorProfile, focusTopic: String? = null) {
        if (_analysisActionState.value.isAnalyzing) return
        viewModelScope.launch {
            _analysisActionState.value = DailyAnalysisActionState(isAnalyzing = true, errorMessage = null)
            val aiResult = aiService.analyzeDailyTrends(
                profile = profile,
                dateKey = todayDateKey,
                focusTopic = focusTopic
            )
            aiResult.fold(
                onSuccess = { radarResult ->
                    val saveResult = repository.saveDailyAnalysis(
                        userId = currentUserId,
                        summary = radarResult.summary,
                        trends = radarResult.trends
                    )
                    saveResult.fold(
                        onSuccess = {
                            _analysisActionState.value = DailyAnalysisActionState(isAnalyzing = false)
                            _toastMessage.value = "Radar de hoje atualizado com ${radarResult.trends.size} tendências!"
                        },
                        onFailure = { err ->
                            _analysisActionState.value = DailyAnalysisActionState(
                                isAnalyzing = false,
                                errorMessage = err.localizedMessage ?: "Erro ao salvar análise diária."
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _analysisActionState.value = DailyAnalysisActionState(
                        isAnalyzing = false,
                        errorMessage = err.localizedMessage ?: "Falha na análise com IA."
                    )
                }
            )
        }
    }

    fun adaptTrendScript(profile: CreatorProfile, trend: ViralTrend, customTopic: String) {
        if (_adaptActionState.value.isAdapting) return
        val cleanTopic = customTopic.trim()
        if (cleanTopic.length < 3) {
            _adaptActionState.value = ScriptAdaptActionState(
                isAdapting = false,
                errorMessage = "Descreva o assunto com pelo menos 3 caracteres."
            )
            return
        }
        viewModelScope.launch {
            _adaptActionState.value = ScriptAdaptActionState(isAdapting = true, errorMessage = null)
            val aiResult = aiService.adaptScriptForTopic(profile, trend, cleanTopic)
            aiResult.fold(
                onSuccess = { adapted ->
                    val saveRes = repository.updateTrendAdaptedScript(
                        userId = currentUserId,
                        trendId = trend.id,
                        hookText = adapted.hookText,
                        bodyBeats = adapted.bodyBeats,
                        ctaText = adapted.ctaText,
                        visualTips = adapted.visualTips,
                        captionAndHashtags = adapted.captionAndHashtags,
                        durationSeconds = adapted.durationSeconds
                    )
                    saveRes.fold(
                        onSuccess = {
                            _adaptActionState.value = ScriptAdaptActionState(isAdapting = false)
                            _toastMessage.value = "Roteiro adaptado para \"$cleanTopic\"!"
                        },
                        onFailure = { err ->
                            _adaptActionState.value = ScriptAdaptActionState(
                                isAdapting = false,
                                errorMessage = err.localizedMessage ?: "Erro ao salvar adaptação."
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _adaptActionState.value = ScriptAdaptActionState(
                        isAdapting = false,
                        errorMessage = err.localizedMessage ?: "Erro ao adaptar roteiro."
                    )
                }
            )
        }
    }

    fun saveTrendToLibrary(trend: ViralTrend) {
        viewModelScope.launch {
            val res = repository.saveScriptFromTrend(currentUserId, trend)
            res.onSuccess {
                _toastMessage.value = "Roteiro salvo em Meus Roteiros!"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Erro ao salvar roteiro."
            }
        }
    }

    fun updateScriptStatus(scriptId: String, newStatus: ScriptStatus) {
        viewModelScope.launch {
            val res = repository.updateScriptStatus(currentUserId, scriptId, newStatus)
            res.onSuccess {
                _toastMessage.value = "Status atualizado para ${newStatus.labelPt}"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Erro ao atualizar status."
            }
        }
    }

    fun updateSavedScript(
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
    ) {
        viewModelScope.launch {
            val res = repository.updateSavedScriptContent(
                userId = currentUserId,
                scriptId = scriptId,
                title = title,
                platform = platform,
                durationSeconds = durationSeconds,
                hookText = hookText,
                bodyBeats = bodyBeats,
                ctaText = ctaText,
                visualTips = visualTips,
                captionAndHashtags = captionAndHashtags,
                status = status
            )
            res.onSuccess {
                _toastMessage.value = "Roteiro atualizado!"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Erro ao atualizar roteiro."
            }
        }
    }

    fun deleteScript(scriptId: String) {
        viewModelScope.launch {
            val res = repository.deleteSavedScript(currentUserId, scriptId)
            res.onSuccess {
                _toastMessage.value = "Roteiro removido."
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Erro ao excluir roteiro."
            }
        }
    }

    private fun unwrapFirestoreErrorOrRethrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "RadarViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
