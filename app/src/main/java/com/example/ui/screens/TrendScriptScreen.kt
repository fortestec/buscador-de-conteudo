package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.VideoPlatform
import com.example.model.ViralTrend
import com.example.ui.components.CopyableScriptSectionCard
import com.example.ui.components.EngagementScoreRing
import com.example.ui.components.PlatformBadge
import com.example.ui.components.openPlatformVideoSearch
import com.example.ui.viewmodel.ScriptAdaptActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScriptScreen(
    trend: ViralTrend,
    adaptState: ScriptAdaptActionState,
    onBack: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onAdaptWithAi: (customTopic: String) -> Unit,
    onDismissAdaptError: () -> Unit,
    onCopiedFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val platformShortName = remember(trend.platform) {
        VideoPlatform.entries.firstOrNull { it.label == trend.platform }?.shortLabel ?: trend.platform
    }
    var showAdaptDialog by remember { mutableStateOf(false) }
    var customTopic by remember { mutableStateOf("") }

    if (showAdaptDialog) {
        AlertDialog(
            onDismissRequest = { if (!adaptState.isAdapting) showAdaptDialog = false },
            title = { Text("Adaptar Roteiro com IA") },
            text = {
                Column {
                    Text(
                        text = "Quer usar exatamente essa estrutura viral para falar de um assunto, produto ou história sua?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customTopic,
                        onValueChange = { customTopic = it.take(120) },
                        label = { Text("Sobre o que você quer gravar?") },
                        placeholder = { Text("Ex: meu serviço de consultoria, receita rápida...") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adapt_topic_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdaptDialog = false
                        onAdaptWithAi(customTopic)
                    },
                    enabled = customTopic.trim().length >= 3,
                    modifier = Modifier.testTag("confirm_adapt_button")
                ) {
                    Text("Adaptar Agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdaptDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Roteiro de Replicação", maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("back_from_trend_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSaveToLibrary,
                        enabled = !trend.savedToScripts,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_save_script_button")
                    ) {
                        Icon(
                            imageVector = if (trend.savedToScripts) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (trend.savedToScripts) "Salvo nos roteiros" else "Salvar roteiro",
                            tint = if (trend.savedToScripts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PlatformBadge(platform = trend.platform)
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Timer,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${trend.durationSeconds}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = trend.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${trend.formatType} • ${trend.estimatedViewsLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            EngagementScoreRing(score = trend.engagementScore, size = 70.dp)
                        }
                    }
                }

                if (adaptState.isAdapting) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Reescrevendo o roteiro simples para o seu tema...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (!adaptState.errorMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = adaptState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissAdaptError) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Por que usar como inspiração hoje",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = trend.whyItWorks,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Como adaptar ao seu estilo:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trend.adaptationAngle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Áudio: ${trend.viralAudioStyle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Roteiro Simples para Gravar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                item {
                    CopyableScriptSectionCard(
                        stepLabel = "PASSO 1",
                        title = "Gancho (0 a 3 seg)",
                        content = trend.hookText,
                        accentColor = MaterialTheme.colorScheme.primary,
                        copyTestTag = "copy_hook_button",
                        onCopyClick = {
                            clipboard.setText(AnnotatedString(trend.hookText))
                            onCopiedFeedback("Gancho copiado!")
                        }
                    )
                }

                item {
                    val beatsText = trend.bodyBeats.mapIndexed { idx, beat ->
                        "${idx + 1}. $beat"
                    }.joinToString("\n\n")
                    CopyableScriptSectionCard(
                        stepLabel = "PASSO 2",
                        title = "Desenvolvimento Rápido",
                        content = beatsText,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        copyTestTag = "copy_beats_button",
                        onCopyClick = {
                            clipboard.setText(AnnotatedString(beatsText))
                            onCopiedFeedback("Passos do roteiro copiados!")
                        }
                    )
                }

                item {
                    CopyableScriptSectionCard(
                        stepLabel = "PASSO 3",
                        title = "Chamada para Ação (CTA)",
                        content = trend.ctaText,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        copyTestTag = "copy_cta_button",
                        onCopyClick = {
                            clipboard.setText(AnnotatedString(trend.ctaText))
                            onCopiedFeedback("CTA copiado!")
                        }
                    )
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dicas de Gravação no Celular",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = trend.visualTips,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    CopyableScriptSectionCard(
                        stepLabel = "LEGENDA",
                        title = "Texto + Hashtags Prontas",
                        content = trend.captionAndHashtags,
                        accentColor = MaterialTheme.colorScheme.primary,
                        copyTestTag = "copy_caption_button",
                        onCopyClick = {
                            clipboard.setText(AnnotatedString(trend.captionAndHashtags))
                            onCopiedFeedback("Legenda copiada!")
                        }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                openPlatformVideoSearch(context, trend.platform, trend.title)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("open_platform_video_detail_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Vídeos Parecidos no $platformShortName")
                        }

                        FilledTonalButton(
                            onClick = { showAdaptDialog = true },
                            enabled = !adaptState.isAdapting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("adapt_script_ai_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adaptar Roteiro para Outro Assunto")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val fullScript = buildString {
                                        appendLine("TÍTULO: ${trend.title} (${trend.platform})")
                                        appendLine()
                                        appendLine("GANCHO (0-3s):")
                                        appendLine(trend.hookText)
                                        appendLine()
                                        appendLine("ROTEIRO:")
                                        trend.bodyBeats.forEachIndexed { idx, beat ->
                                            appendLine("${idx + 1}. $beat")
                                        }
                                        appendLine()
                                        appendLine("CTA:")
                                        appendLine(trend.ctaText)
                                        appendLine()
                                        appendLine("LEGENDA:")
                                        appendLine(trend.captionAndHashtags)
                                    }
                                    clipboard.setText(AnnotatedString(fullScript))
                                    onCopiedFeedback("Roteiro completo copiado!")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_full_script_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Tudo", maxLines = 1)
                            }

                            Button(
                                onClick = onSaveToLibrary,
                                enabled = !trend.savedToScripts,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_to_scripts_cta")
                            ) {
                                Icon(
                                    imageVector = if (trend.savedToScripts) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (trend.savedToScripts) "Salvo!" else "Salvar Roteiro",
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}
