package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.signOutUser
import com.example.model.CreatorProfile
import com.example.model.VideoPlatform
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToLong

private val PROFILE_SUGGESTED_NICHES = listOf(
    "Vídeos de Motoboys Engraçados",
    "Humor e Cotidiano",
    "Tecnologia e IA",
    "Finanças Pessoais",
    "Fitness e Saúde",
    "Marketing e Vendas",
    "Gastronomia",
    "Beleza e Moda"
)

private val PROFILE_TONE_OPTIONS = listOf(
    "Direto e Energético",
    "Didático e Leve",
    "Storytelling Emocional",
    "Humor e Espontâneo",
    "Opinião Forte"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: CreatorProfile,
    onSaveProfile: (
        displayName: String,
        niche: String,
        targetAudience: String,
        toneStyle: String,
        preferredPlatforms: List<String>,
        weeklyGoal: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val userEmail = remember { Firebase.auth.currentUser?.email.orEmpty() }

    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var niche by remember(profile) { mutableStateOf(profile.niche) }
    var targetAudience by remember(profile) { mutableStateOf(profile.targetAudience) }
    var toneStyle by remember(profile) { mutableStateOf(profile.toneStyle) }
    var selectedPlatforms by remember(profile) {
        mutableStateOf(
            profile.preferredPlatforms.toSet().ifEmpty { VideoPlatform.entries.map { it.label }.toSet() }
        )
    }
    var weeklyGoal by remember(profile) { mutableFloatStateOf(profile.weeklyGoal.toFloat().coerceIn(1f, 14f)) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Perfil de Criador",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Conta conectada",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(60) },
                label = { Text("Nome artístico / do canal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    text = "Nicho principal",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PROFILE_SUGGESTED_NICHES.forEach { preset ->
                        val selected = niche.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { niche = preset },
                            label = { Text(preset) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = niche,
                    onValueChange = { niche = it.take(80) },
                    label = { Text("Ou digite seu nicho") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = targetAudience,
                onValueChange = { targetAudience = it.take(160) },
                label = { Text("Público-alvo") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    text = "Tom de comunicação",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PROFILE_TONE_OPTIONS.forEach { tone ->
                        FilterChip(
                            selected = toneStyle == tone,
                            onClick = { toneStyle = tone },
                            label = { Text(tone) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "Plataformas prioritárias",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoPlatform.entries.forEach { platform ->
                        val selected = selectedPlatforms.contains(platform.label)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedPlatforms = if (selected && selectedPlatforms.size > 1) {
                                    selectedPlatforms - platform.label
                                } else {
                                    selectedPlatforms + platform.label
                                }
                            },
                            label = { Text(platform.label) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "Meta semanal: ${weeklyGoal.roundToLong()} vídeos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    value = weeklyGoal,
                    onValueChange = { weeklyGoal = it },
                    valueRange = 1f..14f,
                    steps = 12
                )
            }

            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (niche.trim().length < 2 || targetAudience.trim().length < 2) {
                        errorText = "Preencha nicho e público-alvo com pelo menos 2 caracteres."
                        return@Button
                    }
                    errorText = null
                    onSaveProfile(
                        displayName.trim().ifBlank { "Criador" },
                        niche.trim(),
                        targetAudience.trim(),
                        toneStyle,
                        selectedPlatforms.toList(),
                        weeklyGoal.roundToLong()
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("update_profile_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
