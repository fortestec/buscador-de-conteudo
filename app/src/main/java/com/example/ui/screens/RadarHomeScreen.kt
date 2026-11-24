package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CreatorProfile
import com.example.model.DailySummary
import com.example.model.UiState
import com.example.model.VideoPlatform
import com.example.model.ViralTrend
import com.example.ui.components.EngagementScoreRing
import com.example.ui.components.PlatformBadge
import com.example.ui.components.TrendAnalysisLoadingCard
import com.example.ui.components.openPlatformVideoSearch
import com.example.ui.theme.AmberWarm
import com.example.ui.viewmodel.DailyAnalysisActionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadarHomeScreen(
    profile: CreatorProfile,
    todayFormatted: String,
    summaryState: UiState<DailySummary?>,
    trendsState: UiState<List<ViralTrend>>,
    analysisActionState: DailyAnalysisActionState,
    selectedPlatformFilter: String?,
    onSelectPlatformFilter: (String?) -> Unit,
    onRunDailyAnalysis: (focusTopic: String?) -> Unit,
    onDismissError: () -> Unit,
    onOpenTrendScript: (ViralTrend) -> Unit,
    onSaveTrendToLibrary: (ViralTrend) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFocusDialog by remember { mutableStateOf(false) }
    var focusTopicText by remember { mutableStateOf("") }
    val rememberedFilteredTrends = remember(trendsState, selectedPlatformFilter) {
        val raw = (trendsState as? UiState.Success)?.data.orEmpty()
        raw.filter { trend ->
            selectedPlatformFilter == null || trend.platform == selectedPlatformFilter
        }.sortedWith(
            compareByDescending<ViralTrend> { it.estimatedViewsCount }
                .thenBy { it.rank }
        )
    }

    val focusPresets = remember {
        listOf(
            "Vídeos de motoboys engraçados",
            "Perrengues de entrega e trânsito",
            "Humor do dia a dia",
            "Bastidores do meu trabalho"
        )
    }

    if (showFocusDialog) {
        AlertDialog(
            onDismissRequest = { showFocusDialog = false },
            title = { Text("Analisar com Foco Específico") },
            text = {
                Column {
                    Text(
                        text = "Escolha uma opção rápida ou digite um tema para buscar os Reels e vídeos em alta:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        focusPresets.forEach { preset ->
                            FilterChip(
                                selected = focusTopicText.equals(preset, ignoreCase = true),
                                onClick = { focusTopicText = preset },
                                label = { Text(preset) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = focusTopicText,
                        onValueChange = { focusTopicText = it.take(100) },
                        label = { Text("Ex: Vídeos de motoboys engraçados") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focus_topic_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFocusDialog = false
                        onRunDailyAnalysis(focusTopicText.trim().ifBlank { null })
                    },
                    modifier = Modifier.testTag("confirm_focus_analysis_button")
                ) {
                    Text("Analisar Agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFocusDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                RadarHeaderCard(
                    profile = profile,
                    todayFormatted = todayFormatted,
                    isAnalyzing = analysisActionState.isAnalyzing,
                    onRefreshToday = { onRunDailyAnalysis(null) },
                    onFocusTopicClick = { showFocusDialog = true }
                )
            }

            if (!analysisActionState.errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = analysisActionState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    onDismissError()
                                    onRunDailyAnalysis(null)
                                }) {
                                    Text("Tentar Novamente")
                                }
                                TextButton(onClick = onDismissError) {
                                    Text("Fechar")
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = analysisActionState.isAnalyzing) {
                    TrendAnalysisLoadingCard(niche = profile.niche)
                }
            }

            if (summaryState is UiState.Success && summaryState.data != null) {
                val summary = summaryState.data
                item {
                    DailyInsightCard(summary = summary)
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedPlatformFilter == null,
                            onClick = { onSelectPlatformFilter(null) },
                            label = { Text("Todas") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_platform_all")
                        )
                    }
                    items(VideoPlatform.entries) { platform ->
                        val selected = selectedPlatformFilter == platform.label
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSelectPlatformFilter(if (selected) null else platform.label)
                            },
                            label = { Text(platform.shortLabel) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_platform_${platform.shortLabel.lowercase()}")
                        )
                    }
                }
            }

            when (trendsState) {
                is UiState.Loading -> {
                    if (!analysisActionState.isAnalyzing) {
                        item {
                            TrendAnalysisLoadingCard(niche = profile.niche)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = trendsState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val filteredTrends = rememberedFilteredTrends
                    if (filteredTrends.isEmpty()) {
                        if (!analysisActionState.isAnalyzing) {
                            item {
                                EmptyRadarCard(
                                    hasAnyTrends = trendsState.data.isNotEmpty(),
                                    onAnalyzeClick = { onRunDailyAnalysis(null) },
                                    onResetFilterClick = { onSelectPlatformFilter(null) }
                                )
                            }
                        }
                    } else {
                        val topThreeTrends = filteredTrends.take(3)
                        val remainingTrends = filteredTrends.drop(3)
                        val activePlatformName = VideoPlatform.entries
                            .firstOrNull { it.label == selectedPlatformFilter }
                            ?.shortLabel

                        item {
                            TopViewsSectionHeader(
                                title = if (activePlatformName != null) {
                                    "Top 3 Mais Vistos • $activePlatformName"
                                } else {
                                    "Top 3 Vídeos Mais Vistos Hoje"
                                },
                                subtitle = if (activePlatformName != null) {
                                    "Os 3 vídeos com maior alcance no $activePlatformName para você modelar."
                                } else {
                                    "Os 3 vídeos mais vistos entre YouTube, TikTok, Instagram e Facebook. Toque em uma plataforma acima para ver o Top 3 dela."
                                }
                            )
                        }

                        items(
                            items = topThreeTrends.mapIndexed { index, trend -> index + 1 to trend },
                            key = { (_, trend) -> "top_${trend.id.ifBlank { "${trend.rank}_${trend.platform}_${trend.title}" }}" }
                        ) { (viewRank, trend) ->
                            ViralTrendCard(
                                trend = trend,
                                displayRank = viewRank,
                                isTopThree = true,
                                onOpenScript = { onOpenTrendScript(trend) },
                                onOpenPlatformVideo = {
                                    openPlatformVideoSearch(context, trend.platform, trend.title)
                                },
                                onSaveScript = { onSaveTrendToLibrary(trend) }
                            )
                        }

                        if (remainingTrends.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mais Vídeos em Alta nas 4 Plataformas (${remainingTrends.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(
                                items = remainingTrends.mapIndexed { index, trend -> index + 4 to trend },
                                key = { (_, trend) -> "more_${trend.id.ifBlank { "${trend.rank}_${trend.platform}_${trend.title}" }}" }
                            ) { (viewRank, trend) ->
                                ViralTrendCard(
                                    trend = trend,
                                    displayRank = viewRank,
                                    isTopThree = false,
                                    onOpenScript = { onOpenTrendScript(trend) },
                                    onOpenPlatformVideo = {
                                        openPlatformVideoSearch(context, trend.platform, trend.title)
                                    },
                                    onSaveScript = { onSaveTrendToLibrary(trend) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun RadarHeaderCard(
    profile: CreatorProfile,
    todayFormatted: String,
    isAnalyzing: Boolean,
    onRefreshToday: () -> Unit,
    onFocusTopicClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HOJE • ${todayFormatted.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Radar de ${profile.displayName}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nicho: ${profile.niche}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRefreshToday,
                    enabled = !isAnalyzing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("analyze_today_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analisar Hoje", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onFocusTopicClick,
                    enabled = !isAnalyzing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("focus_topic_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Focar Tema")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyInsightCard(summary: DailySummary) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Termômetro do Dia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = summary.bestPostingWindow,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.dailyInsight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.topHashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.topHashtags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopViewsSectionHeader(
    title: String,
    subtitle: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AmberWarm.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, AmberWarm.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = AmberWarm,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ViralTrendCard(
    trend: ViralTrend,
    displayRank: Int,
    isTopThree: Boolean,
    onOpenScript: () -> Unit,
    onOpenPlatformVideo: () -> Unit,
    onSaveScript: () -> Unit
) {
    val borderColor = if (isTopThree) {
        AmberWarm.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    val rankBadgeBg = if (isTopThree) {
        AmberWarm.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val rankBadgeFg = if (isTopThree) {
        AmberWarm
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val rankLabel = if (isTopThree) {
        "#$displayRank MAIS VISTO"
    } else {
        "#$displayRank EM ALTA"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenScript)
            .testTag("trend_card_$displayRank"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = rankBadgeBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = rankLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = rankBadgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        PlatformBadge(platform = trend.platform)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = trend.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trend.estimatedViewsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trend.durationSeconds}s • ${trend.formatType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                EngagementScoreRing(score = trend.engagementScore)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "GANCHO SUGERIDO (0-3s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${trend.hookText}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = trend.whyItWorks,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenScript,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_trend_script_$displayRank")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Roteiro", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onOpenPlatformVideo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("watch_trend_video_$displayRank")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Vídeo", maxLines = 1)
                }

                IconButton(
                    onClick = onSaveScript,
                    enabled = !trend.savedToScripts,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_trend_button_$displayRank")
                ) {
                    Icon(
                        imageVector = if (trend.savedToScripts) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (trend.savedToScripts) "Roteiro já salvo" else "Salvar roteiro",
                        tint = if (trend.savedToScripts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRadarCard(
    hasAnyTrends: Boolean,
    onAnalyzeClick: () -> Unit,
    onResetFilterClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasAnyTrends) "Nenhum vídeo nesta plataforma hoje" else "Seu radar diário está pronto",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasAnyTrends) {
                    "Limpe o filtro de plataforma para ver todas as tendências analisadas hoje."
                } else {
                    "Toque abaixo para analisar os reels e vídeos mais vistos de hoje e gerar roteiros simples para seu nicho."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = if (hasAnyTrends) onResetFilterClick else onAnalyzeClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(if (hasAnyTrends) "Ver Todas Plataformas" else "Analisar Agora")
            }
        }
    }
}
