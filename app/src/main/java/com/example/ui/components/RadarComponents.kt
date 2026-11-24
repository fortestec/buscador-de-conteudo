package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.VideoPlatform
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralElectric
import com.example.ui.theme.CyanCyber
import com.example.ui.theme.EmeraldViral

@Composable
fun EngagementScoreRing(
    score: Long,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val clamped = score.coerceIn(0L, 100L)
    var targetProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(clamped) {
        targetProgress = clamped / 100f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 700),
        label = "engagement_ring"
    )

    val ringColor = when {
        clamped >= 90L -> EmeraldViral
        clamped >= 78L -> CyanCyber
        else -> AmberWarm
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 6.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${clamped}%",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "match",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlatformBadge(
    platform: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when {
        platform.contains("Facebook", ignoreCase = true) -> Triple(
            Color(0xFF3B82F6).copy(alpha = 0.18f),
            Color(0xFF60A5FA),
            VideoPlatform.FACEBOOK.shortLabel
        )
        platform.contains("TikTok", ignoreCase = true) -> Triple(
            CyanCyber.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary,
            VideoPlatform.TIKTOK.shortLabel
        )
        platform.contains("Shorts", ignoreCase = true) || platform.contains("YouTube", ignoreCase = true) -> Triple(
            AmberWarm.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.tertiary,
            VideoPlatform.YOUTUBE.shortLabel
        )
        else -> Triple(
            CoralElectric.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.primary,
            VideoPlatform.INSTAGRAM.shortLabel
        )
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun openPlatformVideoSearch(
    context: android.content.Context,
    platform: String,
    query: String
) {
    val encoded = android.net.Uri.encode(query)
    val url = when {
        platform.contains("YouTube", ignoreCase = true) || platform.contains("Shorts", ignoreCase = true) ->
            "https://www.youtube.com/results?search_query=$encoded"
        platform.contains("TikTok", ignoreCase = true) ->
            "https://www.tiktok.com/search?q=$encoded"
        platform.contains("Facebook", ignoreCase = true) ->
            "https://www.facebook.com/search/videos/?q=$encoded"
        else ->
            "https://www.instagram.com/explore/search/keyword/?q=$encoded"
    }
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore if no browser/handler is available on device
    }
}

@Composable
fun CopyableScriptSectionCard(
    stepLabel: String,
    title: String,
    content: String,
    accentColor: Color,
    copyTestTag: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$stepLabel • $title",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(copyTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copiar $title",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TrendAnalysisLoadingCard(
    niche: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = "Analisando tendências",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Escaneando os Reels e vídeos em alta...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Calculando probabilidade de engajamento para \"$niche\" e montando roteiros simples para você gravar hoje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
