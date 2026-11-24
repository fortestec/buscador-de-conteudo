package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SavedScript
import com.example.model.ScriptStatus
import com.example.model.UiState
import com.example.ui.components.PlatformBadge

@Composable
fun ScriptsLibraryScreen(
    scriptsState: UiState<List<SavedScript>>,
    weeklyGoal: Long,
    onUpdateStatus: (scriptId: String, status: ScriptStatus) -> Unit,
    onUpdateScriptContent: (
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
    ) -> Unit,
    onDeleteScript: (scriptId: String) -> Unit,
    onCopiedFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var statusFilter by remember { mutableStateOf<ScriptStatus?>(null) }
    var editingScript by remember { mutableStateOf<SavedScript?>(null) }
    var deletingScript by remember { mutableStateOf<SavedScript?>(null) }

    editingScript?.let { script ->
        ScriptEditDialog(
            script = script,
            onDismiss = { editingScript = null },
            onSave = { title, hook, beatsText, cta, caption, status ->
                val beats = beatsText.lines().map { it.trim() }.filter { it.isNotBlank() }.take(8)
                    .ifEmpty { script.bodyBeats }
                onUpdateScriptContent(
                    script.id,
                    title,
                    script.platform,
                    script.durationSeconds,
                    hook,
                    beats,
                    cta,
                    script.visualTips,
                    caption,
                    status
                )
                editingScript = null
            }
        )
    }

    deletingScript?.let { script ->
        AlertDialog(
            onDismissRequest = { deletingScript = null },
            title = { Text("Excluir Roteiro") },
            text = { Text("Deseja remover \"${script.title}\" dos seus roteiros salvos?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteScript(script.id)
                        deletingScript = null
                    },
                    modifier = Modifier.testTag("confirm_delete_script_button")
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingScript = null }) {
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
        when (scriptsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = scriptsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is UiState.Success -> {
                val allScripts = scriptsState.data
                val publishedCount = allScripts.count { it.scriptStatus == ScriptStatus.PUBLICADO }
                val progress = (publishedCount.toFloat() / weeklyGoal.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val filteredScripts = allScripts.filter {
                    statusFilter == null || it.scriptStatus == statusFilter
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Meus Roteiros",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Edite, copie como teleprompter e acompanhe suas gravações.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Meta Semanal de Publicações",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$publishedCount / $weeklyGoal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = statusFilter == null,
                                    onClick = { statusFilter = null },
                                    label = { Text("Todos (${allScripts.size})") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                            items(ScriptStatus.entries) { st ->
                                val count = allScripts.count { it.scriptStatus == st }
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = if (statusFilter == st) null else st },
                                    label = { Text("${st.labelPt} ($count)") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }

                    if (filteredScripts.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MovieCreation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (allScripts.isEmpty()) "Nenhum roteiro salvo ainda" else "Nenhum roteiro com esse status",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Abra qualquer tendência no Radar de Hoje e toque em Salvar Roteiro para organizar sua produção.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredScripts, key = { it.id }) { script ->
                            SavedScriptCard(
                                script = script,
                                onEditClick = { editingScript = script },
                                onCopyClick = {
                                    val full = buildString {
                                        appendLine(script.title)
                                        appendLine()
                                        appendLine("GANCHO: ${script.hookText}")
                                        appendLine()
                                        script.bodyBeats.forEachIndexed { idx, b -> appendLine("${idx + 1}. $b") }
                                        appendLine()
                                        appendLine("CTA: ${script.ctaText}")
                                        appendLine()
                                        appendLine(script.captionAndHashtags)
                                    }
                                    clipboard.setText(AnnotatedString(full))
                                    onCopiedFeedback("Roteiro copiado!")
                                },
                                onCycleStatus = { nextStatus ->
                                    onUpdateStatus(script.id, nextStatus)
                                },
                                onDeleteClick = { deletingScript = script }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SavedScriptCard(
    script: SavedScript,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onCycleStatus: (ScriptStatus) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onEditClick)
            .testTag("saved_script_card_${script.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformBadge(platform = script.platform)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "${script.engagementScore}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar roteiro",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Excluir roteiro",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = script.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gancho: \"${script.hookText}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScriptStatus.entries.forEach { st ->
                    val active = script.scriptStatus == st
                    FilterChip(
                        selected = active,
                        onClick = { if (!active) onCycleStatus(st) },
                        label = { Text(st.labelPt, maxLines = 1) },
                        leadingIcon = if (active) {
                            { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onEditClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir e Editar Roteiro")
            }
        }
    }
}

@Composable
private fun ScriptEditDialog(
    script: SavedScript,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        hook: String,
        beatsText: String,
        cta: String,
        caption: String,
        status: ScriptStatus
    ) -> Unit
) {
    var title by remember { mutableStateOf(script.title) }
    var hook by remember { mutableStateOf(script.hookText) }
    var beatsText by remember { mutableStateOf(script.bodyBeats.joinToString("\n")) }
    var cta by remember { mutableStateOf(script.ctaText) }
    var caption by remember { mutableStateOf(script.captionAndHashtags) }
    var status by remember { mutableStateOf(script.scriptStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Roteiro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hook,
                    onValueChange = { hook = it.take(400) },
                    label = { Text("Gancho (0-3s)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = beatsText,
                    onValueChange = { beatsText = it.take(1500) },
                    label = { Text("Passos do roteiro (1 por linha)") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cta,
                    onValueChange = { cta = it.take(300) },
                    label = { Text("CTA final") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(600) },
                    label = { Text("Legenda e hashtags") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScriptStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.labelPt) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().length >= 2 && hook.trim().length >= 2 && cta.trim().length >= 2) {
                        onSave(title, hook, beatsText, cta, caption, status)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
