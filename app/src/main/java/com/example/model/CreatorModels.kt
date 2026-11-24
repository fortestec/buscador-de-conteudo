package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ScriptStatus(val labelPt: String) {
    RASCUNHO("Rascunho"),
    GRAVADO("Gravado"),
    PUBLICADO("Publicado");

    companion object {
        fun fromRaw(raw: String): ScriptStatus {
            return entries.firstOrNull { it.name == raw } ?: RASCUNHO
        }
    }
}

enum class VideoPlatform(val label: String, val shortLabel: String) {
    YOUTUBE("YouTube Shorts", "YouTube"),
    TIKTOK("TikTok", "TikTok"),
    INSTAGRAM("Instagram Reels", "Instagram"),
    FACEBOOK("Facebook Reels", "Facebook");

    companion object {
        fun normalize(raw: String): String {
            return when {
                raw.contains("Facebook", ignoreCase = true) || raw.contains("Face", ignoreCase = true) -> FACEBOOK.label
                raw.contains("TikTok", ignoreCase = true) -> TIKTOK.label
                raw.contains("Shorts", ignoreCase = true) || raw.contains("YouTube", ignoreCase = true) -> YOUTUBE.label
                else -> INSTAGRAM.label
            }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CreatorProfile(
    val userId: String = "",
    val displayName: String = "",
    val niche: String = "",
    val targetAudience: String = "",
    val toneStyle: String = "",
    val preferredPlatforms: List<String> = emptyList(),
    val weeklyGoal: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailySummary(
    val dateKey: String = "",
    val nicheSnapshot: String = "",
    val dailyInsight: String = "",
    val bestPostingWindow: String = "",
    val topHashtags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ViralTrend(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val rank: Long = 1L,
    val title: String = "",
    val platform: String = VideoPlatform.INSTAGRAM.label,
    val formatType: String = "",
    val estimatedViewsLabel: String = "",
    val estimatedViewsCount: Long = 0L,
    val engagementScore: Long = 85L,
    val whyItWorks: String = "",
    val adaptationAngle: String = "",
    val viralAudioStyle: String = "",
    val durationSeconds: Long = 30L,
    val hookText: String = "",
    val bodyBeats: List<String> = emptyList(),
    val ctaText: String = "",
    val visualTips: String = "",
    val captionAndHashtags: String = "",
    val savedToScripts: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedScript(
    @DocumentId val id: String = "",
    val title: String = "",
    val platform: String = VideoPlatform.INSTAGRAM.label,
    val trendInspirationTitle: String = "",
    val engagementScore: Long = 85L,
    val durationSeconds: Long = 30L,
    val hookText: String = "",
    val bodyBeats: List<String> = emptyList(),
    val ctaText: String = "",
    val visualTips: String = "",
    val captionAndHashtags: String = "",
    val status: String = ScriptStatus.RASCUNHO.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val scriptStatus: ScriptStatus
        get() = ScriptStatus.fromRaw(status)
}
