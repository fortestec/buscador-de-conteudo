package com.example.ai

import android.util.Log
import com.example.model.CreatorProfile
import com.example.model.DailySummary
import com.example.model.VideoPlatform
import com.example.model.ViralTrend
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class DailyRadarResult(
    val summary: DailySummary,
    val trends: List<ViralTrend>
)

data class AdaptedScriptResult(
    val hookText: String,
    val bodyBeats: List<String>,
    val ctaText: String,
    val visualTips: String,
    val captionAndHashtags: String,
    val durationSeconds: Long
)

class ViralAiService {
    private val dailyRadarSchema = Schema.obj(
        properties = mapOf(
            "dailyInsight" to Schema.string(
                description = "Resumo estratégico de hoje (2 a 3 frases em português) explicando quais padrões de reels e vídeos curtos estão com mais visualizações e retenção."
            ),
            "bestPostingWindow" to Schema.string(
                description = "Melhor faixa de horário recomendada para postar hoje, ex: 18h30 - 20h30"
            ),
            "topHashtags" to Schema.array(
                items = Schema.string(),
                description = "Lista de 4 a 6 hashtags relevantes em alta hoje (máximo 10)."
            ),
            "trends" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "rank" to Schema.integer(description = "Posição de 1 a 12"),
                        "title" to Schema.string(description = "Título curto e direto do vídeo/formato mais visto (máx 90 caracteres)"),
                        "platform" to Schema.enumeration(
                            values = listOf("YouTube Shorts", "TikTok", "Instagram Reels", "Facebook Reels"),
                            description = "Plataforma do vídeo"
                        ),
                        "formatType" to Schema.string(description = "Tipo de formato, ex: POV / Bastidores, Mito vs Realidade, Lista Express, Humor Rápido"),
                        "estimatedViewsLabel" to Schema.string(description = "Faixa estimada de visualizações, ex: 3.4M views em alta"),
                        "estimatedViewsCount" to Schema.integer(description = "Número inteiro estimado de visualizações para ordenar os 3 mais vistos (ex: 3400000)"),
                        "engagementScore" to Schema.integer(description = "Probabilidade personalizada de engajamento de 65 a 98 para este criador"),
                        "whyItWorks" to Schema.string(description = "Explicação prática do porquê esse vídeo está entre os mais vistos hoje"),
                        "adaptationAngle" to Schema.string(description = "Como o usuário pode usar esse vídeo viral como inspiração no próprio nicho"),
                        "viralAudioStyle" to Schema.string(description = "Estilo de áudio/música ou ritmo recomendado"),
                        "durationSeconds" to Schema.integer(description = "Duração recomendada em segundos entre 15 e 60"),
                        "hookText" to Schema.string(description = "Frase exata e simples para falar ou mostrar nos primeiros 3 segundos (gancho)"),
                        "bodyBeats" to Schema.array(
                            items = Schema.string(),
                            description = "Lista de 3 a 4 passos cronológicos simples para gravar, ex: [03s-12s] Mostre a cena..."
                        ),
                        "ctaText" to Schema.string(description = "Chamada para ação final simples para gerar comentários ou salvamentos"),
                        "visualTips" to Schema.string(description = "Dica objetiva de gravação, legenda na tela e enquadramento com celular"),
                        "captionAndHashtags" to Schema.string(description = "Legenda pronta para postar com 4 hashtags")
                    )
                ),
                description = "Exatamente 12 vídeos mais visualizados: 3 para YouTube Shorts, 3 para TikTok, 3 para Instagram Reels e 3 para Facebook Reels."
            )
        )
    )

    private val adaptScriptSchema = Schema.obj(
        properties = mapOf(
            "hookText" to Schema.string(description = "Novo gancho magnético de 0 a 3 segundos adaptado ao tema pedido"),
            "bodyBeats" to Schema.array(
                items = Schema.string(),
                description = "Lista de 3 a 5 passos simples e cronológicos para gravar"
            ),
            "ctaText" to Schema.string(description = "Chamada para ação final para comentários/salvamentos"),
            "visualTips" to Schema.string(description = "Dicas simples de câmera, texto na tela e ritmo"),
            "captionAndHashtags" to Schema.string(description = "Legenda pronta com hashtags"),
            "durationSeconds" to Schema.integer(description = "Duração ideal em segundos entre 15 e 60")
        )
    )

    private val dailyRadarConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = dailyRadarSchema
            temperature = 0.7f
        }
    }

    private val adaptScriptConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = adaptScriptSchema
            temperature = 0.7f
        }
    }

    private val dailyRadarPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = dailyRadarConfig
        )
    }

    private val dailyRadarFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = dailyRadarConfig
        )
    }

    private val adaptScriptPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = adaptScriptConfig
        )
    }

    private val adaptScriptFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = adaptScriptConfig
        )
    }

    private fun isTransientDemandError(error: Throwable): Boolean {
        val msg = generateSequence(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return msg.contains("high demand") ||
            msg.contains("overloaded") ||
            msg.contains("temporarily unavailable") ||
            msg.contains("unavailable") ||
            msg.contains("resource exhausted") ||
            msg.contains("429") ||
            msg.contains("503") ||
            msg.contains("try again later")
    }

    private suspend fun generateWithRetryAndFallback(
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        prompt: String
    ): GenerateContentResponse {
        return withTimeout(60_000L) {
            try {
                primaryModel.generateContent(prompt)
            } catch (primaryError: Exception) {
                if (primaryError is CancellationException) throw primaryError
                if (!isTransientDemandError(primaryError)) throw primaryError

                Log.w(TAG, "Primary model ($PRIMARY_MODEL) busy, switching to $FALLBACK_MODEL", primaryError)
                delay(800L)
                try {
                    fallbackModel.generateContent(prompt)
                } catch (fallbackError: Exception) {
                    if (fallbackError is CancellationException) throw fallbackError
                    if (!isTransientDemandError(fallbackError)) throw fallbackError

                    Log.w(TAG, "Fallback model ($FALLBACK_MODEL) busy, retrying once", fallbackError)
                    delay(1800L)
                    fallbackModel.generateContent(prompt)
                }
            }
        }
    }

    suspend fun analyzeDailyTrends(
        profile: CreatorProfile,
        dateKey: String,
        focusTopic: String? = null
    ): Result<DailyRadarResult> {
        val platformsList = if (profile.preferredPlatforms.isEmpty()) {
            "YouTube Shorts, TikTok, Instagram Reels, Facebook Reels"
        } else {
            profile.preferredPlatforms.joinToString(", ")
        }
        val extraFocus = if (!focusTopic.isNullOrBlank()) {
            "\nFOCO ESPECIAL DE HOJE PEDIDO PELO CRIADOR: $focusTopic"
        } else {
            ""
        }

        val prompt = """
            Você é um estrategista sênior de conteúdo viral no Brasil para YouTube Shorts, TikTok, Instagram Reels e Facebook Reels.
            Data de hoje: $dateKey.
            
            Perfil do Criador:
            - Nome: ${profile.displayName}
            - Nicho principal: ${profile.niche}
            - Público-alvo: ${profile.targetAudience}
            - Tom de comunicação: ${profile.toneStyle}
            - Plataformas preferidas: $platformsList
            $extraFocus
            
            Sua tarefa diária OBRIGATÓRIA:
            1. Gere EXATAMENTE 12 análises de vídeos mais visualizados em alta hoje:
               - 3 vídeos para "YouTube Shorts"
               - 3 vídeos para "TikTok"
               - 3 vídeos para "Instagram Reels"
               - 3 vídeos para "Facebook Reels"
            2. Para cada vídeo, estime as visualizações reais em alta (estimatedViewsCount inteiro, ex: 4200000 para 4.2M, e estimatedViewsLabel) e a probabilidade de engajamento (65 a 98) para ESTE criador replicar usando-o como inspiração.
            3. Crie um ROTEIRO SIMPLES, direto e fácil de gravar só com o celular para cada um dos 12 vídeos (gancho de 3s, 3 passos rápidos, CTA, dica visual e legenda).
            
            Regras obrigatórias:
            - Escreva 100% em Português do Brasil, linguagem natural e prática.
            - Seja objetivo e conciso em cada campo (títulos até 85 caracteres, whyItWorks e adaptationAngle até 260 caracteres, bodyBeats com 3 passos curtos).
            - Não use markdown nem emojis excessivos nos campos estruturados.
        """.trimIndent()

        return try {
            val response = generateWithRetryAndFallback(
                primaryModel = dailyRadarPrimaryModel,
                fallbackModel = dailyRadarFallbackModel,
                prompt = prompt
            )
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("A IA retornou uma resposta vazia."))

            val root = JSONObject(jsonText)
            val hashtagsArray = root.optJSONArray("topHashtags")
            val topHashtags = if (hashtagsArray != null) {
                (0 until minOf(hashtagsArray.length(), 10))
                    .map { hashtagsArray.optString(it).trim() }
                    .filter { it.isNotBlank() }
            } else {
                emptyList()
            }

            val summary = DailySummary(
                dateKey = dateKey,
                nicheSnapshot = profile.niche.take(80),
                dailyInsight = root.optString("dailyInsight").trim().take(580)
                    .ifBlank { "Vídeos curtos com gancho nos primeiros 3 segundos e resposta direta a uma dor do público estão liderando o alcance hoje." },
                bestPostingWindow = root.optString("bestPostingWindow").trim().take(80)
                    .ifBlank { "18h30 - 20h30" },
                topHashtags = topHashtags
            )

            val trendsArray = root.getJSONArray("trends")
            val trends = (0 until minOf(trendsArray.length(), 16)).map { idx ->
                val obj = trendsArray.getJSONObject(idx)
                val beatsArray = obj.optJSONArray("bodyBeats")
                val beats = if (beatsArray != null && beatsArray.length() > 0) {
                    (0 until minOf(beatsArray.length(), 8))
                        .map { beatsArray.optString(it).trim() }
                        .filter { it.isNotBlank() }
                        .ifEmpty { listOf("[03s-20s] Desenvolva a ideia principal com exemplos diretos.") }
                } else {
                    listOf("[03s-20s] Desenvolva a ideia principal com exemplos diretos.")
                }

                val viewsCount = obj.optLong("estimatedViewsCount", 1_000_000L).coerceAtLeast(10_000L)
                ViralTrend(
                    dateKey = dateKey,
                    rank = (idx + 1).toLong(),
                    title = obj.optString("title").trim().take(115).ifBlank { "Tendência Viral #${idx + 1}" },
                    platform = VideoPlatform.normalize(obj.optString("platform")),
                    formatType = obj.optString("formatType").trim().take(58).ifBlank { "Formato Curto" },
                    estimatedViewsLabel = obj.optString("estimatedViewsLabel").trim().take(58).ifBlank { "1M+ views em alta" },
                    estimatedViewsCount = viewsCount,
                    engagementScore = obj.optInt("engagementScore", 85).coerceIn(1, 100).toLong(),
                    whyItWorks = obj.optString("whyItWorks").trim().take(480).ifBlank { "Retenção alta nos 3 primeiros segundos." },
                    adaptationAngle = obj.optString("adaptationAngle").trim().take(480).ifBlank { "Adapte o exemplo principal para a rotina do seu público." },
                    viralAudioStyle = obj.optString("viralAudioStyle").trim().take(115).ifBlank { "Áudio dinâmico em alta" },
                    durationSeconds = obj.optInt("durationSeconds", 30).coerceIn(5, 180).toLong(),
                    hookText = obj.optString("hookText").trim().take(380).ifBlank { "Você precisa testar isso hoje!" },
                    bodyBeats = beats,
                    ctaText = obj.optString("ctaText").trim().take(280).ifBlank { "Salva esse vídeo e comenta o que achou!" },
                    visualTips = obj.optString("visualTips").trim().take(480).ifBlank { "Boa iluminação frontal, cortes rápidos e legenda grande." },
                    captionAndHashtags = obj.optString("captionAndHashtags").trim().take(580).ifBlank { "Salve para aplicar hoje! #reels #dicas" },
                    savedToScripts = false
                )
            }.sortedWith(
                compareByDescending<ViralTrend> { it.estimatedViewsCount }
                    .thenByDescending { it.engagementScore }
            ).mapIndexed { index, trend -> trend.copy(rank = (index + 1).toLong()) }

            if (trends.isEmpty()) {
                return Result.failure(IllegalStateException("Não foi possível gerar tendências para hoje."))
            }

            Result.success(DailyRadarResult(summary = summary, trends = trends))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Daily trend analysis timed out", e)
            Result.failure(IllegalStateException("A análise demorou mais de 60 segundos. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze daily trends", e)
            val friendly = if (isTransientDemandError(e)) {
                IllegalStateException(
                    "Os servidores de IA estão com alta demanda agora. Tentamos automaticamente o modelo reserva, mas ainda há congestionamento. Toque em 'Tentar Novamente' em alguns segundos.",
                    e
                )
            } else {
                e
            }
            Result.failure(friendly)
        }
    }

    suspend fun adaptScriptForTopic(
        profile: CreatorProfile,
        trend: ViralTrend,
        customTopic: String
    ): Result<AdaptedScriptResult> {
        val prompt = """
            Você é um roteirista especialista em vídeos curtos virais no Brasil.
            Adapte este formato viral mantendo a mesma estrutura psicológica que gera engajamento, mas agora 100% focado no assunto específico pedido pelo criador.
            
            Formato de inspiração:
            - Título: ${trend.title}
            - Plataforma: ${trend.platform}
            - Tipo: ${trend.formatType}
            - Por que viraliza: ${trend.whyItWorks}
            
            Perfil do Criador:
            - Nicho: ${profile.niche}
            - Público: ${profile.targetAudience}
            - Tom: ${profile.toneStyle}
            
            NOVO ASSUNTO ESPECÍFICO PEDIDO: $customTopic
            
            Gere um roteiro simples, direto e pronto para gravar com o celular em Português do Brasil.
        """.trimIndent()

        return try {
            val response = generateWithRetryAndFallback(
                primaryModel = adaptScriptPrimaryModel,
                fallbackModel = adaptScriptFallbackModel,
                prompt = prompt
            )
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("A IA retornou uma resposta vazia."))

            val obj = JSONObject(jsonText)
            val beatsArray = obj.optJSONArray("bodyBeats")
            val beats = if (beatsArray != null && beatsArray.length() > 0) {
                (0 until minOf(beatsArray.length(), 8))
                    .map { beatsArray.optString(it).trim() }
                    .filter { it.isNotBlank() }
                    .ifEmpty { trend.bodyBeats }
            } else {
                trend.bodyBeats
            }

            Result.success(
                AdaptedScriptResult(
                    hookText = obj.optString("hookText").trim().take(380).ifBlank { trend.hookText },
                    bodyBeats = beats,
                    ctaText = obj.optString("ctaText").trim().take(280).ifBlank { trend.ctaText },
                    visualTips = obj.optString("visualTips").trim().take(480).ifBlank { trend.visualTips },
                    captionAndHashtags = obj.optString("captionAndHashtags").trim().take(580).ifBlank { trend.captionAndHashtags },
                    durationSeconds = obj.optInt("durationSeconds", trend.durationSeconds.toInt()).coerceIn(5, 180).toLong()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Script adaptation timed out", e)
            Result.failure(IllegalStateException("A adaptação demorou mais de 60 segundos. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adapt script", e)
            val friendly = if (isTransientDemandError(e)) {
                IllegalStateException(
                    "Os servidores de IA estão temporariamente congestionados. Tente adaptar novamente em alguns segundos.",
                    e
                )
            } else {
                e
            }
            Result.failure(friendly)
        }
    }

    private companion object {
        const val TAG = "ViralAiService"
        const val PRIMARY_MODEL = "gemini-3.8-flash"
        const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
    }
}
